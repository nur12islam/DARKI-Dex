package com.darki.dex.client.stream

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.util.concurrent.Executors

/** Low-latency H.264 decoder for DARKI video access units. */
class H264Decoder(
    private val surface: Surface,
    private val width: Int,
    private val height: Int,
    private val onError: (Throwable) -> Unit = {}
) {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var running = false
    private var codec: MediaCodec? = null

    fun start() {
        check(codec == null) { "Decoder already started" }
        val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        decoder.configure(format, surface, null, 0)
        decoder.start()
        codec = decoder
        running = true
        executor.execute { drainOutput() }
    }

    fun queueAccessUnit(data: ByteArray, offset: Int, size: Int, presentationTimeUs: Long, flags: Int = 0) {
        val decoder = codec ?: return
        runCatching {
            val index = decoder.dequeueInputBuffer(10_000)
            if (index < 0) return
            val buffer = decoder.getInputBuffer(index) ?: return
            buffer.clear()
            buffer.put(data, offset, size)
            decoder.queueInputBuffer(index, 0, size, presentationTimeUs, flags)
        }.onFailure(onError)
    }

    private fun drainOutput() {
        val info = MediaCodec.BufferInfo()
        while (running) {
            val decoder = codec ?: break
            runCatching {
                val index = decoder.dequeueOutputBuffer(info, 10_000)
                if (index >= 0) decoder.releaseOutputBuffer(index, true)
            }.onFailure {
                if (running) onError(it)
                return
            }
        }
    }

    fun stop() {
        running = false
        codec?.runCatching { stop() }
        codec?.release()
        codec = null
        executor.shutdownNow()
    }
}
