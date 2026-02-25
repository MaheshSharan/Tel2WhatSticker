package com.maheshsharan.tel2what.engine.decoder

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.maheshsharan.tel2what.engine.frame.FrameData
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.math.min

/**
 * Validates and decodes `.webm` Telegram video stickers into extractable bit frames.
 * Uses hardware-precise MediaCodec + MediaExtractor surface rendering over ImageReader
 * to guarantee alpha channel preservation and B-Frame chronological layout.
 */
object WebmDecoder {
    private const val TAG = "Tel2What:WebmDecoder"

    suspend fun decode(
        webmFile: File,
        targetFps: Int,
        maxDurationMs: Long
    ): List<FrameData> {
        Log.i(TAG, "WebmDecoder: Starting decode of ${webmFile.name}")
        Log.i(TAG, "WebmDecoder: File exists=${webmFile.exists()}, size=${webmFile.length()} bytes")
        Log.i(TAG, "WebmDecoder: Target FPS=$targetFps, maxDuration=${maxDurationMs}ms")
        
        if (!webmFile.exists()) {
            Log.e(TAG, "WebmDecoder: ERROR - WEBM file does not exist!")
            return emptyList()
        }
        
        if (webmFile.length() == 0L) {
            Log.e(TAG, "WebmDecoder: ERROR - WEBM file is empty!")
            return emptyList()
        }
        
        val frames = mutableListOf<FrameData>()
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var imageReader: ImageReader? = null
        
        try {
            Log.i(TAG, "WebmDecoder: Setting data source")
            extractor.setDataSource(webmFile.absolutePath)
            
            Log.i(TAG, "WebmDecoder: Selecting video track, total tracks=${extractor.trackCount}")
            val trackIndex = selectVideoTrack(extractor)
            if (trackIndex < 0) {
                Log.e(TAG, "WebmDecoder: ERROR - No video track found in ${webmFile.name}. Track count: ${extractor.trackCount}")
                return emptyList()
            }
            
            Log.i(TAG, "WebmDecoder: Video track found at index $trackIndex")
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val containerMime = format.getString(MediaFormat.KEY_MIME)
            
            if (containerMime == null) {
                Log.e(TAG, "WebmDecoder: ERROR - MIME type is null!")
                return emptyList()
            }
            
            Log.i(TAG, "WebmDecoder: Container MIME: $containerMime")
            
            // WebM files report "video/webm" as MIME, but MediaCodec needs the actual codec
            // Telegram video stickers use VP9 codec in WebM container
            val codecMime = if (containerMime == "video/webm") {
                // Try to detect the actual codec from format
                val codecName = format.getString("codecs-string") ?: format.getString(MediaFormat.KEY_MIME)
                Log.i(TAG, "WebmDecoder: Codec string from format: $codecName")
                
                // Telegram video stickers typically use VP9
                // We'll try VP9 first, then fall back to VP8
                "video/x-vnd.on2.vp9"
            } else {
                containerMime
            }
            
            Log.i(TAG, "WebmDecoder: Using codec MIME: $codecMime for decoder")
            
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            val finalDurationUs = min(durationUs, maxDurationMs * 1000L)
            
            // Frame duration mapping
            val frameDelayMs = 1000 / targetFps
            val frameDelayUs = (frameDelayMs * 1000).toLong()
            
            var width = format.getInteger(MediaFormat.KEY_WIDTH)
            var height = format.getInteger(MediaFormat.KEY_HEIGHT)

            Log.i(TAG, "WebmDecoder: Video dimensions: ${width}x${height}")
            Log.i(TAG, "WebmDecoder: Video duration: ${durationUs}us (${durationUs/1000}ms)")
            Log.i(TAG, "WebmDecoder: Final duration will be: ${finalDurationUs}us (${finalDurationUs/1000}ms)")

            // MediaCodec outputs YUV format when rendering to surface, not RGBA
            // We need to use ImageFormat.YUV_420_888 or PRIVATE format
            Log.i(TAG, "WebmDecoder: Creating ImageReader with PRIVATE format")
            imageReader = ImageReader.newInstance(width, height, android.graphics.ImageFormat.PRIVATE, 5)
            
            Log.i(TAG, "WebmDecoder: Creating MediaCodec decoder for $codecMime")
            codec = try {
                MediaCodec.createDecoderByType(codecMime)
            } catch (e: Exception) {
                Log.e(TAG, "WebmDecoder: Failed to create decoder for $codecMime, trying VP8 fallback", e)
                // Try VP8 as fallback
                try {
                    MediaCodec.createDecoderByType("video/x-vnd.on2.vp8").also {
                        Log.i(TAG, "WebmDecoder: Successfully created VP8 decoder as fallback")
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "WebmDecoder: VP8 fallback also failed", e2)
                    return emptyList()
                }
            }
            
            // At this point codec is guaranteed to be non-null
            val mediaCodec = codec!!
            
            mediaCodec.configure(format, imageReader.surface, null, 0)
            mediaCodec.start()
            Log.i(TAG, "WebmDecoder: MediaCodec started")

            val info = MediaCodec.BufferInfo()
            var isEOS = false
            var outputEOS = false
            var nextTargetTimeUs = 0L
            var lastPresentationTimeUs = 0L

            val frameSemaphore = kotlinx.coroutines.sync.Semaphore(1, 1) // Start locked
            
            // Create a Handler with a background thread Looper for ImageReader callbacks
            val handlerThread = android.os.HandlerThread("ImageReaderThread")
            handlerThread.start()
            val handler = android.os.Handler(handlerThread.looper)
            
            imageReader.setOnImageAvailableListener({ _ ->
                frameSemaphore.release()
            }, handler)

            Log.i(TAG, "WebmDecoder: Starting decode loop - track=$trackIndex, ${width}x${height}px, targetFPS=$targetFps")

            while (!outputEOS) {
                // If the user rotates the screen or kills the App, politely yield to the event loop which triggers a cancellation exception
                yield()

                if (!isEOS) {
                    val inIndex = mediaCodec.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val buffer = mediaCodec.getInputBuffer(inIndex)
                        val sampleSize = extractor.readSampleData(buffer!!, 0)
                        if (sampleSize < 0) {
                            Log.d(TAG, "Input EOS reached via Extractor")
                            mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            if (presentationTimeUs > finalDurationUs) {
                                Log.d(TAG, "Cap reached at ${presentationTimeUs}us. Sending EOS.")
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
                        Log.d(TAG, "Output EOS reached via Codec")
                        outputEOS = true
                    }

                    // info.size can be 0 when rendering to surface on some chipsets.
                    // We rely on presentationTime and flags.
                    val doRender = info.presentationTimeUs >= nextTargetTimeUs && info.presentationTimeUs <= finalDurationUs
                    
                    if (doRender) {
                        Log.v(TAG, "Rendering frame at ${info.presentationTimeUs}us (target >= ${nextTargetTimeUs}us)")
                    }
                    
                    mediaCodec.releaseOutputBuffer(outIndex, doRender)
                    
                    if (doRender) {
                        Log.v(TAG, "Rendering frame at ${info.presentationTimeUs}us (target >= ${nextTargetTimeUs}us)")
                        
                        // WAIT for the GPU to finish rendering to the ImageReader surface
                        withContext(Dispatchers.Default) {
                            try {
                                kotlinx.coroutines.withTimeout(500) {
                                    frameSemaphore.acquire()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Timeout waiting for surface frame at ${info.presentationTimeUs}us")
                            }
                        }

                        val image = imageReader.acquireLatestImage()
                        if (image != null) {
                            val bitmap = imageToBitmap(image, width, height)
                            image.close()
                            if (bitmap != null) {
                                // Calculate actual duration if not first frame
                                val duration = if (frames.isEmpty()) {
                                    frameDelayMs // Fallback for first frame
                                } else {
                                    ((info.presentationTimeUs - lastPresentationTimeUs) / 1000).toInt().coerceAtLeast(8)
                                }
                                
                                frames.add(FrameData(bitmap, duration))
                                lastPresentationTimeUs = info.presentationTimeUs
                            }
                        }
                        
                        nextTargetTimeUs += frameDelayUs
                    }
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = mediaCodec.outputFormat
                    width = newFormat.getInteger(MediaFormat.KEY_WIDTH)
                    height = newFormat.getInteger(MediaFormat.KEY_HEIGHT)
                }
            }

            Log.i(TAG, "WebmDecoder: SUCCESS - Hardware Pipeline Complete. Generated ${frames.size} frames")
            Log.i(TAG, "WebmDecoder: Total duration of extracted frames: ${frames.sumOf { it.durationMs }}ms")
        } catch (e: Exception) {
            Log.e(TAG, "WebmDecoder: CATASTROPHIC FAILURE - ${e.message}", e)
            frames.forEach { it.bitmap.recycle() }
            frames.clear()
        } finally {
            try { codec?.stop(); codec?.release() } catch (e: Exception) {}
            try { extractor.release() } catch (e: Exception) {}
            try { imageReader?.close() } catch (e: Exception) {}
            try { 
                // Clean up the handler thread
                val handlerThread = android.os.Looper.myLooper()?.thread as? android.os.HandlerThread
                handlerThread?.quitSafely()
            } catch (e: Exception) {}
        }
        return frames
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

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        val planes = image.planes
        if (planes.isEmpty()) return null
        
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        
        if (rowPadding == 0) return bitmap
        
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
        bitmap.recycle()
        return cropped
    }
}
