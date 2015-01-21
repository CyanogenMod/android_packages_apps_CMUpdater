#
# Copyright (C) 2012-2015 The CyanogenMod Project
#
# Licensed under the GNU GPLv2 license
#
# The text of the license can be found in the LICENSE file
# or at https://www.gnu.org/licenses/gpl-2.0.txt
#

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
LOCAL_STATIC_JAVA_LIBRARIES := volley

LOCAL_PACKAGE_NAME := CMUpdater

LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)
