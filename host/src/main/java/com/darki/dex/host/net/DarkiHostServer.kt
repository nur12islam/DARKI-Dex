package com.darki.dex.host.net

import android.util.Log
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class DarkiHostServer(
    private val port: Int = DEFAULT_PORT,
    private val deviceName: String = "DARKI-Dex Host"
) : AutoCloseable {
    companion object {
        const val DEFAULT_PORT = 47291
        private const val TAG = "DARKI-Network"
    }

    private val executor = Executors.newCachedThreadPool()
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        check(!running) { "Server already running" }
        running = true
        executor.execute {
            try {
                ServerSocket(port, 16, InetAddress.getByName("0.0.0.0")).use { server ->
                    serverSocket = server
                    Log.i(TAG, "DARKI host listening on TCP $port")
                    while (running) {
                        val socket = server.accept()
                        executor.execute { handle(socket) }
                    }
                }
            } catch (e: Exception) {
                if (running) Log.e(TAG, "Host server stopped unexpectedly", e)
            } finally {
                serverSocket = null
            }
        }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.tcpNoDelay = true
            Log.i(TAG, "Client connected: ${client.inetAddress.hostAddress}")

            DarkiSession(client).use { session ->
                session.sendHello(deviceName, "host")
                val packet = session.receive()
                when (packet.type) {
                    DarkiProtocol.TYPE_HELLO_ACK -> Log.i(TAG, "Client handshake accepted")
                    DarkiProtocol.TYPE_PING -> {
                        Log.i(TAG, "Client ping received")
                        // A dedicated session implementation will own pong replies.
                    }
                    else -> Log.w(TAG, "Unexpected first packet type=${packet.type}")
                }
            }
        }
    }

    override fun close() {
        running = false
        serverSocket?.close()
        executor.shutdownNow()
        serverSocket = null
    }
}
