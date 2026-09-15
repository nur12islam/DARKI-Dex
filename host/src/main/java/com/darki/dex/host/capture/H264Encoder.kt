package com.darki.dex.host.capture

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

/** Hardware-first H.264 encoder whose input is a Surface backed by MediaProjection. */
class H264Encoder(
    private val width: Int,
    private val height: Int,
    private val frameRate: Int = 30,
    private val bitRate: Int = 6_000_000,
    private val iFrameIntervalSeconds: Int = 2,
    private val onFormat: (MediaFormat) -> Unit = {},
    private val onFrame: (ByteBuffer, MediaCodec.BufferInfo) -> Unit = { _, _ -> },
    private val onError: (Throwable) -> Unit = {}
) {
    companion object {
        private const val TAG = "DARKI-H264"
        private const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
    }

    private var codec: MediaCodec? = null
    private var surface: Surface? = null

    val inputSurface: Surface
        get() = checkNotNull(surface) { "Encoder has not been started" }

    fun start() {
        check(codec == null) { "Encoder already started" }

        val candidates = MediaCodecListHelper.findEncoders(MIME, width, height, frameRate)
        if (candidates.isEmpty()) {
            error("No H.264 surface encoder supports ${width}x${height}@${frameRate}fps")
        }

        var lastError: Throwable? = null
        for (selected in candidates) {
            try {
                Log.i(TAG, "Trying encoder=${selected.name}, hardware=${selected.isHardwareAccelerated}, vendor=${selected.isVendor}")
                startWith(selected)
                Log.i(TAG, "Encoder started: ${selected.name}")
                return
            } catch (t: Throwable) {
                lastError = t
                Log.e(TAG, "Encoder ${selected.name} failed to start", t)
                cleanupCodec()
            }
        }

        throw IllegalStateException("No H.264 encoder could start for ${width}x${height}@${frameRate}fps", lastError)
    }

    private fun startWith(selected: MediaCodecInfo) {
        val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameIntervalSeconds)
        }

        val c = MediaCodec.createByCodecName(selected.name)
        codec = c
        try {
            c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = c.createInputSurface()
            c.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) = Unit

                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    try {
                        val buffer = codec.getOutputBuffer(index)
                        if (buffer != null && info.size > 0) {
                            val duplicate = buffer.duplicate().apply {
                                position(info.offset)
                                limit(info.offset + info.size)
                            }
                            onFrame(duplicate.slice(), info)
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Output callback failed", t)
                        onError(t)
                    } finally {
                        runCatching { codec.releaseOutputBuffer(index, false) }
                    }
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    Log.i(TAG, "Output format: $format")
                    runCatching { onFormat(format) }.onFailure(onError)
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    Log.e(TAG, "Codec error", e)
                    onError(e)
                }
            })
            c.start()
        } catch (t: Throwable) {
            cleanupCodec()
            throw t
        }
    }

    private fun cleanupCodec() {
        codec?.runCatching { stop() }
        codec?.runCatching { release() }
        codec = null
        surface?.runCatching { release() }
        surface = null
    }

    fun stop() = cleanupCodec()
}
