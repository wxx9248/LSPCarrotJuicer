#ifndef LCJ_NOTIFYHTTP_H
#define LCJ_NOTIFYHTTP_H

#include <inttypes.h>

#define RESPONSE_BUFFER_SIZE 1024

int notifyHTTP(const uint8_t *message, size_t messageSize, bool isResponse);

#endif
