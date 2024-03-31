#ifndef LCJ_LSP_HOOK_H
#define LCJ_LSP_HOOK_H

#include <stdint.h>

typedef int (*HookFunction)(void *function, void *replace, void **backup);

typedef int (*UnhookFunction)(void *function);

typedef void (*NativeOnModuleLoaded)(const char *name, void *handle);

typedef struct {
    uint32_t version;
    HookFunction hook;
    UnhookFunction unhook;
} NativeAPIEntries;

typedef NativeOnModuleLoaded (*NativeInit)(const NativeAPIEntries *entries);

#endif
