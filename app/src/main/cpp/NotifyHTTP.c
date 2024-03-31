#include <errno.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <unistd.h>

#include "NotifyHTTP.h"
#include "Log.h"
#include "Constants.h"
#include "Util.h"

static int readConfig(char *hostOutput, size_t hostBufferSize, uint16_t *portOutput);

static size_t
requestBuilder(const char *host, const uint8_t *message, size_t messageSize, bool isResponse,
               uint8_t **requestBufferOutput);

// IPv4: 255.255.255.255, maximum length 15
static char host[16];
// Port: maximum value: 65535
static uint16_t port;
static bool isConfigLoaded;

int notifyHTTP(const uint8_t *message, size_t messageSize, bool isResponse) {
#define LOG_TAG "LCJ/Native/notifyHTTP"
    if (!isConfigLoaded) {
        int result;
        if ((result = readConfig(host, sizeof host, &port)) < 0) {
            return result;
        }
        isConfigLoaded = true;
    }

    int socketFD = socket(AF_INET, SOCK_STREAM, 0);
    if (socketFD == -1) {
        LOG_E(LOG_TAG, "Socket creation failed (code %d)", errno);
        return -1;
    }
    LOG_D(LOG_TAG, "Socket created");

    struct sockaddr_in serverAddress = {0};
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_addr.s_addr = inet_addr(host);
    serverAddress.sin_port = htons(port);

    if (connect(socketFD, (struct sockaddr *) &serverAddress, sizeof serverAddress)) {
        LOG_E(LOG_TAG, "Connection failed (code %d)", errno);
        close(socketFD);
        return -1;
    }
    LOG_D(LOG_TAG, "Connected");

    uint8_t *requestBuffer = nullptr;
    size_t requestSize = requestBuilder(host, message, messageSize, isResponse, &requestBuffer);
    if (send(socketFD, requestBuffer, requestSize, 0) == -1) {
        LOG_E(LOG_TAG, "Request failed (code %d)", errno);
        free(requestBuffer);
        close(socketFD);
        return -1;
    }
    LOG_D(LOG_TAG, "Request sent");
    free(requestBuffer);

    char response[RESPONSE_BUFFER_SIZE] = {0};
    if (recv(socketFD, response, sizeof response, 0) == -1) {
        LOG_E(LOG_TAG, "Receive failed (code %d)", errno);
        close(socketFD);
        return -1;
    }
    LOG_D(LOG_TAG, "Response received");

    if (strstr(response, "200 OK") == NULL) {
        LOG_E(LOG_TAG, "Server did not return 200 OK");
        close(socketFD);
        return -1;
    }
    LOG_D(LOG_TAG, "Server returned 200 OK");

    close(socketFD);
    return 0;
#undef LOG_TAG
}

int readConfig(char *hostOutput, size_t hostBufferSize, uint16_t *portOutput) {
#define LOG_TAG "LCJ/Native/readConfig"
    size_t bytesRead = 0;

    FILE *configHost = fopen(CONFIG_DIRECTORY_PATH "/host", "r");
    if (!configHost) {
        LOG_E(LOG_TAG, "Cannot open %s (code: %d)", CONFIG_DIRECTORY_PATH "/host", errno);
        return -1;
    }
    bytesRead = fread(hostOutput, sizeof(char), hostBufferSize, configHost);
    LOG_D(LOG_TAG, "Read %zd bytes from %s", bytesRead, CONFIG_DIRECTORY_PATH "/host");
    hostOutput[hostBufferSize - 1] = '\0';
    LOG_D(LOG_TAG, "Read host from config as %s", hostOutput);
    fclose(configHost);

    // Type uint16_t: maximum length 5
    char portBuffer[6] = {0};
    FILE *configPort = fopen(CONFIG_DIRECTORY_PATH "/port", "r");
    if (!configPort) {
        LOG_E(LOG_TAG, "Cannot open %s (code: %d)", CONFIG_DIRECTORY_PATH "/port", errno);
        return -1;
    }
    bytesRead = fread(portBuffer, sizeof(char), sizeof portBuffer, configPort);
    LOG_D(LOG_TAG, "Read %zd bytes from %s", bytesRead, CONFIG_DIRECTORY_PATH "/port");
    portBuffer[sizeof portBuffer - 1] = '\0';
    LOG_D(LOG_TAG, "Read port from config as %s", portBuffer);
    fclose(configPort);

    if (sscanf(portBuffer, "%" SCNu16, portOutput) != 1) {
        LOG_E(LOG_TAG, "Cannot parse port number from config");
        return -1;
    }
    LOG_D(LOG_TAG, "Parsed port number as %" PRIu16, *portOutput);

    return 0;
#undef LOG_TAG
}

size_t requestBuilder(const char *host, const uint8_t *message, size_t messageSize, bool isResponse,
                      uint8_t **requestBufferOutput) {
#define LOG_TAG "LCJ/Native/requestBuilder"
#define HTTP_HEADER_FORMAT_STRING \
    "POST /notify/%.8s HTTP/1.1\r\n" \
    "Host: %.15s\r\n" \
    "User-Agent: LSPCarrotJuicer\r\n" \
    "Content-Type: application/x-msgpack\r\n" \
    "Content-Length: %zd\r\n" \
    "\r\n"

    // -1 for '\0' in the end
    size_t headerSize = sizeof(HTTP_HEADER_FORMAT_STRING) - 1;
    headerSize += 8 - (sizeof "%.8s" - 1);
    headerSize += 15 - (sizeof "%.15s" - 1);
    headerSize += countDigits(messageSize) - (sizeof "%zd" - 1);

    size_t requestBufferSize = headerSize + messageSize + sizeof "\r\n";
    LOG_D(LOG_TAG, "Maximum request size expected to be %zd", requestBufferSize);

    uint8_t *requestBuffer = malloc(requestBufferSize);
    memset(requestBuffer, 0, requestBufferSize);
    LOG_D(LOG_TAG, "Request buffer allocated and zeroed");

    int requestSize = sprintf(
            (char *) requestBuffer,
            HTTP_HEADER_FORMAT_STRING,
            isResponse ? "response" : "request",
            host,
            messageSize
    );
    memcpy(requestBuffer + requestSize, message, messageSize);
    requestSize += messageSize;
    memcpy(requestBuffer + requestSize, "\r\n", sizeof "\r\n");
    requestSize += sizeof "\r\n";
    LOG_D(LOG_TAG, "%d bytes written to request buffer", requestSize);

    *requestBufferOutput = requestBuffer;
    return requestSize;
#undef LOG_TAG
}
