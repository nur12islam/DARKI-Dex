package com.darki.dex.host.net

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

object DarkiDiscovery {
    const val PORT = 47290
    const val PREFIX = "DARKI_DISCOVER_V1"
    private const val TAG = "DARKI-Discovery"

    fun createBeacon(deviceName: String, tcpPort: Int = DarkiHostServer.DEFAULT_PORT): ByteArray =
        "$PREFIX\nname=$deviceName\nport=$tcpPort".toByteArray(Charsets.UTF_8)

    class HostBeacon(
        private val deviceName: String = "DARKI-Dex Host",
        private val tcpPort: Int = DarkiHostServer.DEFAULT_PORT
    ) : AutoCloseable {
        private val executor = Executors.newSingleThreadExecutor()
        @Volatile private var running = false
        private var socket: DatagramSocket? = null

        fun start() {
            if (running) return
            running = true
            executor.execute {
                try {
                    DatagramSocket(PORT, InetAddress.getByName("0.0.0.0")).use { udp ->
                        socket = udp
                        udp.broadcast = true
                        val payload = createBeacon(deviceName, tcpPort)
                        val packet = DatagramPacket(
                            payload,
                            payload.size,
                            InetAddress.getByName("255.255.255.255"),
                            PORT
                        )
                        Log.i(TAG, "Discovery beacon started on UDP $PORT")
                        while (running) {
                            udp.send(packet)
                            Thread.sleep(2000)
                        }
                    }
                } catch (e: Exception) {
                    if (running) Log.e(TAG, "Discovery beacon stopped unexpectedly", e)
                } finally {
                    socket = null
                }
            }
        }

        override fun close() {
            running = false
            socket?.close()
            executor.shutdownNow()
            socket = null
        }
    }
}
