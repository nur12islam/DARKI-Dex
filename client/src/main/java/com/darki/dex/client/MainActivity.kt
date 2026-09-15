package com.darki.dex.client

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.darki.dex.client.net.DarkiClient
import com.darki.dex.client.net.DarkiDiscovery
import com.darki.dex.client.stream.H264Decoder

class MainActivity : Activity(), SurfaceHolder.Callback {
    companion object {
        private const val HOST_WIDTH = 1280f
        private const val HOST_HEIGHT = 720f
    }

    private val discovery = DarkiDiscovery()
    private val client = DarkiClient()
    private val hosts = linkedMapOf<String, DarkiDiscovery.Host>()
    private lateinit var status: TextView
    private lateinit var hostList: LinearLayout
    private lateinit var scanButton: Button
    private lateinit var surfaceView: SurfaceView
    private var decoder: H264Decoder? = null
    private var surfaceReady = false
    private var pendingConfig: DarkiClient.VideoConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        startDiscovery()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        status = TextView(this).apply { text = "Searching for DARKI-Dex hosts…"; textSize = 15f; setPadding(0, 16, 0, 16) }
        val title = TextView(this).apply { text = "DARKI-Dex Desktop"; textSize = 28f }
        val device = TextView(this).apply { text = "Lenovo Tab 6 • Desktop Client"; textSize = 16f }
        scanButton = Button(this).apply { text = "Scan again"; setOnClickListener { startDiscovery() } }
        surfaceView = SurfaceView(this).apply {
            setBackgroundColor(Color.BLACK)
            holder.addCallback(this@MainActivity)
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                    client.sendKey(event.action, keyCode, event.metaState, event.unicodeChar)
                    true
                } else false
            }
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        requestFocus()
                        sendMouse(DarkiMouseAction.DOWN, event)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        sendMouse(DarkiMouseAction.UP, event)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        sendMouse(DarkiMouseAction.MOVE, event)
                        true
                    }
                    else -> true
                }
            }
            setOnGenericMotionListener { _, event ->
                if (event.action == MotionEvent.ACTION_SCROLL) {
                    sendMouse(DarkiMouseAction.SCROLL, event, event.getAxisValue(MotionEvent.AXIS_HSCROLL), event.getAxisValue(MotionEvent.AXIS_VSCROLL))
                    true
                } else false
            }
            visibility = View.GONE
        }
        hostList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(title)
        root.addView(device)
        root.addView(status)
        root.addView(scanButton)
        root.addView(surfaceView, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(hostList)
        setContentView(root)
    }

    private fun sendMouse(action: Int, event: MotionEvent, scrollX: Float = 0f, scrollY: Float = 0f) {
        val viewWidth = surfaceView.width.takeIf { it > 0 }?.toFloat() ?: return
        val viewHeight = surfaceView.height.takeIf { it > 0 }?.toFloat() ?: return
        val x = (event.x / viewWidth * HOST_WIDTH).coerceIn(0f, HOST_WIDTH)
        val y = (event.y / viewHeight * HOST_HEIGHT).coerceIn(0f, HOST_HEIGHT)
        client.sendMouse(action, x, y, event.buttonState, scrollX, scrollY)
    }

    private object DarkiMouseAction {
        const val DOWN = 1
        const val UP = 2
        const val MOVE = 0
        const val SCROLL = 3
    }

    private fun startDiscovery() {
        client.close()
        decoder?.stop(); decoder = null
        surfaceView.visibility = View.GONE
        pendingConfig = null
        hosts.clear(); hostList.removeAllViews()
        status.text = "Searching local network…"
        discovery.start(object : DarkiDiscovery.Callback {
            override fun onStarted() = runOnUiThread { status.text = "Listening for DARKI-Dex hosts…"; scanButton.isEnabled = true }
            override fun onHost(host: DarkiDiscovery.Host) = runOnUiThread {
                val key = "${host.address}:${host.port}"
                if (hosts.putIfAbsent(key, host) == null) addHost(host)
                status.text = "${hosts.size} host${if (hosts.size == 1) "" else "s"} found"
            }
            override fun onError(error: Throwable) = runOnUiThread { status.text = "Discovery error: ${error.message ?: error.javaClass.simpleName}"; scanButton.isEnabled = true }
        })
    }

    private fun addHost(host: DarkiDiscovery.Host) {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        val name = TextView(this).apply { text = host.name; textSize = 19f }
        val address = TextView(this).apply { text = "${host.address}:${host.port}"; textSize = 14f }
        val connect = Button(this).apply {
            text = "Connect"
            setOnClickListener {
                isEnabled = false; text = "Connecting…"; status.text = "Connecting to ${host.name}…"
                client.connect(host.address, host.port, object : DarkiClient.Callback {
                    override fun onConnected(hostAddress: String) = runOnUiThread {
                        status.text = "Connected • waiting for video…"
                        surfaceView.requestFocus()
                    }
                    override fun onPacket(type: Int, payload: ByteArray) = runOnUiThread { handlePacket(type, payload) }
                    override fun onError(error: Throwable) = runOnUiThread {
                        status.text = "Connection lost: ${error.message ?: error.javaClass.simpleName}"
                        isEnabled = true; text = "Connect"
                        decoder?.stop(); decoder = null
                    }
                })
            }
        }
        card.addView(name); card.addView(address); card.addView(connect); hostList.addView(card)
    }

    private fun handlePacket(type: Int, payload: ByteArray) {
        when (type) {
            DarkiClient.TYPE_VIDEO_CONFIG -> {
                val config = runCatching { client.parseVideoConfig(payload) }.getOrElse {
                    status.text = "Invalid video configuration: ${it.message}"; return
                }
                pendingConfig = config
                status.text = "Video ${config.width}×${config.height} • preparing decoder…"
                startDecoderIfReady()
            }
            DarkiClient.TYPE_VIDEO_FRAME -> {
                val frame = runCatching { client.parseVideoFrame(payload) }.getOrElse { return }
                decoder?.queueAccessUnit(frame.data, frame.presentationTimeUs, frame.flags)
            }
            DarkiClient.TYPE_PONG -> status.text = "Connected"
        }
    }

    private fun startDecoderIfReady() {
        val config = pendingConfig ?: return
        if (!surfaceReady) return
        decoder?.stop()
        decoder = runCatching {
            H264Decoder(surfaceView.holder.surface, config.width, config.height, config.csd0, config.csd1) { error ->
                runOnUiThread { status.text = "Decoder error: ${error.message ?: error.javaClass.simpleName}" }
            }.also { it.start() }
        }.getOrElse {
            status.text = "Decoder start failed: ${it.message}"; null
        }
        if (decoder != null) {
            surfaceView.visibility = View.VISIBLE
            status.text = "DARKI-Dex stream active • ${config.width}×${config.height}"
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) { surfaceReady = true; startDecoderIfReady() }
    override fun surfaceDestroyed(holder: SurfaceHolder) { surfaceReady = false; decoder?.stop(); decoder = null }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun onDestroy() {
        decoder?.stop()
        discovery.shutdown()
        client.shutdown()
        super.onDestroy()
    }
}
