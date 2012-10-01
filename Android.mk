LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += src/com/cyanogenmod/updater/interfaces/IUpdateCheckServiceCallback.aidl \
		src/com/cyanogenmod/updater/interfaces/IUpdateCheckService.aidl

LOCAL_PACKAGE_NAME := CMUpdater

LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

include $(BUILD_PACKAGE)
