#ifndef GSM_TRACKER_JNI_HELPER_H
#define GSM_TRACKER_JNI_HELPER_H

#include <jni.h>
#include <sys/types.h>
#include "android/log.h"

#define TAG              "NATIVE_TRACKER"
#define LOG_NDEBUG       1 

class JNI_Helper {

private:
    static jobject mAssetManager;
    static jobject mClassObj;
    static jfieldID sJniTrackerHelper;
    static void checkAndClearExceptionFromCallback(JNIEnv *e,
        const char *methodName);

public:
    static off_t length;
    static off_t length_pkey;
    static void setNativeAssetManager(JNIEnv *env, jobject clazz, jobject assetManager);
    static jbyte *openAsset(const char *path); 
    static void setPrefixPath(void *array, int length);

};

#endif

