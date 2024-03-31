#ifndef LCJ_LOG_H
#define LCJ_LOG_H

#include <android/log.h>

#define  LOG_E(...)  __android_log_print(ANDROID_LOG_ERROR, __VA_ARGS__)
#define  LOG_W(...)  __android_log_print(ANDROID_LOG_WARN, __VA_ARGS__)
#define  LOG_D(...)  __android_log_print(ANDROID_LOG_DEBUG, __VA_ARGS__)
#define  LOG_I(...)  __android_log_print(ANDROID_LOG_INFO, __VA_ARGS__)

#endif
