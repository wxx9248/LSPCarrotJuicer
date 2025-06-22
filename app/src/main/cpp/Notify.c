#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <errno.h>
#include <time.h>
#include <pthread.h>

#include "Notify.h"
#include "Log.h"
#include "Util.h"

#define PIPE_PACKET_PATH "/data/data/jp.co.cygames.umamusume/pipes/lcj_packets"
#define PIPE_LOG_PATH "/data/data/jp.co.cygames.umamusume/pipes/lcj_logs"
#define PIPE_CONTROL_PATH "/data/data/jp.co.cygames.umamusume/pipes/lcj_control"

#define MAX_PACKET_SIZE 65536
#define MAX_LOG_SIZE 2048
#define HANDSHAKE_TIMEOUT_SECONDS 10

// IPC state
static int packetPipeFd = -1;
static int logPipeFd = -1;
static int controlPipeFd = -1;
static bool ipcInitialized = false;
static pthread_mutex_t ipcMutex = PTHREAD_MUTEX_INITIALIZER;

// Function prototypes
static bool createNamedPipe(const char *path);

static bool performHandshake();

static ssize_t writeWithTimeout(int fd, const void *buffer, size_t size, int timeoutMs);

bool initializeIPC() {
#define LOG_TAG "LCJ/Native/initializeIPC"
    pthread_mutex_lock(&ipcMutex);

    if (ipcInitialized) {
        pthread_mutex_unlock(&ipcMutex);
        return true;
    }

    LOG_I(LOG_TAG, "Initializing IPC communication");

    // Create named pipes
    if (!createNamedPipe(PIPE_PACKET_PATH) ||
        !createNamedPipe(PIPE_LOG_PATH) ||
        !createNamedPipe(PIPE_CONTROL_PATH)) {
        LOG_E(LOG_TAG, "Failed to create named pipes");
        pthread_mutex_unlock(&ipcMutex);
        return false;
    }

    // Open pipes for writing (blocking until reader connects)
    LOG_D(LOG_TAG, "Opening packet pipe: %s", PIPE_PACKET_PATH);
    packetPipeFd = open(PIPE_PACKET_PATH, O_WRONLY);
    if (packetPipeFd == -1) {
        LOG_E(LOG_TAG, "Failed to open packet pipe: %s", strerror(errno));
        pthread_mutex_unlock(&ipcMutex);
        return false;
    }

    LOG_D(LOG_TAG, "Opening log pipe: %s", PIPE_LOG_PATH);
    logPipeFd = open(PIPE_LOG_PATH, O_WRONLY);
    if (logPipeFd == -1) {
        LOG_E(LOG_TAG, "Failed to open log pipe: %s", strerror(errno));
        close(packetPipeFd);
        packetPipeFd = -1;
        pthread_mutex_unlock(&ipcMutex);
        return false;
    }

    LOG_D(LOG_TAG, "Opening control pipe: %s", PIPE_CONTROL_PATH);
    controlPipeFd = open(PIPE_CONTROL_PATH, O_RDWR);
    if (controlPipeFd == -1) {
        LOG_E(LOG_TAG, "Failed to open control pipe: %s", strerror(errno));
        close(packetPipeFd);
        close(logPipeFd);
        packetPipeFd = -1;
        logPipeFd = -1;
        pthread_mutex_unlock(&ipcMutex);
        return false;
    }

    // Perform handshake
    if (!performHandshake()) {
        LOG_E(LOG_TAG, "IPC handshake failed");
        shutdownIPC();
        pthread_mutex_unlock(&ipcMutex);
        return false;
    }

    ipcInitialized = true;
    LOG_I(LOG_TAG, "IPC initialization completed successfully");
    pthread_mutex_unlock(&ipcMutex);
    return true;
#undef LOG_TAG
}

void shutdownIPC() {
#define LOG_TAG "LCJ/Native/shutdownIPC"
    pthread_mutex_lock(&ipcMutex);

    if (!ipcInitialized) {
        pthread_mutex_unlock(&ipcMutex);
        return;
    }

    LOG_I(LOG_TAG, "Shutting down IPC communication");

    // Close file descriptors
    if (packetPipeFd != -1) {
        close(packetPipeFd);
        packetPipeFd = -1;
    }

    if (logPipeFd != -1) {
        close(logPipeFd);
        logPipeFd = -1;
    }

    if (controlPipeFd != -1) {
        close(controlPipeFd);
        controlPipeFd = -1;
    }

    ipcInitialized = false;
    LOG_I(LOG_TAG, "IPC shutdown completed");
    pthread_mutex_unlock(&ipcMutex);
#undef LOG_TAG
}

