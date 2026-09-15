package com.darki.dex.client.net

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

class DarkiDiscoveryClient {
    companion object {
        private const val PORT = 47290
        private const val PREFIX = "DARKI_DISCOVER_V1"
        private const val BROADCAST_ADDRESS = "255.255.255.255"
    }

    data class Host(val name: String, val address: InetAddress, val port: Int)

    private val executor = Executors.newSingleThreadExecutor()

    fun discover(timeoutMs: Int = 2500, callback: Callback) {
        executor.execute {
            val hosts = linkedMapOf<String, Host>()
            runCatching {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = 500
                    val request = "$PREFIX\nrequest=discover".toByteArray(Charsets.UTF_8)
                    socket.send(
                        DatagramPacket(
                            request,
                            request.size,
                            InetAddress.getByName(BROADCAST_ADDRESS),
                            PORT
                        )
                    )

                    val end = System.currentTimeMillis() + timeoutMs
                    while (System.currentTimeMillis() < end) {
                        try {
                            val buffer = ByteArray(2048)
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            parse(packet)?.let { hosts["${it.address.hostAddress}:${it.port}"] = it }
                        } catch (_: java.net.SocketTimeoutException) {
                            // Continue until the discovery window expires.
                        }
                    }
                }
            }.onFailure { callback.onError(it) }
            callback.onHosts(hosts.values.toList())
        }
    }

    fun shutdown() = executor.shutdownNow()

    private fun parse(packet: DatagramPacket): Host? {
        val text = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
        val lines = text.lines()
        if (lines.firstOrNull() != PREFIX) return null
        val values = lines.drop(1).mapNotNull { line ->
            val i = line.indexOf('=')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }.toMap()
        val name = values["name"] ?: return null
        val port = values["port"]?.toIntOrNull() ?: return null
        return Host(name, packet.address, port)
    }

    interface Callback {
        fun onHosts(hosts: List<Host>)
        fun onError(error: Throwable)
    }
}
