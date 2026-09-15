package com.darki.dex.client.stream

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer

class H264Decoder(
    private val surface: Surface,
    private val width: Int,
    private val height: Int,
    private val onError: (Throwable) -> Unit = {}
) {
    private var codec: MediaCodec? = null

    fun start() {
        check(codec == null) { "Decoder already started" }
        val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        decoder.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) = Unit
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                codec.releaseOutputBuffer(index, info.size > 0)
            }
            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) = Unit
            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) = onError(e)
        })
        decoder.configure(format, surface, null, 0)
        decoder.start()
        codec = decoder
    }

    fun queueAccessUnit(data: ByteArray, offset: Int, size: Int, presentationTimeUs: Long, flags: Int = 0) {
        val decoder = codec ?: return
        val index = decoder.dequeueInputBuffer(10_000)
        if (index < 0) return
        decoder.getInputBuffer(index)?.let { buffer ->
            buffer.clear()
            buffer.put(data, offset, size)
            decoder.queueInputBuffer(index, 0, size, presentationTimeUs, flags)
        }
    }

    fun stop() {
        codec?.runCatching { stop() }
        codec?.release()
        codec = null
    }
}
