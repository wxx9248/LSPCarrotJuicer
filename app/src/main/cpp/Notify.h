#ifndef LCJ_NOTIFY_H
#define LCJ_NOTIFY_H

#define E_NOTIFY_OK 0

int notifyRequest(const uint8_t *buffer, size_t bufferLength);
int notifyResponse(const uint8_t *buffer, size_t bufferLength);

#endif
