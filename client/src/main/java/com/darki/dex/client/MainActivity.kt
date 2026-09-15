package com.darki.dex.client

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.darki.dex.client.net.DarkiClient
import com.darki.dex.client.net.DarkiDiscovery

class MainActivity : Activity() {
    private val discovery = DarkiDiscovery()
    private val client = DarkiClient()
    private val hosts = linkedMapOf<String, DarkiDiscovery.Host>()

    private lateinit var status: TextView
    private lateinit var hostList: LinearLayout
    private lateinit var scanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        startDiscovery()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "DARKI-Dex Desktop"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        }

        val device = TextView(this).apply {
            text = "Lenovo Tab 6 • Desktop Client"
            textSize = 16f
        }

        status = TextView(this).apply {
            text = "Searching for DARKI-Dex hosts…"
            textSize = 15f
            setPadding(0, 28, 0, 20)
        }

        scanButton = Button(this).apply {
            text = "Scan again"
            setOnClickListener { startDiscovery() }
        }

        hostList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(title)
        root.addView(device)
        root.addView(status)
        root.addView(scanButton)
        root.addView(hostList)
        setContentView(root)
    }

    private fun startDiscovery() {
        hosts.clear()
        hostList.removeAllViews()
        status.text = "Searching local network…"
        scanButton.isEnabled = false

        discovery.start(object : DarkiDiscovery.Callback {
            override fun onStarted() = runOnUiThread {
                status.text = "Listening for DARKI-Dex hosts…"
                scanButton.isEnabled = true
            }

            override fun onHost(host: DarkiDiscovery.Host) = runOnUiThread {
                val key = "${host.address}:${host.port}"
                if (hosts.putIfAbsent(key, host) == null) addHost(host)
                status.text = "${hosts.size} host${if (hosts.size == 1) "" else "s"} found"
            }

            override fun onError(error: Throwable) = runOnUiThread {
                status.text = "Discovery error: ${error.message ?: error.javaClass.simpleName}"
                scanButton.isEnabled = true
            }
        })
    }

    private fun addHost(host: DarkiDiscovery.Host) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER_VERTICAL
        }

        val name = TextView(this).apply {
            text = host.name
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
        }

        val address = TextView(this).apply {
            text = "${host.address}:${host.port}"
            textSize = 14f
        }

        val connect = Button(this).apply {
            text = "Connect"
            setOnClickListener {
                isEnabled = false
                text = "Connecting…"
                status.text = "Connecting to ${host.name}…"
                client.connect(host.address, host.port, object : DarkiClient.Callback {
                    override fun onConnected(hostAddress: String) = runOnUiThread {
                        text = "Connected ✓"
                        status.text = "Connected to ${host.name}"
                    }

                    override fun onError(error: Throwable) = runOnUiThread {
                        isEnabled = true
                        text = "Connect"
                        status.text = "Connection failed: ${error.message ?: error.javaClass.simpleName}"
                    }
                })
            }
        }

        card.addView(name)
        card.addView(address)
        card.addView(connect)
        hostList.addView(card)
    }

    override fun onDestroy() {
        discovery.shutdown()
        client.shutdown()
        super.onDestroy()
    }
}