int notifyRequest(const uint8_t *message, size_t messageSize) {
#define LOG_TAG "LCJ/Native/notifyRequest"
    if (!ipcInitialized || packetPipeFd == -1) {
        LOG_W(LOG_TAG, "IPC not initialized, dropping request packet");
        return -1;
    }

    if (messageSize > MAX_PACKET_SIZE) {
        LOG_E(LOG_TAG, "Packet too large: %zu bytes (max: %d)", messageSize, MAX_PACKET_SIZE);
        return -1;
    }

    // Prepare packet header: [4 bytes size][1 byte is_request][timestamp][data]
    uint32_t packetSize = (uint32_t) messageSize;
    uint8_t isRequest = 1;
    uint64_t timestamp = (uint64_t) time(NULL) * 1000; // Convert to milliseconds

    // Write packet header
    if (writeWithTimeout(packetPipeFd, &packetSize, sizeof(packetSize), 1000) !=
        sizeof(packetSize)) {
        LOG_E(LOG_TAG, "Failed to write packet size");
        return -1;
    }

    if (writeWithTimeout(packetPipeFd, &isRequest, sizeof(isRequest), 1000) != sizeof(isRequest)) {
        LOG_E(LOG_TAG, "Failed to write request flag");
        return -1;
    }

    if (writeWithTimeout(packetPipeFd, &timestamp, sizeof(timestamp), 1000) != sizeof(timestamp)) {
        LOG_E(LOG_TAG, "Failed to write timestamp");
        return -1;
    }

    // Write packet data
    if (writeWithTimeout(packetPipeFd, message, messageSize, 5000) != (ssize_t) messageSize) {
        LOG_E(LOG_TAG, "Failed to write packet data");
        return -1;
    }

    LOG_D(LOG_TAG, "Request packet sent successfully (%zu bytes)", messageSize);
    return 0;
#undef LOG_TAG
}

int notifyResponse(const uint8_t *message, size_t messageSize) {
#define LOG_TAG "LCJ/Native/notifyResponse"
    if (!ipcInitialized || packetPipeFd == -1) {
        LOG_W(LOG_TAG, "IPC not initialized, dropping response packet");
        return -1;
    }

    if (messageSize > MAX_PACKET_SIZE) {
        LOG_E(LOG_TAG, "Packet too large: %zu bytes (max: %d)", messageSize, MAX_PACKET_SIZE);
        return -1;
    }

    // Prepare packet header: [4 bytes size][1 byte is_request][timestamp][data]
    uint32_t packetSize = (uint32_t) messageSize;
    uint8_t isRequest = 0;
    uint64_t timestamp = (uint64_t) time(NULL) * 1000; // Convert to milliseconds

    // Write packet header
    if (writeWithTimeout(packetPipeFd, &packetSize, sizeof(packetSize), 1000) !=
        sizeof(packetSize)) {
        LOG_E(LOG_TAG, "Failed to write packet size");
        return -1;
    }

    if (writeWithTimeout(packetPipeFd, &isRequest, sizeof(isRequest), 1000) != sizeof(isRequest)) {
        LOG_E(LOG_TAG, "Failed to write request flag");
        return -1;
    }

    if (writeWithTimeout(packetPipeFd, &timestamp, sizeof(timestamp), 1000) != sizeof(timestamp)) {
        LOG_E(LOG_TAG, "Failed to write timestamp");
        return -1;
    }

    // Write packet data
    if (writeWithTimeout(packetPipeFd, message, messageSize, 5000) != (ssize_t) messageSize) {
        LOG_E(LOG_TAG, "Failed to write packet data");
        return -1;
    }

    LOG_D(LOG_TAG, "Response packet sent successfully (%zu bytes)", messageSize);
    return 0;
#undef LOG_TAG
}

