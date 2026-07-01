package com.maheshsharan.tel2what.engine.frame

import android.graphics.Bitmap
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrameNormalizerTest {

    @Test
    fun `normalizeToSubCanvas with already 512x512 image returns same bitmap`() {
        val source = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertSame("Should return same bitmap when dimensions match", source, result)
        assertEquals(512, result.width)
        assertEquals(512, result.height)
    }

    @Test
    fun `normalizeToSubCanvas with portrait image maintains aspect ratio`() {
        val source = Bitmap.createBitmap(300, 600, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertEquals("Width should be 512", 512, result.width)
        assertEquals("Height should be 512", 512, result.height)

        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas with landscape image maintains aspect ratio`() {
        val source = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertEquals("Width should be 512", 512, result.width)
        assertEquals("Height should be 512", 512, result.height)

        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas with very small image scales up correctly`() {
        val source = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertEquals("Width should be 512", 512, result.width)
        assertEquals("Height should be 512", 512, result.height)

        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas with very large image scales down correctly`() {
        val source = Bitmap.createBitmap(2048, 2048, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertEquals("Width should be 512", 512, result.width)
        assertEquals("Height should be 512", 512, result.height)

        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas with extreme portrait maintains aspect ratio`() {
        val source = Bitmap.createBitmap(100, 1000, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertEquals("Width should be 512", 512, result.width)
        assertEquals("Height should be 512", 512, result.height)

        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas with extreme landscape maintains aspect ratio`() {
        val source = Bitmap.createBitmap(1000, 100, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertEquals("Width should be 512", 512, result.width)
        assertEquals("Height should be 512", 512, result.height)

        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas with recycleOriginal true does not recycle when same bitmap`() {
        val source = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = true)

        assertSame("Should return same bitmap", source, result)
        assertFalse("Original bitmap should not be recycled when it's the result", result.isRecycled)
    }

    @Test
    fun `normalizeToSubCanvas with recycleOriginal true recycles when different bitmap`() {
        val source = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = true)

        assertNotSame("Should return different bitmap", source, result)
        assertTrue("Original bitmap should be recycled", source.isRecycled)
        assertFalse("Result bitmap should not be recycled", result.isRecycled)

        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas with recycleOriginal false does not recycle original`() {
        val source = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertFalse("Original bitmap should not be recycled", source.isRecycled)
        assertFalse("Result bitmap should not be recycled", result.isRecycled)

        source.recycle()
        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas creates bitmap with correct config`() {
        val source = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertEquals("Result should have ARGB_8888 config", Bitmap.Config.ARGB_8888, result.config)

        source.recycle()
        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas with 1x1 image works`() {
        val source = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 512, 512, recycleOriginal = false)

        assertEquals("Width should be 512", 512, result.width)
        assertEquals("Height should be 512", 512, result.height)

        source.recycle()
        result.recycle()
    }

    @Test
    fun `normalizeToSubCanvas with non-square target dimensions`() {
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val result = FrameNormalizer.normalizeToSubCanvas(source, 800, 600, recycleOriginal = false)

        assertEquals("Width should match target", 800, result.width)
        assertEquals("Height should match target", 600, result.height)

        source.recycle()
        result.recycle()
    }
}
