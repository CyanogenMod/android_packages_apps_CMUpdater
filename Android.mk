LOCAL_PATH:= $(call my-dir)

ifneq ($(TARGET_RECOVERY_FSTAB),)
  recovery_fstab := $(strip $(wildcard $(TARGET_RECOVERY_FSTAB)))
else
  recovery_fstab := $(strip $(wildcard $(TARGET_DEVICE_DIR)/recovery.fstab))
endif

ALTERNATE_IS_INTERNAL := false
ifneq ($(recovery_fstab),)
  recovery_fstab := $(ANDROID_BUILD_TOP)/$(recovery_fstab)
  ifneq ($(shell grep "/emmc" $(recovery_fstab)),)
  ALTERNATE_IS_INTERNAL := true
  endif
endif

include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

ifeq ($(ALTERNATE_IS_INTERNAL), true)
  LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res-compat $(LOCAL_RESOURCE_DIR)
endif

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := dashclockapi

LOCAL_PACKAGE_NAME := CMUpdater

LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

include $(BUILD_PACKAGE)

##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := dashclockapi:libs/dashclock-api-r1.1.jar

include $(BUILD_MULTI_PREBUILT)
