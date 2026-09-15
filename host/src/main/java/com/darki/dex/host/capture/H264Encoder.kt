package com.darki.dex.host.capture

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer

/**
 * Hardware-first H.264 encoder used by the MediaProjection proof.
 *
 * The encoder consumes frames through an input Surface. Encoded access units are
 * drained on a dedicated thread so the capture service never blocks on codec I/O.
 */
class H264Encoder(
    private val width: Int,
    private val height: Int,
    private val frameRate: Int = 30,
    private val bitRate: Int = 6_000_000,
    private val iFrameIntervalSeconds: Int = 2,
    private val onFormat: (MediaFormat) -> Unit = {},
    private val onFrame: (ByteBuffer, MediaCodec.BufferInfo) -> Unit = { _, _ -> }
) {
    companion object {
        private const val TAG = "DARKI-H264"
        private const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
    }

    private var codec: MediaCodec? = null
    private var drainThread: Thread? = null
    @Volatile private var running = false

    val inputSurface
        get() = codec?.createInputSurface()

    fun start() {
        check(codec == null) { "Encoder already started" }

        val selected = MediaCodecListHelper.findEncoder(MIME)
            ?: error("No H.264 encoder is available on this device")

        Log.i(TAG, "Using encoder: ${selected.name}")
        Log.i(TAG, "Hardware accelerated=${selected.isHardwareAccelerated}, vendor=${selected.isVendor}")

        val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameIntervalSeconds)
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                setInteger(MediaFormat.KEY_PRIORITY, 0)
            }
        }

        codec = MediaCodec.createByCodecName(selected.name).also {
            it.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) = Unit

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    index: Int,
                    info: MediaCodec.BufferInfo
                ) {
                    val buffer = codec.getOutputBuffer(index)
                    if (buffer != null && info.size > 0) {
                        val duplicate = buffer.duplicate().apply {
                            position(info.offset)
                            limit(info.offset + info.size)
                        }
                        onFrame(duplicate.slice(), info)
                    }
                    codec.releaseOutputBuffer(index, false)
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    Log.i(TAG, "Output format: $format")
                    onFormat(format)
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    Log.e(TAG, "Codec error", e)
                }
            })
            it.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            it.start()
        }

        running = true
        drainThread = Thread({
            // Callback mode does the actual draining. This thread simply keeps a
            // named lifecycle point for future encoder metrics/backpressure work.
            while (running) Thread.sleep(1_000)
        }, "darki-h264-lifecycle").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        drainThread?.interrupt()
        drainThread = null
        codec?.runCatching { stop() }
        codec?.release()
        codec = null
    }
}
