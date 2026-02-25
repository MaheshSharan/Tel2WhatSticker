package com.maheshsharan.tel2what.engine.encoder

import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AnimatedWebpEncoder {

    private companion object {
        const val TAG = "Tel2What:NativeEncoder"

        init {
            try {
                System.loadLibrary("native_webp_encoder")
                Log.i(TAG, "Successfully loaded libnative_webp_encoder.so into Dalvik memory.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "FATAL: Failed to load libnative_webp_encoder.so", e)
            }
        }
    }

    /**
     * Muxes an arbitrary sequence of Android Bitmaps into an Animated WebP file natively.
     * 
     * @param bitmaps An array of rendered ARGB_8888 Bitmaps padding to 512x512.
     * @param durationsMs The presentation duration (in milliseconds) for each corresponding frame.
     * @param outputFile The destination file where the binary WebP data should be dumped.
     * @param targetQuality (0-100) The lossy compression quality. Determines final file size.
     * @return true if encoding and IO write succeeded, false otherwise.
     */
    fun encode(
        bitmaps: Array<Bitmap>,
        durationsMs: IntArray,
        outputFile: File,
        targetQuality: Int = 90
    ): Boolean {
        if (bitmaps.isEmpty() || durationsMs.isEmpty()) {
            Log.e(TAG, "Frames array is empty.")
            return false
        }
        
        if (bitmaps.size != durationsMs.size) {
            Log.e(TAG, "Frame count mismatch: ${bitmaps.size} vs ${durationsMs.size}")
            return false
        }

        val width = bitmaps[0].width
        val height = bitmaps[0].height

        // Cross the JNI Bridge into C++ libwebp
        val encodedBytes = encodeAnimatedWebpNative(
            bitmaps,
            durationsMs,
            width,
            height,
            targetQuality
        )

        return if (encodedBytes != null && encodedBytes.isNotEmpty()) {
            try {
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                FileOutputStream(outputFile).use { fos ->
                    fos.write(encodedBytes)
                }
                Log.i(TAG, "Successfully wrote ${encodedBytes.size} bytes natively to ${outputFile.name}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed IO write for encoded Animated WebP", e)
                false
            }
        } else {
            Log.e(TAG, "JNI Encoder returned null or empty byte array.")
            false
        }
    }

    /**
     * The actual external C/C++ boundary binding mapped inside `webp_native_bridge.cpp`.
     */
    private external fun encodeAnimatedWebpNative(
        bitmaps: Array<Bitmap>,
        durationsMs: IntArray,
        width: Int,
        height: Int,
        quality: Int
    ): ByteArray?
}
