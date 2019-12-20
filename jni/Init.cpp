#include <Init.h>
#include <JNI_Helper.h>
#include <stdlib.h>

JavaVM* JNI::cachedJVM = NULL;
JNIEnv* JNI::Env = NULL;
jclass  JNI::gTrackerNative = NULL;

static void setNativeAAssetManager(JNIEnv *env, jobject clazz,
        jobject assetManager) {
    JNI_Helper::setNativeAssetManager(env, clazz, assetManager);
}

static void getPrefixPathFromAsset (JNIEnv *env, jclass clazz) {
    jbyte * expath = JNI_Helper::openAsset("extra/t_token");
    if (NULL == expath) {
        LOGE("ERROR: failed to open asset file\n");
        return;
    }
    if (JNI_Helper::length > 0) {
        JNI_Helper::setPrefixPath((void *)expath,JNI_Helper::length);
    }
}

static void getPublicKeyFromAsset(JNIEnv *env, jobject clazz) {
    jbyte * pkey = JNI_Helper::openAsset("extra/pubkey");
    if (NULL == pkey) {
        LOGE("ERROR: failed to open asset file\n");
        return;
    }
    if (JNI_Helper::length_pkey > 0) {
        JNI_Helper::setPrefixPath((void *)pkey,JNI_Helper::length_pkey);
    }
}

static JNINativeMethod native_methods[] = {
        { "getPKey", "()V", (void *)getPublicKeyFromAsset },
        { "getPrefixPath", "()V", (void *)getPrefixPathFromAsset },
        { "setAssetManager", "(Landroid/content/res/AssetManager;)V",
                (void *)setNativeAAssetManager },
};

/*
 * This method register JNI native methods. 
 */
static int registerNativeMethods(jclass claz) {
    JNIEnv *env = JNI::Env;
    if (env->RegisterNatives(claz, native_methods,
            sizeof(native_methods) / sizeof(JNINativeMethod)) < 0) {
        LOGE("error %d (%s) register native methods failed\n", errno, strerror(errno));
        env->DeleteLocalRef(claz);
    	return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * This method is called automatically by the android when 
 * library is loaded. We can register here our native
 * methods.
 */
JNIEXPORT
jint
JNI_OnLoad(JavaVM* jvm, void* reserved)
{ 
    JNI::cachedJVM = jvm;
    JNIEnv *env = NULL;
    LOGI("%s: ...",__FUNCTION__);
    
    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOGI("ERROR: (%s): failed to get pointer to JNI environment\n",
            __FUNCTION__);
        return JNI_ERR;
    }
    JNI::Env = env;
    jclass clazz = env->FindClass("com/hsp/gsm/cell/tracker/TrackerNative");
    if (clazz) {
        JNI::gTrackerNative = (jclass)env->NewGlobalRef(clazz);

    }
    if (clazz) {
        if (!registerNativeMethods(clazz)) {
            return JNI_ERR; 
        }
    }
    env->DeleteLocalRef(clazz);

    return JNI_VERSION_1_6;
}
