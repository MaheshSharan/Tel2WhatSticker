package com.maheshsharan.tel2what.engine.decoder

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.maheshsharan.tel2what.engine.frame.FrameData
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.min

/**
 * Simplified WebM decoder using ByteBuffer output instead of Surface rendering.
 * This avoids YUV/RGBA format mismatch issues.
 */
object WebmDecoderSimple {
    private const val TAG = "Tel2What:WebmDecoder"

    suspend fun decode(
        webmFile: File,
        targetFps: Int,
        maxDurationMs: Long
    ): List<FrameData> = withContext(Dispatchers.IO) {
        Log.i(TAG, "WebmDecoder: Starting decode of ${webmFile.name}")
        Log.i(TAG, "WebmDecoder: File exists=${webmFile.exists()}, size=${webmFile.length()} bytes")
        Log.i(TAG, "WebmDecoder: Target FPS=$targetFps, maxDuration=${maxDurationMs}ms")
        
        if (!webmFile.exists()) {
            Log.e(TAG, "WebmDecoder: ERROR - WEBM file does not exist!")
            return@withContext emptyList()
        }
        
        if (webmFile.length() == 0L) {
            Log.e(TAG, "WebmDecoder: ERROR - WEBM file is empty!")
            return@withContext emptyList()
        }
        
        val frames = mutableListOf<FrameData>()
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        
        try {
            Log.i(TAG, "WebmDecoder: Setting data source")
            extractor.setDataSource(webmFile.absolutePath)
            
            Log.i(TAG, "WebmDecoder: Selecting video track, total tracks=${extractor.trackCount}")
            val trackIndex = selectVideoTrack(extractor)
            if (trackIndex < 0) {
                Log.e(TAG, "WebmDecoder: ERROR - No video track found")
                return@withContext emptyList()
            }
            
            Log.i(TAG, "WebmDecoder: Video track found at index $trackIndex")
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val containerMime = format.getString(MediaFormat.KEY_MIME)
            
            if (containerMime == null) {
                Log.e(TAG, "WebmDecoder: ERROR - MIME type is null!")
                return@withContext emptyList()
            }
            
            Log.i(TAG, "WebmDecoder: Container MIME: $containerMime")
            
            val codecMime = if (containerMime == "video/webm") {
                "video/x-vnd.on2.vp9"
            } else {
                containerMime
            }
            
            Log.i(TAG, "WebmDecoder: Using codec MIME: $codecMime for decoder")
            
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            val finalDurationUs = min(durationUs, maxDurationMs * 1000L)
            
            val frameDelayMs = 1000 / targetFps
            val frameDelayUs = (frameDelayMs * 1000).toLong()
            
            val width = format.getInteger(MediaFormat.KEY_WIDTH)
            val height = format.getInteger(MediaFormat.KEY_HEIGHT)

            Log.i(TAG, "WebmDecoder: Video dimensions: ${width}x${height}")
            Log.i(TAG, "WebmDecoder: Video duration: ${durationUs}us (${durationUs/1000}ms)")
            
            Log.i(TAG, "WebmDecoder: Creating MediaCodec decoder for $codecMime")
            codec = try {
                MediaCodec.createDecoderByType(codecMime)
            } catch (e: Exception) {
                Log.e(TAG, "WebmDecoder: Failed to create decoder for $codecMime, trying VP8 fallback", e)
                try {
                    MediaCodec.createDecoderByType("video/x-vnd.on2.vp8").also {
                        Log.i(TAG, "WebmDecoder: Successfully created VP8 decoder as fallback")
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "WebmDecoder: VP8 fallback also failed", e2)
                    return@withContext emptyList()
                }
            }
            
            val mediaCodec = codec!!
            
            // Configure WITHOUT surface - decode to ByteBuffer
            mediaCodec.configure(format, null, null, 0)
            mediaCodec.start()
            Log.i(TAG, "WebmDecoder: MediaCodec started (ByteBuffer mode)")

            val info = MediaCodec.BufferInfo()
            var isEOS = false
            var outputEOS = false
            var nextTargetTimeUs = 0L
            var lastPresentationTimeUs = 0L

            Log.i(TAG, "WebmDecoder: Starting decode loop")

            while (!outputEOS) {
                yield()

                if (!isEOS) {
                    val inIndex = mediaCodec.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val buffer = mediaCodec.getInputBuffer(inIndex)
                        val sampleSize = extractor.readSampleData(buffer!!, 0)
                        if (sampleSize < 0) {
                            Log.d(TAG, "Input EOS reached")
                            mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            if (presentationTimeUs > finalDurationUs) {
                                Log.d(TAG, "Duration cap reached at ${presentationTimeUs}us")
                                mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isEOS = true
                            } else {
                                mediaCodec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIndex = mediaCodec.dequeueOutputBuffer(info, 10000)
                if (outIndex >= 0) {
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "Output EOS reached")
                        outputEOS = true
                    }

                    val doCapture = info.presentationTimeUs >= nextTargetTimeUs && info.presentationTimeUs <= finalDurationUs
                    
                    if (doCapture && info.size > 0) {
                        Log.v(TAG, "Capturing frame at ${info.presentationTimeUs}us")
                        
                        // Get the decoded frame from ByteBuffer
                        val outputBuffer = mediaCodec.getOutputBuffer(outIndex)
                        if (outputBuffer != null) {
                            // Convert YUV to RGB bitmap
                            val bitmap = yuvToRgbBitmap(outputBuffer, width, height, info)
                            if (bitmap != null) {
                                val duration = if (frames.isEmpty()) {
                                    frameDelayMs
                                } else {
                                    ((info.presentationTimeUs - lastPresentationTimeUs) / 1000).toInt().coerceAtLeast(8)
                                }
                                
                                frames.add(FrameData(bitmap, duration))
                                lastPresentationTimeUs = info.presentationTimeUs
                            }
                        }
                        
                        nextTargetTimeUs += frameDelayUs
                    }
                    
                    mediaCodec.releaseOutputBuffer(outIndex, false)
                }
            }

            Log.i(TAG, "WebmDecoder: SUCCESS - Generated ${frames.size} frames")
            Log.i(TAG, "WebmDecoder: Total duration: ${frames.sumOf { it.durationMs }}ms")
        } catch (e: Exception) {
            Log.e(TAG, "WebmDecoder: CATASTROPHIC FAILURE - ${e.message}", e)
            frames.forEach { it.bitmap.recycle() }
            frames.clear()
        } finally {
            try { codec?.stop(); codec?.release() } catch (e: Exception) {}
            try { extractor.release() } catch (e: Exception) {}
        }
        return@withContext frames
    }

    private fun selectVideoTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("video/")) {
                return i
            }
        }
        return -1
    }

    private fun yuvToRgbBitmap(buffer: ByteBuffer, width: Int, height: Int, info: MediaCodec.BufferInfo): Bitmap? {
        try {
            val ySize = width * height
            val uvSize = ySize / 4
            
            if (info.size < ySize + uvSize * 2) {
                Log.e(TAG, "Buffer too small: ${info.size} < ${ySize + uvSize * 2}")
                return null
            }
            
            // Read YUV data from buffer
            val yuvBytes = ByteArray(info.size)
            buffer.position(info.offset)
            buffer.get(yuvBytes, 0, info.size)
            
            // MediaCodec outputs YUV_420_888 which is I420 format
            // Convert to NV21 for YuvImage (swap U and V planes and interleave)
            val nv21 = ByteArray(ySize + uvSize * 2)
            
            // Copy Y plane
            System.arraycopy(yuvBytes, 0, nv21, 0, ySize)
            
            // Interleave U and V planes (I420 -> NV21)
            val uStart = ySize
            val vStart = ySize + uvSize
            var nv21Index = ySize
            for (i in 0 until uvSize) {
                nv21[nv21Index++] = yuvBytes[vStart + i] // V
                nv21[nv21Index++] = yuvBytes[uStart + i] // U
            }
            
            // Use YuvImage for fast conversion
            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                width,
                height,
                null
            )
            
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 95, out)
            val jpegBytes = out.toByteArray()
            
            return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert YUV to RGB: ${e.message}", e)
            return null
        }
    }
}
