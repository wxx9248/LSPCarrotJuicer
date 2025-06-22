#ifndef LCJ_NOTIFY_H
#define LCJ_NOTIFY_H

#include <inttypes.h>
#include <stdbool.h>

// IPC initialization and cleanup
bool initializeIPC();

void shutdownIPC();

// Packet notification functions
int notifyRequest(const uint8_t *message, size_t messageSize);

int notifyResponse(const uint8_t *message, size_t messageSize);

// Log notification to Kotlin layer
void notifyLog(int level, const char *tag, const char *message);

// IPC connection status
bool isIPCConnected();

// Constants for log levels (matching Kotlin LogLevel enum)
#define LOG_LEVEL_VERBOSE 0
#define LOG_LEVEL_DEBUG   1
#define LOG_LEVEL_INFO    2
#define LOG_LEVEL_WARN    3
#define LOG_LEVEL_ERROR   4

#endif
