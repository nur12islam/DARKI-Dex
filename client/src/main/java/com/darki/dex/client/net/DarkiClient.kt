package com.darki.dex.client.net

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class DarkiClient {
    companion object {
        const val DEFAULT_PORT = 47291
        private const val MAGIC = 0x4441524B
        private const val VERSION = 1
        private const val TYPE_HELLO = 1
        private const val TYPE_HELLO_ACK = 2
        private const val MAX_CONTROL_PAYLOAD = 64 * 1024
    }

    private val executor = Executors.newSingleThreadExecutor()

    fun connect(host: String, port: Int = DEFAULT_PORT, callback: Callback) {
        executor.execute {
            runCatching {
                Socket().apply {
                    tcpNoDelay = true
                    connect(InetSocketAddress(host, port), 3000)
                }
            }.onSuccess { socket ->
                socket.use { client ->
                    val input = DataInputStream(client.getInputStream().buffered())
                    val output = DataOutputStream(client.getOutputStream().buffered())
                    val packet = readPacket(input)
                    if (packet.first != TYPE_HELLO) error("Expected HELLO, received ${packet.first}")
                    writePacket(output, TYPE_HELLO_ACK, "device=DARKI-Desktop\nrole=client".toByteArray())
                    callback.onConnected(client.inetAddress.hostAddress ?: host)
                }
            }.onFailure { callback.onError(it) }
        }
    }

    fun shutdown() = executor.shutdownNow()

    interface Callback {
        fun onConnected(host: String)
        fun onError(error: Throwable)
    }

    private fun readPacket(input: DataInputStream): Pair<Int, ByteArray> {
        require(input.readInt() == MAGIC) { "Invalid DARKI magic" }
        require(input.readUnsignedShort() == VERSION) { "Unsupported protocol version" }
        val type = input.readUnsignedByte()
        val length = input.readInt()
        require(length in 0..MAX_CONTROL_PAYLOAD) { "Invalid payload length" }
        return type to ByteArray(length).also(input::readFully)
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
}
