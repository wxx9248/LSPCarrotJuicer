#include <dlfcn.h>

#include "Log.h"
#include "Util.h"
#include "Hook.h"
#include "Notify.h"

#define TARGET_LIBRARY "libnative.so"
#define TARGET_SYMBOL_COMPRESSOR "LZ4_compress_default_ext"
#define TARGET_SYMBOL_DECOMPRESSOR "LZ4_decompress_safe_ext"

// Type definitions for target functions
typedef int (*Compressor)(const char *src, char *dst, int srcSize, int dstCapacity);

typedef int (*Decompressor)(const char *src, char *dst, int compressedSize, int dstCapacity);

// Prototypes
static int hookedCompressor(const char *src, char *dst, int srcSize, int dstCapacity);

static int hookedDecompressor(const char *src, char *dst, int compressedSize, int dstCapacity);

static void onLibraryLoaded(const char *name, void *handle);

// Global variables
static HookFunction hook = nullptr;
static bool isHooked = false;
static Compressor originalCompressor = nullptr;
static Decompressor originalDecompressor = nullptr;

// Entry point here
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
#define LOG_TAG "LCJ/Native/native_init"
    hook = entries->hook;
    return onLibraryLoaded;
#undef LOG_TAG
}

void onLibraryLoaded(const char *name, void *handle) {
#define LOG_TAG "LCJ/Native/onLibraryLoaded"
    if (isHooked) {
        return;
    }

    if (!endsWith(name, TARGET_LIBRARY)) {
        // Not what we want
        return;
    }
    LOG_D(LOG_TAG, "Got handle for %s at %p", TARGET_LIBRARY, handle);
    LOG_D(LOG_TAG, "Library path: %s", name);

    Compressor compressor = (Compressor) dlsym(handle, TARGET_SYMBOL_COMPRESSOR);
    if (!compressor) {
        LOG_E(LOG_TAG, "Cannot get handle for %s", TARGET_SYMBOL_COMPRESSOR);
        return;
    }
    LOG_D(LOG_TAG, "Got handle for %s at %p", TARGET_SYMBOL_COMPRESSOR, compressor);

    Decompressor decompressor = (Decompressor) dlsym(handle, TARGET_SYMBOL_DECOMPRESSOR);
    if (!decompressor) {
        LOG_E(LOG_TAG, "Cannot get handle for %s", TARGET_SYMBOL_DECOMPRESSOR);
        return;
    }
    LOG_D(LOG_TAG, "Got handle for %s at %p", TARGET_SYMBOL_DECOMPRESSOR, decompressor);

    hook((void *) compressor, (void *) hookedCompressor, (void **) &originalCompressor);
    LOG_D(LOG_TAG, "Compressor hooked to %p (original at %p)", hookedCompressor,
          originalCompressor);

    hook((void *) decompressor, (void *) hookedDecompressor, (void **) &originalDecompressor);
    LOG_D(LOG_TAG, "Decompressor hooked to %p (original at %p)", hookedDecompressor,
          originalDecompressor);

    LOG_I(LOG_TAG, "Native hook completed");
    isHooked = true;
#undef LOG_TAG
}

// Hooked functions
int hookedCompressor(const char *src, char *dst, int srcSize, int dstCapacity) {
#define LOG_TAG "LCJ/Native/hookedCompressor"
    int code = originalCompressor(src, dst, srcSize, dstCapacity);
    if (code < 0) {
        LOG_E(LOG_TAG, "Compressor returned negative code (%d)", code);
        return code;
    }
    LOG_I(LOG_TAG, "Got unencrypted message at %p: attempting to notify", src);
    notifyRequest((const uint8_t *) src, (size_t) srcSize);
    return code;
#undef LOG_TAG
}

int hookedDecompressor(const char *src, char *dst, int compressedSize, int dstCapacity) {
#define LOG_TAG "LCJ/Native/hookedDecompressor"
    int code = originalDecompressor(src, dst, compressedSize, dstCapacity);
    if (code < 0) {
        LOG_E(LOG_TAG, "Decompressor returned negative code (%d)", code);
        return code;
    }
    LOG_I(LOG_TAG, "Got unencrypted message at %p: attempting to notify", dst);
    notifyResponse((const uint8_t *) dst, (size_t) dstCapacity);
    return code;
#undef LOG_TAG
}
