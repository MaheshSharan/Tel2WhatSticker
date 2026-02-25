cmake_minimum_required(VERSION 3.22.1)

project("tel2what_native_engine")

# Define the root of our local libwebp repository
set(WEBP_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/libwebp/src)

# Include directories needed for libwebp
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/libwebp
    ${WEBP_SRC_DIR}
)

# 1. Compile libwebp core (Decoders, Encoders, DSP, Utils, and SharpYuv)
# We add exactly what is required to build WebPAnimEncoder from the WebP source.
file(GLOB_RECURSE WEBP_SRC_FILES
    "${WEBP_SRC_DIR}/dec/*.c"
    "${WEBP_SRC_DIR}/demux/*.c"
    "${WEBP_SRC_DIR}/dsp/*.c"
    "${WEBP_SRC_DIR}/enc/*.c"
    "${WEBP_SRC_DIR}/mux/*.c"
    "${WEBP_SRC_DIR}/utils/*.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/libwebp/sharpyuv/*.c"
)

# Create a static library for libwebp so it doesn't pollute the final shared namespace
add_library(libwebp STATIC ${WEBP_SRC_FILES})

# 2. Compile our custom Android JNI Bridge
add_library(
    native_webp_encoder
    SHARED
    webp_native_bridge.cpp
)

# 3. Link Android system libraries and libwebp into our dynamic bridge
target_link_libraries(
    native_webp_encoder
    libwebp
    android     # For reading from Android Bitmaps natively if needed
    jnigraphics # For ARGB_8888 bitmap locking
    log         # For native Android Logcat
)
