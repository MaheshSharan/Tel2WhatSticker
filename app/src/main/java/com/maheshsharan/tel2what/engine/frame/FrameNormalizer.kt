package com.maheshsharan.tel2what.engine.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff

/**
 * Normalizes arbitrary bitmaps to WhatsApp's required 512×512 dimensions.
 *
 * Handles images of any size or aspect ratio by:
 * - Scaling to fit within target bounds while preserving aspect ratio
 * - Adding transparent padding (letterboxing/pillarboxing) to reach exact dimensions
 * - Maintaining ARGB_8888 color format for transparency support
 *
 * WhatsApp requires stickers to be exactly 512×512 pixels. This normalizer
 * ensures all frames meet that constraint regardless of source dimensions.
 */
object FrameNormalizer {

    /**
     * Normalizes a bitmap to target dimensions with aspect ratio preservation.
     *
     * @param source The input bitmap (any size or aspect ratio)
     * @param targetWidth The required output width (typically 512)
     * @param targetHeight The required output height (typically 512)
     * @param recycleOriginal Whether to recycle the source bitmap after processing
     * @return A new bitmap at exact target dimensions, or the original if already correct size
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
