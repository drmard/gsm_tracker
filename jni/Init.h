#ifndef GSM_TRACKER_INIT_H
#define GSM_TRACKER_INIT_H

#include <jni.h>
#include "android/log.h"
#include <errno.h>

#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

class JNI {
public:
    static JavaVM *cachedJVM;
    static JNIEnv *Env;
    static jclass gTrackerNative;
};

#endif