LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := trackgsm

LIB_PATH := $(LOCAL_PATH)/../libs/$(TARGET_ARCH_ABI)/
LOCAL_CPPFLAGS := -std=gnu++11 -Wall -fPIE -pie -fpermissive
LOCAL_SRC_FILES += JNI_Helper.cpp Init.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += ${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++/4.9/include
LOCAL_C_INCLUDES += ${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++/4.9/libs/armeabi-v7a/include
LOCAL_C_INCLUDES += ${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++/4.9/libs/arm64-v8a/include

#LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_LDLIBS := -L$(LIB_PATH) -landroid -llog -lz -lm

include $(BUILD_SHARED_LIBRARY)
