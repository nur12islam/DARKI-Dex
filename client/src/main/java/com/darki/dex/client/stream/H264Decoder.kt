package com.darki.dex.client.stream

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer

class H264Decoder(
    private val surface: Surface,
    private val width: Int,
    private val height: Int,
    private val csd0: ByteArray,
    private val csd1: ByteArray?,
    private val onError: (Throwable) -> Unit = {}
) {
    private var codec: MediaCodec? = null
    @Volatile private var running = false
    private var outputThread: Thread? = null

    fun start() {
        check(codec == null) { "Decoder already started" }
        val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setByteBuffer("csd-0", ByteBuffer.wrap(csd0))
            csd1?.let { setByteBuffer("csd-1", ByteBuffer.wrap(it)) }
        }
        decoder.configure(format, surface, null, 0)
        decoder.start()
        codec = decoder
        running = true
        outputThread = Thread({ drainOutput(decoder) }, "DARKI-H264-Decoder").also { it.start() }
    }

    fun queueAccessUnit(data: ByteArray, presentationTimeUs: Long, flags: Int = 0) {
        val decoder = codec ?: return
        if (!running) return
        val index = decoder.dequeueInputBuffer(10_000)
        if (index < 0) return
        decoder.getInputBuffer(index)?.let { buffer ->
            buffer.clear()
            buffer.put(data)
            decoder.queueInputBuffer(index, 0, data.size, presentationTimeUs, flags)
        }
    }

    private fun drainOutput(decoder: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        while (running) {
            try {
                val index = decoder.dequeueOutputBuffer(info, 10_000)
                when {
                    index >= 0 -> decoder.releaseOutputBuffer(index, true)
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                }
            } catch (t: Throwable) {
                if (running) onError(t)
                break
            }
        }
    }

    fun stop() {
        running = false
        outputThread?.interrupt()
        outputThread = null
        codec?.runCatching { stop() }
        codec?.release()
        codec = null
    }
}
