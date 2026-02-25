#include <jni.h>
#include <string>
#include <vector>
#include <android/bitmap.h>
#include <android/log.h>
#include "webp/encode.h"
#include "webp/mux.h"

#define LOG_TAG "Tel2What:NativeWebp"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_maheshsharan_tel2what_engine_encoder_AnimatedWebpEncoder_encodeAnimatedWebpNative(
        JNIEnv *env, jobject thiz,
        jobjectArray bitmaps,
        jintArray durations_ms,
        jint width, jint height,
        jint quality) {

    jsize frame_count = env->GetArrayLength(bitmaps);
    jint *durations = env->GetIntArrayElements(durations_ms, nullptr);

    LOGI("Begin native encoding: %d frames, %dx%d, Q=%d", frame_count, width, height, quality);

    // Initialize WebP encoding
    WebPAnimEncoderOptions enc_options;
    if (!WebPAnimEncoderOptionsInit(&enc_options)) {
        LOGE("Failed to initialize WebPAnimEncoderOptions");
        env->ReleaseIntArrayElements(durations_ms, durations, JNI_ABORT);
        return nullptr;
    }
    
    // WhatsApp requires animated stickers to loop infinitely
    enc_options.anim_params.loop_count = 0;

    WebPAnimEncoder* enc = WebPAnimEncoderNew(width, height, &enc_options);
    if (enc == nullptr) {
        LOGE("Failed to create WebPAnimEncoder");
        env->ReleaseIntArrayElements(durations_ms, durations, JNI_ABORT);
        return nullptr;
    }

    WebPConfig config;
    if (!WebPConfigInit(&config)) {
        LOGE("Failed to initialize WebPConfig");
        WebPAnimEncoderDelete(enc);
        env->ReleaseIntArrayElements(durations_ms, durations, JNI_ABORT);
        return nullptr;
    }
    
    config.quality = quality;
    // Lossy encoding (to respect the massive 500KB cap)
    config.lossless = 0;
    // Balance encoding speed vs compression (method 0=fastest, 6=slowest)
    // Using method=1 for very fast encoding with acceptable quality
    config.method = 1; 

    // Time accumulators for timestamp passing to Muxer
    int timestamp_ms = 0;

    for (int i = 0; i < frame_count; ++i) {
        jobject bitmap = env->GetObjectArrayElement(bitmaps, i);
        
        AndroidBitmapInfo info;
        void* pixels;
        
        if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
            LOGE("Failed to get bitmap info for frame %d", i);
            continue;
        }

        if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            LOGE("Unsupported bitmap format. Require RGBA_8888.");
            continue;
        }

        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
            LOGE("Failed to lock pixels for frame %d", i);
            continue;
        }

        // Import straight from Android Pixel memory
        WebPPicture pic;
        if (!WebPPictureInit(&pic)) {
            LOGE("Failed to initialize WebPPicture");
            AndroidBitmap_unlockPixels(env, bitmap);
            continue;
        }
        
        pic.width = width;
        pic.height = height;
        pic.use_argb = 1;

        if (!WebPPictureImportRGBA(&pic, (const uint8_t*)pixels, info.stride)) {
            LOGE("Failed to import RGBA for frame %d", i);
            WebPPictureFree(&pic);
            AndroidBitmap_unlockPixels(env, bitmap);
            continue;
        }

        AndroidBitmap_unlockPixels(env, bitmap);

        // Add frame to Animated Muxer
        if (!WebPAnimEncoderAdd(enc, &pic, timestamp_ms, &config)) {
            LOGE("Failed to add frame %d to encoder. Error Code: %d", i, pic.error_code);
        }

        WebPPictureFree(&pic);
        env->DeleteLocalRef(bitmap);
        
        // Advance timestamp
        timestamp_ms += durations[i];
    }

    // Pass the final terminator frame to signal EOS (End Of Sequence)
    if (!WebPAnimEncoderAdd(enc, nullptr, timestamp_ms, nullptr)) {
        LOGE("Failed to add final terminator frame to encoder.");
    }

    // Compile into final binary WebP Data
    WebPData webp_data;
    WebPDataInit(&webp_data);
    
    if (!WebPAnimEncoderAssemble(enc, &webp_data)) {
        LOGE("Failed to assemble the final WebP animation");
        WebPAnimEncoderDelete(enc);
        env->ReleaseIntArrayElements(durations_ms, durations, JNI_ABORT);
        return nullptr;
    }

    LOGI("Successfully encoded Native WebP! Output Size: %zu bytes", webp_data.size);

    // Convert malloc'd WebPData back to JVM ByteArray
    jbyteArray result = env->NewByteArray(webp_data.size);
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, webp_data.size, (const jbyte*)webp_data.bytes);
    }

    // Cleanup
    WebPDataClear(&webp_data);
    WebPAnimEncoderDelete(enc);
    env->ReleaseIntArrayElements(durations_ms, durations, JNI_ABORT);

    return result;
}
