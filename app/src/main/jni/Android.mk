LOCAL_PATH := $(call my-dir)

# 1. Path settings
NDPI_ROOT := $(LOCAL_PATH)/submodules/nDPI
NDPI_SRC := $(NDPI_ROOT)/src/lib
NDPI_THIRD_PARTY_SRC := $(NDPI_ROOT)/src/lib/third_party/src

# --- capture module definition ---
include $(CLEAR_VARS)
LOCAL_MODULE := capture
LOCAL_SHORT_COMMANDS := true

LOCAL_LDLIBS := -llog

# 2. C_INCLUDES
LOCAL_C_INCLUDES := \
    $(NDPI_ROOT)/src/include \
    $(NDPI_ROOT)/src/lib/third_party/include

# 3. Compiler flags
LOCAL_CFLAGS := \
    -DNDPI_LIB_COMPILATION \
    -D__bswap_64=bswap_64 \
    -DNDPI_SLIM

# 4. Source file collection
NDPI_ALL_C_FILES := $(wildcard $(NDPI_SRC)/*.c)
NDPI_ALL_C_FILES += $(wildcard $(NDPI_THIRD_PARTY_SRC)/*.c)
NDPI_ALL_C_FILES += $(wildcard $(NDPI_THIRD_PARTY_SRC)/hll/*.c)
NDPI_ALL_C_FILES += $(wildcard $(NDPI_ROOT)/src/lib/protocols/*.c)

# Convert to relative paths
NDPI_CLEAN_C_FILES := $(NDPI_ALL_C_FILES:$(LOCAL_PATH)/%=%)

# 5. Excluded files
LOCAL_EXCLUDE_FILES_RELATIVE := \
    submodules/nDPI/src/lib/third_party/src/libinjection_html5.c \
    submodules/nDPI/src/lib/third_party/src/libinjection_xss.c \
    submodules/nDPI/src/lib/third_party/src/libinjection_sqli.c \
    submodules/nDPI/src/lib/third_party/src/roaring.c \
    submodules/nDPI/src/lib/third_party/src/roaring_v2.c \
    submodules/nDPI/src/lib/ndpi_bitmap.c \
    submodules/nDPI/src/lib/ndpi_bitmap64_fuse.c \
    submodules/nDPI/src/lib/ndpi_binary_bitmap.c \
    submodules/nDPI/src/lib/ndpi_filter.c

# Filter out excluded files
NDPI_FINAL_C_FILES := $(filter-out $(LOCAL_EXCLUDE_FILES_RELATIVE), $(NDPI_CLEAN_C_FILES))


# 6. Final LOCAL_SRC_FILES
LOCAL_SRC_FILES := \
    $(NDPI_FINAL_C_FILES) \
    ndpi_config.c \
    vpnrelay.c \
    zdtun.c \
    utils.c \
    common/jni_utils.c \
    common/uid_resolver.c \
    common/utils.c \
    ip_lru.c \
    log_writer.c \
    port_map.c \
    third_party/libchash.c \
    blacklist.c

# Build shared library
include $(BUILD_SHARED_LIBRARY)
