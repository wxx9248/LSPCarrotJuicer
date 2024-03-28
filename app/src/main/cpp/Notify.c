#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>
#include <stdlib.h>
#include <memory.h>
#include <unistd.h>

#include "Notify.h"
#include "Log.h"

static int notifyHTTP(const uint8_t *message, size_t bufferLength, bool isResponse);

int notifyRequest(const uint8_t *message, size_t bufferLength) {
#define LOG_TAG "LCJ/Native/notifyRequest"
    return notifyHTTP(message, bufferLength, false);
#undef LOG_TAG
}

int notifyResponse(const uint8_t *message, size_t bufferLength) {
#define LOG_TAG "LCJ/Native/notifyResponse"
    return notifyHTTP(message, bufferLength, true);
#undef LOG_TAG
}

int notifyHTTP(const uint8_t *message, size_t bufferLength, bool isResponse) {
#define LOG_TAG "LCJ/Native/notifyHTTP"
    int socketFD = socket(AF_INET, SOCK_STREAM, 0);
    if (socketFD == -1) {
        LOG_E(LOG_TAG, "Socket creation failed (code %d)", errno);
        return -1;
    }

    struct sockaddr_in serverAddress = {0};
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_addr.s_addr = inet_addr("10.0.0.28");
    serverAddress.sin_port = htons(4693);

    if (connect(socketFD, (struct sockaddr *) &serverAddress, sizeof serverAddress)) {
        LOG_E(LOG_TAG, "Connection failed (code %d)", errno);
        close(socketFD);
        return -1;
    }

    // 100 MB
    char *request = malloc(100 * 1024 * 1024);
    memset(request, 0, 100 * 1024 * 1024);
    int written = sprintf(request,
        "POST /notify/%s HTTP/1.1\r\n"
        "Host: 10.0.0.28\r\n"
        "User-Agent: LSPCarrotJuicer\r\n"
        "Content-Type: application/x-msgpack\r\n"
        "Content-Length: %zd\r\n"
        "\r\n",
        isResponse ? "response" : "request",
        bufferLength
    );
    memcpy(request + written, message, bufferLength);
    written += bufferLength;
    memcpy(request + written, "\r\n", 3);
    written += 3;

    if (send(socketFD, request, written, 0) == -1) {
        LOG_E(LOG_TAG, "Send failed (code %d)", errno);
        free(request);
        close(socketFD);
        return -1;
    }
    free(request);

    char response[1024] = {0};
    if (recv(socketFD, response, sizeof(response), 0) == -1) {
        LOG_E(LOG_TAG, "Receive failed (code %d)", errno);
        close(socketFD);
        return -1;
    }

    if (strstr(response, "200 OK") == NULL) {
        LOG_E(LOG_TAG, "Server did not return 200 OK");
        close(socketFD);
        return -1;
    }

    LOG_I(LOG_TAG, "Request sent successfully");
    close(socketFD);
    return 0;
#undef LOG_TAG
}
