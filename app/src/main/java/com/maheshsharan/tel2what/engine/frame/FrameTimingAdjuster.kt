package com.maheshsharan.tel2what.engine.frame

import android.util.Log

/**
 * Manipulates the flow of time by decimating excess frames.
 */
object FrameTimingAdjuster {

    private const val TAG = "Tel2What:FrameAdjuster"
    private const val MIN_FRAME_DURATION_MS = 8 // WhatsApp minimum frame duration requirement

    /**
     * Decimates a sequence of frames by a specified target FPS drop, 
     * accumulating duration timings forward natively into the retained frames.
     */
    fun decimateFps(
        originalFrames: List<FrameData>,
        currentFps: Int,
        targetFps: Int
    ): List<FrameData> {
        if (targetFps >= currentFps || originalFrames.isEmpty()) return originalFrames

        val scaleFactor = currentFps.toFloat() / targetFps
        val result = mutableListOf<FrameData>()

        var accumulatedDurationMs = 0
        var skipCounter = 0f
        var globalDurationMs = 0

        for (i in originalFrames.indices) {
            val frame = originalFrames[i]
            
            skipCounter += (1f / scaleFactor)

            if (skipCounter >= 1f) {
                var newDuration = frame.durationMs + accumulatedDurationMs

                if (newDuration < MIN_FRAME_DURATION_MS) newDuration = MIN_FRAME_DURATION_MS

                // HARD CONSTRAINT: Global animation duration absolutely cannot exceed 10.000 seconds
                if (globalDurationMs + newDuration > 10000) {
                    newDuration = 10000 - globalDurationMs
                    if (newDuration > 0) {
                        result.add(FrameData(frame.bitmap, newDuration))
                    }
                    Log.w(TAG, "Capped animation structurally at exact 10000ms limit.")
                    break
                }
                
                result.add(FrameData(frame.bitmap, newDuration))
                globalDurationMs += newDuration
                accumulatedDurationMs = 0
                skipCounter -= 1f
            } else {
                // Drop this frame index, BUT absorb its native duration delay
                accumulatedDurationMs += frame.durationMs
                
                // Track global progression even on dropped frames, so the overall animation timeframe never deviates
                // Wait. We are carrying it forward so it adds to global when accepted.
            }
            
            // Note: We DO NOT recycle the dropped bitmaps here because they are often 
            // pointers pointing to the master Array reference. Recycling is controlled
            // by the top level Engine orchestrator at the very end of the JNI encoder loop.
        }

        Log.i(TAG, "Decimated frames from ${originalFrames.size} (@${currentFps}fps) down to ${result.size} (@${targetFps}fps)")
        return result
    }
}
