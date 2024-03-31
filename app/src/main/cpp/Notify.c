#include "Constants.h"
#include "Notify.h"
#include "NotifyHTTP.h"
#include "Log.h"

int notifyRequest(const uint8_t *message, size_t messageSize) {
#define LOG_TAG "LCJ/Native/notifyRequest"
    return notifyHTTP(message, messageSize, false);
#undef LOG_TAG
}

int notifyResponse(const uint8_t *message, size_t messageSize) {
#define LOG_TAG "LCJ/Native/notifyResponse"
    return notifyHTTP(message, messageSize, true);
#undef LOG_TAG
}
