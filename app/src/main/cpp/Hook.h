#ifndef LCJ_HOOK_H
#define LCJ_HOOK_H

#define TARGET_LIBRARY "libnative.so"
#define TARGET_SYMBOL_COMPRESSOR "LZ4_compress_default_ext"
#define TARGET_SYMBOL_DECOMPRESSOR "LZ4_decompress_safe_ext"

// Type definitions for target functions
typedef int (*Compressor)(const char *src, char *dst, int srcSize, int dstCapacity);

typedef int (*Decompressor)(const char *src, char *dst, int compressedSize, int dstCapacity);

#endif
