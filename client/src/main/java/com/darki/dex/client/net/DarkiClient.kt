package com.darki.dex.client.net

import com.darki.dex.host.net.DarkiProtocol
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class DarkiClient {
    companion object {
        const val DEFAULT_PORT = 47291
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
                    val input = client.getInputStream().buffered()
                    val output = client.getOutputStream().buffered()
                    val packet = readPacket(input)
                    if (packet.type != DarkiProtocol.TYPE_HELLO) {
                        error("Expected HELLO, received ${packet.type}")
                    }

                    writePacket(output, DarkiProtocol.TYPE_HELLO_ACK, "device=DARKI-Desktop\nrole=client".toByteArray())
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

    private fun readPacket(input: java.io.InputStream): DarkiProtocol.Packet {
        val data = java.io.DataInputStream(input)
        val magic = data.readInt()
        require(magic == DarkiProtocol.MAGIC) { "Invalid DARKI magic" }
        require(data.readUnsignedShort() == DarkiProtocol.VERSION) { "Unsupported protocol version" }
        val type = data.readUnsignedByte()
        val length = data.readInt()
        require(length in 0..DarkiProtocol.MAX_CONTROL_PAYLOAD) { "Invalid payload length" }
        return DarkiProtocol.Packet(type, ByteArray(length).also(data::readFully))
    }

    private fun writePacket(output: java.io.OutputStream, type: Int, payload: ByteArray) {
        val data = java.io.DataOutputStream(output)
        data.writeInt(DarkiProtocol.MAGIC)
        data.writeShort(DarkiProtocol.VERSION)
        data.writeByte(type)
        data.writeInt(payload.size)
        data.write(payload)
        data.flush()
    }
}
