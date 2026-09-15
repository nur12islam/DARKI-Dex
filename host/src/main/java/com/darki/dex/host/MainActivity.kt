package com.darki.dex.host

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val projectionRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "DARKI-Dex Host\n\nPhone: iQOO Z10x\nRole: Host / Computing Device\n\nPhase 1: Screen capture proof"
            textSize = 18f
            setPadding(48, 48, 48, 32)
        }

        val start = Button(this).apply {
            text = "Start screen capture test"
            setOnClickListener { requestProjection() }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(start)
        })
    }

    private fun requestProjection() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        startActivityForResult(manager.createScreenCaptureIntent(), projectionRequest)
    }

    @Deprecated("Uses the platform activity-result callback for the initial prototype")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != projectionRequest || resultCode != RESULT_OK || data == null) return

        val serviceIntent = Intent(this, capture.ScreenCaptureService::class.java).apply {
            putExtra(capture.ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(capture.ScreenCaptureService.EXTRA_RESULT_DATA, data)
        }
        startForegroundService(serviceIntent)
    }
}
