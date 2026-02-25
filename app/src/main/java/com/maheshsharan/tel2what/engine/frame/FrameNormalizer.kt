package com.maheshsharan.tel2what.engine.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff

/**
 * Normalizes unpredictable arbitrary bitmaps into standard constrained frames.
 */
object FrameNormalizer {

    /**
     * Scales an arbitrary Bitmap to fit exactly within the target WhatsApp dimensions
     * while flawlessly maintaining the aspect ratio. Translucent padding handles the letterboxing.
     */
    fun normalizeToSubCanvas(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        recycleOriginal: Boolean = true
    ): Bitmap {
        // Optimization: if it's already exactly the WhatsApp specification, do nothing
        if (source.width == targetWidth && source.height == targetHeight) {
            return source 
        }

        val scale = minOf(
            targetWidth.toFloat() / source.width,
            targetHeight.toFloat() / source.height
        )

        val scaledWidth = (source.width * scale).toInt()
        val scaledHeight = (source.height * scale).toInt()

        val leftOffset = (targetWidth - scaledWidth) / 2f
        val topOffset = (targetHeight - scaledHeight) / 2f

        // First apply the aspect-ratio correct scale
        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        
        // Then initialize the bounding box canvas that WhatsApp expects
        val targetCanvasBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(targetCanvasBitmap)
        
        // The background of letterbox edges MUST be completely transparent (ARGB)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        
        // Stamp the correctly scaled image inside the bounding box
        canvas.drawBitmap(scaledBitmap, leftOffset, topOffset, null)

        scaledBitmap.recycle()

        // Clean up the original dirty frame if it's discarded to rescue heap fragmentation
        if (recycleOriginal && targetCanvasBitmap !== source) {
            source.recycle()
        }

        return targetCanvasBitmap
    }
}
