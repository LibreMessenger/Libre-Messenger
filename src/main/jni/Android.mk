LOCAL_PATH := $(call my-dir)

LOCAL_SRC_FILES     += \
./libyuv/source/compare_common.cc \
./libyuv/source/compare_neon.cc \
./libyuv/source/compare_posix.cc \
./libyuv/source/compare_win.cc \
./libyuv/source/compare.cc \
./libyuv/source/convert_argb.cc \
./libyuv/source/convert_from_argb.cc \
./libyuv/source/convert_from.cc \
./libyuv/source/convert_jpeg.cc \
./libyuv/source/convert_to_argb.cc \
./libyuv/source/convert_to_i420.cc \
./libyuv/source/convert.cc \
./libyuv/source/cpu_id.cc \
./libyuv/source/format_conversion.cc \
./libyuv/source/mjpeg_decoder.cc \
./libyuv/source/mjpeg_validate.cc \
./libyuv/source/planar_functions.cc \
./libyuv/source/rotate_argb.cc \
./libyuv/source/rotate_mips.cc \
./libyuv/source/rotate_neon.cc \
./libyuv/source/rotate_neon64.cc \
./libyuv/source/rotate.cc \
./libyuv/source/row_any.cc \
./libyuv/source/row_common.cc \
./libyuv/source/row_mips.cc \
./libyuv/source/row_neon.cc \
./libyuv/source/row_neon64.cc \
./libyuv/source/row_posix.cc \
./libyuv/source/row_win.cc \
./libyuv/source/scale_argb.cc \
./libyuv/source/scale_common.cc \
./libyuv/source/scale_mips.cc \
./libyuv/source/scale_neon.cc \
./libyuv/source/scale_neon64.cc \
./libyuv/source/scale_posix.cc \
./libyuv/source/scale_win.cc \
./libyuv/source/scale.cc \
./libyuv/source/video_common.cc

LOCAL_SRC_FILES     += \
./jni.c \
./video.c

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)