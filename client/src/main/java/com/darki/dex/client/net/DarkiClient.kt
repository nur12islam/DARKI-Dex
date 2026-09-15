package com.darki.dex.client.net

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class DarkiClient {
    companion object {
        const val DEFAULT_PORT = 47291
        private const val MAGIC = 0x4441524B
        private const val VERSION = 1
        private const val TYPE_HELLO = 1
        private const val TYPE_HELLO_ACK = 2
        const val TYPE_PING = 3
        const val TYPE_PONG = 4
        const val TYPE_VIDEO_CONFIG = 10
        const val TYPE_VIDEO_FRAME = 11
        private const val MAX_CONTROL_PAYLOAD = 64 * 1024
        private const val MAX_VIDEO_PAYLOAD = 4 * 1024 * 1024
    }

    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var socket: Socket? = null

    fun connect(host: String, port: Int = DEFAULT_PORT, callback: Callback) {
        executor.execute {
            runCatching {
                Socket().apply {
                    tcpNoDelay = true
                    keepAlive = true
                    connect(InetSocketAddress(host, port), 3000)
                }
            }.onSuccess { client ->
                socket = client
                runCatching {
                    val input = DataInputStream(client.getInputStream().buffered())
                    val output = DataOutputStream(client.getOutputStream().buffered())
                    val hello = readPacket(input)
                    require(hello.type == TYPE_HELLO) { "Expected HELLO, received ${hello.type}" }
                    writePacket(output, TYPE_HELLO_ACK, "device=DARKI-Desktop\nrole=client".toByteArray())
                    callback.onConnected(client.inetAddress.hostAddress ?: host)
                    while (!client.isClosed) {
                        when (val packet = readPacket(input)) {
                            else -> callback.onPacket(packet.type, packet.payload)
                        }
                    }
                }.onFailure {
                    callback.onError(it)
                    close()
                }
            }.onFailure { callback.onError(it) }
        }
    }

    fun close() {
        socket?.runCatching { close() }
        socket = null
    }

    fun shutdown() {
        close()
        executor.shutdownNow()
    }

    interface Callback {
        fun onConnected(host: String)
        fun onPacket(type: Int, payload: ByteArray)
        fun onError(error: Throwable)
    }

    private data class Packet(val type: Int, val payload: ByteArray)

    private fun readPacket(input: DataInputStream): Packet {
        require(input.readInt() == MAGIC) { "Invalid DARKI magic" }
        require(input.readUnsignedShort() == VERSION) { "Unsupported protocol version" }
        val type = input.readUnsignedByte()
        val length = input.readInt()
        val limit = if (type == TYPE_VIDEO_FRAME) MAX_VIDEO_PAYLOAD else MAX_CONTROL_PAYLOAD
        require(length in 0..limit) { "Invalid payload length: $length" }
        return Packet(type, ByteArray(length).also(input::readFully))
    }

    private fun writePacket(output: DataOutputStream, type: Int, payload: ByteArray) {
        require(payload.size <= MAX_CONTROL_PAYLOAD)
        output.writeInt(MAGIC)
        output.writeShort(VERSION)
        output.writeByte(type)
        output.writeInt(payload.size)
        output.write(payload)
        output.flush()
    }

    fun parseVideoConfig(payload: ByteArray): VideoConfig {
        val input = ByteBuffer.wrap(payload)
        val width = input.int
        val height = input.int
        val csd0Size = input.int
        require(csd0Size >= 0 && csd0Size <= input.remaining())
        val csd0 = ByteArray(csd0Size).also(input::get)
        val csd1Size = input.int
        require(csd1Size >= 0 && csd1Size <= input.remaining())
        val csd1 = ByteArray(csd1Size).also(input::get)
        return VideoConfig(width, height, csd0, csd1.takeIf { it.isNotEmpty() })
    }

    fun parseVideoFrame(payload: ByteArray): VideoFrame {
        val input = ByteBuffer.wrap(payload)
        val pts = input.long
        val flags = input.int
        val data = ByteArray(input.remaining()).also(input::get)
        return VideoFrame(data, pts, flags)
    }

    data class VideoConfig(val width: Int, val height: Int, val csd0: ByteArray, val csd1: ByteArray?)
    data class VideoFrame(val data: ByteArray, val presentationTimeUs: Long, val flags: Int)
}
