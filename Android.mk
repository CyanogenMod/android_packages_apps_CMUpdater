LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-design \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    org.cyanogenmod.platform.internal \
    volley

LOCAL_RESOURCE_DIR := \
    $(TOP)/frameworks/support/design/res \
    $(TOP)/frameworks/support/v7/appcompat/res \
    $(TOP)/frameworks/support/v7/cardview/res \
    $(TOP)/frameworks/support/v7/preference/res \
    $(TOP)/frameworks/support/v7/recyclerview/res \
    $(LOCAL_PATH)/res

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.design \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.v7.preference \
    --extra-packages android.support.v7.recyclerview \
    --extra-packages android.support.v7.cardview

LOCAL_AAPT_FLAGS += --rename-manifest-package org.lineageos.updater

LOCAL_PACKAGE_NAME := CMUpdater

LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)
