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
import java.net.ServerSocket
import java.net.Socket

/** Minimal LAN transport used to prove the peer protocol before adding discovery/P2P. */
class PeerConnection(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var serverJob: Job? = null

    fun listen(port: Int, onMessage: (JSONObject) -> Unit, onError: (Throwable) -> Unit) {
        serverJob?.cancel()
        serverJob = scope.launch {
            try {
                ServerSocket(port).use { server ->
                    val client = server.accept()
                    attach(client, onMessage)
                }
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun connect(host: String, port: Int, onMessage: (JSONObject) -> Unit, onError: (Throwable) -> Unit) {
        scope.launch {
            try {
                attach(Socket(host, port), onMessage)
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    @Synchronized
    fun send(message: JSONObject) {
        writer?.println(message.toString())
    }

    fun close() {
        try { socket?.close() } catch (_: Throwable) { }
        serverJob?.cancel()
        scope.cancel()
    }

    private fun attach(newSocket: Socket, onMessage: (JSONObject) -> Unit) {
        socket?.close()
        socket = newSocket
        writer = PrintWriter(newSocket.getOutputStream(), true)
        val reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))
        scope.launch {
            while (true) {
                val line = reader.readLine() ?: break
                runCatching { JSONObject(line) }.onSuccess(onMessage)
            }
        }
    }
}
