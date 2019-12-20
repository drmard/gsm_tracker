#include <Init.h>
#include <JNI_Helper.h>
#include <android/asset_manager_jni.h>
#include <sys/types.h>
#include <string>

#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

off_t JNI_Helper::length = 0;
off_t JNI_Helper::length_pkey = 0;
jobject JNI_Helper::mClassObj;
jobject JNI_Helper::mAssetManager;

void JNI_Helper::checkAndClearExceptionFromCallback(JNIEnv *e,
        const char *methodName) {
    if (e->ExceptionCheck()) {
        LOGE("an exception was thrown by call '%s' ...", methodName);
        e->ExceptionClear();
    }
}

void JNI_Helper::setNativeAssetManager(JNIEnv *env, jobject clazz,
        jobject assetManager) {
    JNI_Helper::mClassObj = (jobject)env->NewGlobalRef(clazz);
    JNI_Helper::mAssetManager = (jobject)env->NewGlobalRef(assetManager);
}

jbyte *JNI_Helper::openAsset(const char *path) {
    AAssetManager* manager = AAssetManager_fromJava(JNI::Env,
            JNI_Helper::mAssetManager);
    if (manager == NULL) {
        LOGI("ERROR: failed to get asset manager");
        return NULL;
    }

    AAsset* asset = AAssetManager_open(manager, path, AASSET_MODE_UNKNOWN);    
    if(asset == NULL) {
        LOGE("couldn't load asset %s", path);
        return NULL;
    }
    off_t len = AAsset_getLength(asset);

    std::string s_path(path);
    size_t fnd = s_path.find("token");
    if (fnd != std::string::npos)
        JNI_Helper::length = len;

    fnd = s_path.find("pubkey");
    if (fnd != std::string::npos)
        JNI_Helper::length_pkey = len;
 
    jbyte* buffer = new jbyte[len];
    int num_read = AAsset_read(asset, buffer, len);

    AAsset_close(asset);

    if (num_read != len) {
        LOGE ("error: read asset failed \n");
        delete[] buffer;
        return NULL;
    }

    return buffer;
}

void JNI_Helper::setPrefixPath(void *array, int length) {

    jbyteArray data = NULL;
    data = JNI::Env->NewByteArray(length);

    if (data) {
        JNI::Env->SetByteArrayRegion((jbyteArray)JNI::Env->NewGlobalRef(data),
            0, length, (const jbyte *)array);
    } else {
        LOGE("ERROR: (%s) unable allocate memory for path data",
            __FUNCTION__);
        return;
    }

    if (!JNI::gTrackerNative)
        return;

    jmethodID methodSetPath =
    JNI::Env->GetStaticMethodID(JNI::gTrackerNative, "setPath", "([B)V");
    if(!methodSetPath) {
        LOGE("failed to get method ID : \"setPath\"");
        JNI::Env->DeleteLocalRef(data);
        return;
    }

    JNI::Env->CallStaticVoidMethod(JNI::gTrackerNative, methodSetPath,
        (jbyteArray)JNI::Env->NewGlobalRef(data));
    JNI_Helper::checkAndClearExceptionFromCallback(JNI::Env,
        __FUNCTION__);
}
