package com.maheshsharan.tel2what.engine.decoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieDrawable
import com.maheshsharan.tel2what.engine.frame.FrameData
import kotlinx.coroutines.yield

/**
 * Renders Lottie Compositions into discrete, timed Bitmaps.
 */
object FrameExtractor {

    private const val TAG = "Tel2What:FrameExtractor"

    /**
     * Extracts scaled, padded 512x512 transparent bitmaps dynamically from a Lottie Composition.
     * Enforces Duration Limits and extracts at the requested continuous framerate.
     */
    suspend fun extractFromLottie(
        composition: LottieComposition,
        targetWidth: Int,
        targetHeight: Int,
        targetFps: Int,
        maxDurationMs: Long
    ): List<FrameData> {

        Log.i(TAG, "FrameExtractor: Starting Lottie frame extraction")
        Log.i(TAG, "FrameExtractor: Target dimensions: ${targetWidth}x${targetHeight}")
        Log.i(TAG, "FrameExtractor: Target FPS: $targetFps")
        Log.i(TAG, "FrameExtractor: Max duration: ${maxDurationMs}ms")
        
        val frames = mutableListOf<FrameData>()

        try {
            val lottieDrawable = LottieDrawable()
            lottieDrawable.composition = composition
            
            Log.i(TAG, "FrameExtractor: Lottie composition duration: ${composition.duration}ms")
            Log.i(TAG, "FrameExtractor: Lottie composition bounds: ${composition.bounds}")
            
            // Native scaling boundaries. This satisfies normalization automatically.
            lottieDrawable.setBounds(0, 0, targetWidth, targetHeight)

            val nativeDurationMs = composition.duration.toLong()
            val finalDurationMs = minOf(nativeDurationMs, maxDurationMs)

            val frameDelayMs = 1000 / targetFps
            var frameCount = (finalDurationMs / frameDelayMs).toInt()
            
            // WhatsApp validator requires animated stickers to have more than 1 frame.
            if (frameCount < 2) {
                Log.w(TAG, "FrameExtractor: Frame count was $frameCount, forcing to 2 minimum")
                frameCount = 2
            }

            Log.i(TAG, "FrameExtractor: Will extract $frameCount frames at $targetFps FPS (Duration: ${finalDurationMs}ms)")

            for (i in 0 until frameCount) {
                yield() // Suspend briefly and gracefully abort if the parent job was cancelled (e.g., App killed or rotated)
                
                // Ensure every frame is a fresh ARGB_8888 bitmap with full alpha bounds.
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Transparent slate
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                val progress = (i) / frameCount.toFloat()
                lottieDrawable.progress = progress
                lottieDrawable.draw(canvas)

                // Cache Frame to sequence
                frames.add(FrameData(bitmap = bitmap, durationMs = frameDelayMs))
            }

            Log.i(TAG, "FrameExtractor: SUCCESS - Extracted ${frames.size} frames from Lottie")
            Log.i(TAG, "FrameExtractor: Total duration: ${frames.sumOf { it.durationMs }}ms")

            // The final frame needs to hit the exact 10 second cap, so its duration might be truncated.
            // But for simple TGS extraction, uniform duration is standard.
            
            return frames
            
        } catch (e: Exception) {
            Log.e(TAG, "FrameExtractor: Exception extracting frames from Lottie - ${e.message}", e)
            
            // Cleanup on failure to prevent massive memory leaks
            for (frame in frames) {
                frame.bitmap.recycle()
            }
            return emptyList()
        }
    }
}
