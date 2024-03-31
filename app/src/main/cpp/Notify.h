#ifndef LCJ_NOTIFY_H
#define LCJ_NOTIFY_H

#include <inttypes.h>

int notifyRequest(const uint8_t *message, size_t messageSize);

int notifyResponse(const uint8_t *message, size_t messageSize);

#endif
