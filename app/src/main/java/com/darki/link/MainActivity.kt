package com.darki.link

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            textSize = 18f
            setPadding(48, 64, 48, 48)
        }
        setContentView(status)

        val deviceId = DeviceIdentity.get(this)
        val capabilities = Capability.entries.map { it.id }
        status.text = buildString {
            appendLine("DARKI Link")
            appendLine()
            appendLine("Foundation build")
            appendLine("Device ID: $deviceId")
            appendLine()
            appendLine("Capabilities")
            capabilities.forEach { appendLine("• $it") }
            appendLine()
            appendLine("Status: ready")
        }

        // Protocol smoke-test object. Networking is intentionally not started
        // automatically until pairing/security is implemented.
        val hello = Protocol.message(
            "hello",
            JSONObject().put("deviceId", deviceId).put("protocol", Protocol.VERSION)
        )
        status.append("\nProtocol: ${hello.optString("type")} v${hello.optInt("version")}")
    }
}
