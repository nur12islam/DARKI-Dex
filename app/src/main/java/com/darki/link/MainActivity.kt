package com.darki.link

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var address: EditText
    private val connection = PeerConnection()
    private val pairing by lazy { PairingStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceId = DeviceIdentity.get(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
        }

        root.addView(TextView(this).apply {
            text = "DARKI Link"
            textSize = 30f
        })
        root.addView(TextView(this).apply {
            text = "Private device ecosystem • Protocol v${Protocol.VERSION}\nDevice: ${deviceId.take(8)}…"
            textSize = 16f
            setPadding(0, 12, 0, 24)
        })

        address = EditText(this).apply {
            hint = "Peer IP address"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            setText(pairing.host().orEmpty())
        }
        root.addView(address)

        root.addView(Button(this).apply {
            text = "Listen for peer"
            setOnClickListener { startListening() }
        })

        root.addView(Button(this).apply {
            text = "Connect to peer"
            setOnClickListener { connectToPeer(address.text.toString()) }
        })

        val ping = Button(this).apply {
            text = "Send ping"
            isEnabled = false
            setOnClickListener {
                val sent = connection.send(Protocol.message("ping"))
                appendStatus(if (sent) "Ping sent" else "No active connection")
            }
        }
        root.addView(ping)

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 24, 0, 0)
        }
        root.addView(status)
        setContentView(root)

        appendStatus("Ready — choose Listen or enter the peer IP.")
        if (pairing.isPaired()) appendStatus("Saved peer: ${pairing.peerId()?.take(8)}…")
    }

    private fun startListening() {
        appendStatus("Listening on TCP ${PairingStore.DEFAULT_PORT}…")
        connection.listen(
            onConnected = { host -> runOnUiThread {
                appendStatus("Connected from $host")
                sendHello()
            } },
            onMessage = ::handleMessage,
            onDisconnected = { runOnUiThread { appendStatus("Peer disconnected") } },
            onError = { error -> runOnUiThread { appendStatus("Listen error: ${error.message}") } }
        )
    }

    private fun connectToPeer(host: String) {
        if (host.isBlank()) {
            appendStatus("Enter a peer IP first.")
            return
        }
        appendStatus("Connecting to $host:${PairingStore.DEFAULT_PORT}…")
        connection.connect(
            host = host,
            onConnected = { peerHost -> runOnUiThread {
                appendStatus("Connected to $peerHost")
                sendHello()
            } },
            onMessage = ::handleMessage,
            onDisconnected = { runOnUiThread { appendStatus("Peer disconnected") } },
            onError = { error -> runOnUiThread { appendStatus("Connection error: ${error.message}") } }
        )
    }

    private fun sendHello() {
        val payload = JSONObject()
            .put("deviceId", DeviceIdentity.get(this))
            .put("status", DeviceStatus.snapshot(this))
            .put("capabilities", Capability.entries.map { it.id })
        connection.send(Protocol.message("device_hello", payload))
    }

    private fun handleMessage(message: JSONObject) {
        runOnUiThread {
            if (!Protocol.isSupported(message)) {
                appendStatus("Rejected unsupported protocol message")
                return@runOnUiThread
            }
            when (message.optString("type")) {
                "device_hello" -> {
                    val payload = message.optJSONObject("payload")
                    val peerId = payload?.optString("deviceId").orEmpty()
                    if (peerId.isNotBlank()) {
                        // The connection proof is still unauthenticated. The secure
                        // pairing layer will replace this temporary persistence path.
                        val host = address.text.toString().trim()
                        if (host.isNotBlank()) pairing.save(peerId, host, PairingStore.DEFAULT_PORT)
                    }
                    appendStatus("Peer hello received: ${peerId.take(8)}…")
                    connection.send(Protocol.message("device_status", DeviceStatus.snapshot(this)))
                }
                "device_status" -> appendStatus("Peer status received: ${message.optJSONObject("payload")}")
                "ping" -> {
                    appendStatus("Ping received")
                    connection.send(Protocol.message("pong"))
                }
                "pong" -> appendStatus("Pong received ✓")
                else -> appendStatus("Message: ${message.optString("type")}")
            }
        }
    }

    private fun appendStatus(line: String) {
        if (!::status.isInitialized) return
        status.append("\n$line")
    }

    override fun onDestroy() {
        connection.close()
        super.onDestroy()
    }
}