void notifyLog(int level, const char *tag, const char *message) {
#define LOG_TAG "LCJ/Native/notifyLog"
    if (!ipcInitialized || logPipeFd == -1) {
        return; // Don't log this to avoid recursion
    }

    size_t tagLen = strnlen(tag, 256);
    size_t msgLen = strnlen(message, MAX_LOG_SIZE - 256);

    if (tagLen == 0 || msgLen == 0) {
        return;
    }

    // Prepare log entry: [4 bytes total size][4 bytes level][8 bytes timestamp][tag][null][message][null]
    uint32_t totalSize = sizeof(uint32_t) + sizeof(uint64_t) + tagLen + 1 + msgLen + 1;
    uint64_t timestamp = (uint64_t) time(NULL) * 1000;

    // Write log header
    if (writeWithTimeout(logPipeFd, &totalSize, sizeof(totalSize), 500) != sizeof(totalSize)) {
        return;
    }

    if (writeWithTimeout(logPipeFd, &level, sizeof(level), 500) != sizeof(level)) {
        return;
    }

    if (writeWithTimeout(logPipeFd, &timestamp, sizeof(timestamp), 500) != sizeof(timestamp)) {
        return;
    }

    // Write tag and message
    if (writeWithTimeout(logPipeFd, tag, tagLen + 1, 500) != (ssize_t) (tagLen + 1)) {
        return;
    }

    if (writeWithTimeout(logPipeFd, message, msgLen + 1, 500) != (ssize_t) (msgLen + 1)) {
        return;
    }
#undef LOG_TAG
}

bool isIPCConnected() {
    pthread_mutex_lock(&ipcMutex);
    bool connected = ipcInitialized && packetPipeFd != -1 && logPipeFd != -1 && controlPipeFd != -1;
    pthread_mutex_unlock(&ipcMutex);
    return connected;
}

// Helper functions
static bool createNamedPipe(const char *path) {
#define LOG_TAG "LCJ/Native/createNamedPipe"
    // Remove existing pipe if it exists
    unlink(path);

    // Create the pipe
    if (mkfifo(path, 0660) == -1) {
        LOG_E(LOG_TAG, "Failed to create named pipe %s: %s", path, strerror(errno));
        return false;
    }

    LOG_D(LOG_TAG, "Created named pipe: %s", path);
    return true;
#undef LOG_TAG
}

static bool performHandshake() {
#define LOG_TAG "LCJ/Native/performHandshake"
    const char *handshakeMsg = "LCJ_NATIVE_READY";
    size_t msgLen = strlen(handshakeMsg);

    LOG_D(LOG_TAG, "Performing IPC handshake");

    // Send handshake message
    if (writeWithTimeout(controlPipeFd, handshakeMsg, msgLen, 5000) != (ssize_t) msgLen) {
        LOG_E(LOG_TAG, "Failed to send handshake message");
        return false;
    }

    // Wait for acknowledgment (with timeout)
    char ackBuffer[32];
    fd_set readFds;
    struct timeval timeout;

    FD_ZERO(&readFds);
    FD_SET(controlPipeFd, &readFds);
    timeout.tv_sec = HANDSHAKE_TIMEOUT_SECONDS;
    timeout.tv_usec = 0;

    int selectResult = select(controlPipeFd + 1, &readFds, NULL, NULL, &timeout);
    if (selectResult <= 0) {
        LOG_E(LOG_TAG, "Handshake timeout or error");
        return false;
    }

    ssize_t bytesRead = read(controlPipeFd, ackBuffer, sizeof(ackBuffer) - 1);
    if (bytesRead <= 0) {
        LOG_E(LOG_TAG, "Failed to read handshake acknowledgment");
        return false;
    }

    ackBuffer[bytesRead] = '\0';
    if (strcmp(ackBuffer, "LCJ_KOTLIN_READY") != 0) {
        LOG_E(LOG_TAG, "Invalid handshake acknowledgment: %s", ackBuffer);
        return false;
    }

    LOG_I(LOG_TAG, "IPC handshake completed successfully");
    return true;
#undef LOG_TAG
}

static ssize_t writeWithTimeout(int fd, const void *buffer, size_t size, int timeoutMs) {
    fd_set writeFds;
    struct timeval timeout;

    FD_ZERO(&writeFds);
    FD_SET(fd, &writeFds);
    timeout.tv_sec = timeoutMs / 1000;
    timeout.tv_usec = (timeoutMs % 1000) * 1000;

    int selectResult = select(fd + 1, NULL, &writeFds, NULL, &timeout);
    if (selectResult <= 0) {
        return -1; // Timeout or error
    }

    return write(fd, buffer, size);
}
