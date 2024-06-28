#ifndef LCJ_NOTIFYHTTP_H
#define LCJ_NOTIFYHTTP_H

#include <inttypes.h>

int notifyHTTP(const uint8_t *message, size_t messageSize, bool isResponse);

#endif
