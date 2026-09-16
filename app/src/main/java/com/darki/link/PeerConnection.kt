package com.darki.link

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Bidirectional LAN transport for the first DARKI Link connection proof.
 * Security/authentication is intentionally a separate layer and must be added
 * before this transport is used for privileged control commands.
 */
class PeerConnection(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var serverJob: Job? = null
    private var readerJob: Job? = null

    fun listen(
        port: Int = PairingStore.DEFAULT_PORT,
        onConnected: (String) -> Unit,
        onMessage: (JSONObject) -> Unit,
        onDisconnected: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        serverJob?.cancel()
        serverJob = scope.launch {
            try {
                ServerSocket(port).use { server ->
                    val client = server.accept()
                    attach(client, onConnected, onMessage, onDisconnected)
                }
            } catch (t: Throwable) {
                if (t !is java.util.concurrent.CancellationException) onError(t)
            }
        }
    }

    fun connect(
        host: String,
        port: Int = PairingStore.DEFAULT_PORT,
        onConnected: (String) -> Unit,
        onMessage: (JSONObject) -> Unit,
        onDisconnected: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        scope.launch {
            try {
                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(host.trim(), port), 5000)
                attach(newSocket, onConnected, onMessage, onDisconnected)
            } catch (t: Throwable) {
                if (t !is java.util.concurrent.CancellationException) onError(t)
            }
        }
    }

    @Synchronized
    fun send(message: JSONObject): Boolean {
        val out = writer ?: return false
        out.println(message.toString())
        return !out.checkError()
    }

    fun close() {
        readerJob?.cancel()
        serverJob?.cancel()
        try { socket?.close() } catch (_: Throwable) { }
        socket = null
        writer = null
        scope.cancel()
    }

    private fun attach(
        newSocket: Socket,
        onConnected: (String) -> Unit,
        onMessage: (JSONObject) -> Unit,
        onDisconnected: () -> Unit
    ) {
        socket?.close()
        socket = newSocket
        writer = PrintWriter(newSocket.getOutputStream(), true)
        val reader = BufferedReader(InputStreamReader(newSocket.getInputStream(), Charsets.UTF_8))
        onConnected(newSocket.inetAddress.hostAddress ?: "peer")

        readerJob?.cancel()
        readerJob = scope.launch {
            try {
                while (true) {
                    val line = reader.readLine() ?: break
                    runCatching { JSONObject(line) }.onSuccess(onMessage)
                }
            } finally {
                onDisconnected()
            }
        }
    }
}
