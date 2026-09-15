package com.darki.dex.host

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.darki.dex.host.capture.ScreenCaptureService
import com.darki.dex.host.net.DarkiDiscovery
import com.darki.dex.host.net.DarkiHostServer

class MainActivity : Activity() {
    private val projectionRequest = 1001
    private var server: DarkiHostServer? = null
    private var beacon: DarkiDiscovery.HostBeacon? = null
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        server = DarkiHostServer().also { it.start() }
        beacon = DarkiDiscovery.HostBeacon("iQOO Z10x • DARKI-Dex").also { it.start() }

        val title = TextView(this).apply {
            text = "DARKI-Dex Host\n\nPhone: iQOO Z10x\nRole: Host / Computing Device\n\nNetwork: Ready\nTCP: ${DarkiHostServer.DEFAULT_PORT}\nDiscovery: UDP ${DarkiDiscovery.PORT}"
            textSize = 18f
            setPadding(48, 48, 48, 32)
        }

        status = TextView(this).apply {
            text = "Capture idle"
            textSize = 15f
            setPadding(48, 16, 48, 16)
        }

        val start = Button(this).apply {
            text = "Start screen capture test"
            setOnClickListener { requestProjection() }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(status)
            addView(start)
        })
    }

    private fun requestProjection() {
        status.text = "Requesting Android screen-capture permission…"
        runCatching {
            val manager = getSystemService(MediaProjectionManager::class.java)
            startActivityForResult(manager.createScreenCaptureIntent(), projectionRequest)
        }.onFailure { error ->
            showCaptureError("Could not open screen capture: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    override fun onDestroy() {
        beacon?.close()
        beacon = null
        server?.close()
        server = null
        super.onDestroy()
    }

    @Deprecated("Uses the platform activity-result callback for the initial prototype")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != projectionRequest) return
        if (resultCode != RESULT_OK || data == null) {
            status.text = "Screen capture permission was cancelled"
            return
        }

        status.text = "Permission granted • starting capture service…"
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
        }
        runCatching {
            startForegroundService(serviceIntent)
        }.onSuccess {
            status.text = "Capture service started • waiting for encoder…"
        }.onFailure { error ->
            showCaptureError("Capture service failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun showCaptureError(message: String) {
        status.text = message
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }
}
