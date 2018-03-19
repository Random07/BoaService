LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := BoaService
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)
