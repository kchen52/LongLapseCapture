package dev.ktown.longlapsecapture.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import dev.ktown.longlapsecapture.data.db.CaptureEntryEntity
import dev.ktown.longlapsecapture.data.repository.LonglapseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class TimelapseExporter(
    private val repository: LonglapseRepository
) {
    suspend fun exportTimelapse(
        projectId: String,
        entries: List<CaptureEntryEntity>,
        fps: Int
    ): Result<String> {
        if (entries.isEmpty()) return Result.failure(IllegalStateException("No entries to export"))

        return withContext(Dispatchers.Default) {
            val outputPath = repository.exportFilePath(projectId)
            val firstBitmap = BitmapFactory.decodeFile(entries.first().filePath)
                ?: return@withContext Result.failure(IllegalStateException("Failed to load first image"))

            val targetWidth = 1280
            val scaledHeight = (firstBitmap.height * targetWidth.toFloat() / firstBitmap.width).toInt()
            val targetHeight = if (scaledHeight % 2 == 0) scaledHeight else scaledHeight + 1

            val format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, targetWidth * targetHeight * 4)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
            val bufferInfo = MediaCodec.BufferInfo()
            var muxer: MediaMuxer? = null
            val muxerState = MuxerState()
            var ptsUs = 0L

            try {
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder.start()
                muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                for (entry in entries) {
                    val bitmap = BitmapFactory.decodeFile(entry.filePath) ?: continue
                    val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                    val yuv = ByteArray(targetWidth * targetHeight * 3 / 2)
                    argbToI420(scaled, yuv)
                    queueInput(encoder, yuv, ptsUs)
                    ptsUs += 1_000_000L / fps
                    drainEncoder(encoder, muxer, bufferInfo, false, muxerState)
                }

                queueInput(encoder, ByteArray(0), ptsUs, endOfStream = true)
                drainEncoder(encoder, muxer, bufferInfo, true, muxerState)

                Result.success(outputPath)
            } catch (ex: Exception) {
                Result.failure(ex)
            } finally {
                try {
                    encoder.stop()
                } catch (_: Exception) {
                }
                try {
                    encoder.release()
                } catch (_: Exception) {
                }
                if (muxerState.started) {
                    try {
                        muxer?.stop()
                    } catch (_: Exception) {
                    }
                }
                try {
                    muxer?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun queueInput(
        encoder: MediaCodec,
        data: ByteArray,
        ptsUs: Long,
        endOfStream: Boolean = false
    ) {
        val index = encoder.dequeueInputBuffer(INPUT_TIMEOUT_US)
        if (index >= 0) {
            val buffer = encoder.getInputBuffer(index)
            buffer?.clear()
            buffer?.put(data)
            val flags = if (endOfStream) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
            encoder.queueInputBuffer(index, 0, data.size, ptsUs, flags)
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer?,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        muxerState: MuxerState
    ) {
        var outputIndex = encoder.dequeueOutputBuffer(bufferInfo, OUTPUT_TIMEOUT_US)
        while (outputIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
            when (outputIndex) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    muxerState.trackIndex = muxer?.addTrack(encoder.outputFormat) ?: -1
                    muxer?.start()
                    muxerState.started = true
                }
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                else -> {
                    val outputBuffer: ByteBuffer = encoder.getOutputBuffer(outputIndex) ?: run {
                        encoder.releaseOutputBuffer(outputIndex, false)
                        return
                    }
                    if (bufferInfo.size > 0 && muxer != null && muxerState.trackIndex >= 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(muxerState.trackIndex, outputBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        return
                    }
                }
            }
            outputIndex = encoder.dequeueOutputBuffer(bufferInfo, OUTPUT_TIMEOUT_US)
        }
        if (endOfStream) {
            while (outputIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                outputIndex = encoder.dequeueOutputBuffer(bufferInfo, OUTPUT_TIMEOUT_US)
            }
        }
    }

    private fun argbToI420(bitmap: Bitmap, output: ByteArray) {
        val width = bitmap.width
        val height = bitmap.height
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        var yIndex = 0
        var uIndex = width * height
        var vIndex = uIndex + (width * height / 4)
        for (j in 0 until height) {
            for (i in 0 until width) {
                val color = argb[j * width + i]
                val r = (color shr 16) and 0xff
                val g = (color shr 8) and 0xff
                val b = color and 0xff
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                output[yIndex++] = y.coerceIn(0, 255).toByte()
                if (j % 2 == 0 && i % 2 == 0) {
                    output[uIndex++] = u.coerceIn(0, 255).toByte()
                    output[vIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
    }

    companion object {
        private const val VIDEO_MIME_TYPE = "video/avc"
        private const val INPUT_TIMEOUT_US = 10_000L
        private const val OUTPUT_TIMEOUT_US = 10_000L
    }

    private data class MuxerState(
        var started: Boolean = false,
        var trackIndex: Int = -1
    )
}
