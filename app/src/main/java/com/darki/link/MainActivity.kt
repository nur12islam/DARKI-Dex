package com.darki.link

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.darki.link.network.LanDiscovery
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var address: EditText
    private val connection = PeerConnection()
    private val pairing by lazy { PairingStore(this) }
    private val discovery by lazy { LanDiscovery(this) }
    private var discoveredPort = PairingStore.DEFAULT_PORT

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
            text = "Make this device discoverable"
            setOnClickListener { startAdvertising() }
        })

        root.addView(Button(this).apply {
            text = "Discover nearby DARKI Link"
            setOnClickListener { discoverPeers() }
        })

        root.addView(Button(this).apply {
            text = "Listen for peer"
            setOnClickListener { startListening() }
        })

        root.addView(Button(this).apply {
            text = "Connect to peer"
            setOnClickListener { connectToPeer(address.text.toString(), discoveredPort) }
        })

        root.addView(Button(this).apply {
            text = "Send ping"
            setOnClickListener {
                val sent = connection.send(Protocol.message("ping"))
                appendStatus(if (sent) "Ping sent" else "No active connection")
            }
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 24, 0, 0)
        }
        root.addView(status)
        setContentView(root)

        appendStatus("Ready — connect both devices to the same network.")
        if (pairing.isPaired()) appendStatus("Saved peer: ${pairing.peerId()?.take(8)}…")
    }

    private fun startAdvertising() {
        discovery.advertise(onEvent = ::appendStatus)
        appendStatus("This device is discoverable on TCP ${PairingStore.DEFAULT_PORT}.")
    }

    private fun discoverPeers() {
        appendStatus("Looking for peers on the current Wi-Fi/hotspot network…")
        discovery.discover(
            onPeer = { host, port, serviceName ->
                runOnUiThread {
                    if (serviceName.startsWith(LanDiscovery.SERVICE_NAME_PREFIX)) {
                        address.setText(host)
                        discoveredPort = port
                        appendStatus("Found $serviceName at $host:$port")
                        appendStatus("Review the address, then tap Connect to peer.")
                    }
                }
            },
            onEvent = ::appendStatus
        )
    }

    private fun startListening() {
        appendStatus("Listening on TCP ${PairingStore.DEFAULT_PORT}…")
        connection.listen(
            port = PairingStore.DEFAULT_PORT,
            onConnected = { host -> runOnUiThread {
                address.setText(host)
                discoveredPort = PairingStore.DEFAULT_PORT
                appendStatus("Connected from $host")
                sendHello()
            } },
            onMessage = ::handleMessage,
            onDisconnected = { runOnUiThread { appendStatus("Peer disconnected") } },
            onError = { error -> runOnUiThread { appendStatus("Listen error: ${error.message}") } }
        )
    }

    private fun connectToPeer(host: String, port: Int = PairingStore.DEFAULT_PORT) {
        val cleanHost = host.trim()
        if (cleanHost.isBlank()) {
            appendStatus("Discover a peer or enter its IP first.")
            return
        }
        appendStatus("Connecting to $cleanHost:$port…")
        connection.connect(
            host = cleanHost,
            port = port,
            onConnected = { peerHost -> runOnUiThread {
                appendStatus("Connected to $peerHost:$port")
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
                        val host = address.text.toString().trim()
                        if (host.isNotBlank()) pairing.save(peerId, host, discoveredPort)
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
        discovery.close()
        connection.close()
        super.onDestroy()
    }
}
