package com.darki.dex.host.net

import android.util.Log
import com.darki.dex.host.input.DarkiInput
import com.darki.dex.host.input.DarkiInputDispatcher
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

class DarkiHostServer(
    private val port: Int = DEFAULT_PORT,
    private val deviceName: String = "DARKI-Dex Host"
) : AutoCloseable {
    companion object {
        const val DEFAULT_PORT = 47291
        private const val TAG = "DARKI-Network"
        @Volatile var current: DarkiHostServer? = null
            private set
    }

    private val executor = Executors.newCachedThreadPool()
    private val sessions = CopyOnWriteArraySet<DarkiSession>()
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null
    @Volatile private var latestVideoConfig: ByteArray? = null

    fun start() {
        check(!running) { "Server already running" }
        running = true
        current = this
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
            } finally { serverSocket = null }
        }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.tcpNoDelay = true
            client.keepAlive = true
            Log.i(TAG, "Client connected: ${client.inetAddress.hostAddress}")
            DarkiSession(client).use { session ->
                session.sendHello(deviceName, "host")
                if (session.receive().type != DarkiProtocol.TYPE_HELLO_ACK) return
                sessions += session
                latestVideoConfig?.let { config -> runCatching { session.send(DarkiProtocol.TYPE_VIDEO_CONFIG, config) }.onFailure { sessions.remove(session) } }
                try {
                    while (running && !client.isClosed) {
                        val incoming = session.receive()
                        when (incoming.type) {
                            DarkiProtocol.TYPE_PING -> session.send(DarkiProtocol.TYPE_PONG)
                            DarkiProtocol.TYPE_MOUSE -> handleMouse(incoming.payload)
                            DarkiProtocol.TYPE_KEY -> handleKey(incoming.payload)
                            DarkiProtocol.TYPE_TEXT -> handleText(incoming.payload)
                            DarkiProtocol.TYPE_NAVIGATION -> handleNavigation(incoming.payload)
                        }
                    }
                } catch (_: IOException) {
                    // Client disconnected.
                } finally {
                    sessions -= session
                    Log.i(TAG, "Client disconnected: ${client.inetAddress.hostAddress}")
                }
            }
        }
    }

    private fun handleMouse(payload: ByteArray) {
        runCatching { DarkiInput.parseMouse(payload) }
            .onSuccess(DarkiInputDispatcher::dispatchMouse)
            .onFailure { Log.w(TAG, "Invalid mouse input", it) }
    }

    private fun handleKey(payload: ByteArray) {
        runCatching { DarkiInput.parseKey(payload) }
            .onSuccess(DarkiInputDispatcher::dispatchKey)
            .onFailure { Log.w(TAG, "Invalid key input", it) }
    }

    private fun handleText(payload: ByteArray) {
        runCatching { payload.toString(Charsets.UTF_8) }
            .onSuccess(DarkiInputDispatcher::dispatchText)
            .onFailure { Log.w(TAG, "Invalid text input", it) }
    }

    private fun handleNavigation(payload: ByteArray) {
        runCatching {
            require(payload.size == 4) { "Invalid navigation payload" }
            ByteBuffer.wrap(payload).int
        }.onSuccess(DarkiInputDispatcher::dispatchNavigation)
            .onFailure { Log.w(TAG, "Invalid navigation input", it) }
    }

    fun broadcastVideoConfig(width: Int, height: Int, csd0: ByteArray, csd1: ByteArray?) {
        val payload = ByteBuffer.allocate(16 + csd0.size + (csd1?.size ?: 0)).apply {
            putInt(width); putInt(height); putInt(csd0.size); put(csd0); putInt(csd1?.size ?: 0); csd1?.let(::put)
        }.array()
        latestVideoConfig = payload
        broadcast(DarkiProtocol.TYPE_VIDEO_CONFIG, payload)
    }

    fun broadcastVideoFrame(data: ByteArray, presentationTimeUs: Long, flags: Int) {
        val payload = ByteBuffer.allocate(12 + data.size).apply {
            putLong(presentationTimeUs); putInt(flags); put(data)
        }.array()
        broadcast(DarkiProtocol.TYPE_VIDEO_FRAME, payload)
    }

    private fun broadcast(type: Int, payload: ByteArray) {
        sessions.forEach { session -> runCatching { session.send(type, payload) }.onFailure { sessions.remove(session) } }
    }

    override fun close() {
        running = false
        sessions.forEach { runCatching { it.close() } }
        sessions.clear(); latestVideoConfig = null; serverSocket?.close(); executor.shutdownNow(); serverSocket = null
        if (current === this) current = null
    }
}
