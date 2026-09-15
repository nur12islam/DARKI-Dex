package com.darki.dex.client.net

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

class DarkiDiscovery {
    companion object {
        private const val PORT = 47290
        private const val PREFIX = "DARKI_DISCOVER_V1"
    }

    data class Host(
        val name: String,
        val address: String,
        val port: Int
    )

    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var running = false
    private var socket: DatagramSocket? = null

    fun start(callback: Callback) {
        stop()
        running = true
        executor.execute {
            try {
                DatagramSocket(PORT, InetAddress.getByName("0.0.0.0")).use { udp ->
                    socket = udp
                    udp.broadcast = true
                    udp.soTimeout = 1500
                    val buffer = ByteArray(2048)
                    callback.onStarted()
                    while (running) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            udp.receive(packet)
                            parse(packet.data, packet.length, packet.address.hostAddress ?: "")?.let(callback::onHost)
                        } catch (_: java.net.SocketTimeoutException) {
                            // Keep listening until stop() is called.
                        }
                    }
                }
            } catch (e: Exception) {
                if (running) callback.onError(e)
            } finally {
                socket = null
            }
        }
    }

    fun stop() {
        running = false
        socket?.close()
        socket = null
    }

    fun shutdown() {
        stop()
        executor.shutdownNow()
    }

    private fun parse(data: ByteArray, length: Int, address: String): Host? {
        val text = data.copyOf(length).toString(Charsets.UTF_8)
        val lines = text.lines()
        if (lines.firstOrNull() != PREFIX) return null
        val values = lines.drop(1).mapNotNull { line ->
            val index = line.indexOf('=')
            if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
        }.toMap()
        val name = values["name"] ?: return null
        val port = values["port"]?.toIntOrNull() ?: return null
        return Host(name, address, port)
    }

    interface Callback {
        fun onStarted()
        fun onHost(host: Host)
        fun onError(error: Throwable)
    }
}
