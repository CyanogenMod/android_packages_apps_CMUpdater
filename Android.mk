LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v7-recyclerview \
    android-support-v13 \
    android-support-v17-leanback \
    org.cyanogenmod.platform.internal \
    volley

LOCAL_RESOURCE_DIR := \
    $(TOP)/frameworks/support/v17/leanback/res \
    $(LOCAL_PATH)/res

LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages android.support.v17.leanback

LOCAL_PACKAGE_NAME := CMUpdater

LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)
