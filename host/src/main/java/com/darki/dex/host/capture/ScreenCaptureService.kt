package com.darki.dex.host.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log

class ScreenCaptureService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "darkscreen"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "DARKI-Capture"
        private const val WIDTH = 1280
        private const val HEIGHT = 720
        private const val FPS = 30
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var encoder: H264Encoder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification("Starting capture…"))

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val resultData = intent?.parcelableIntentExtra(EXTRA_RESULT_DATA)
            ?: return START_NOT_STICKY

        runCatching {
            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = manager.getMediaProjection(resultCode, resultData)
                ?: error("MediaProjection could not be created")

            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection stopped by system/user")
                    stopCapture()
                    stopSelf()
                }
            }, null)

            encoder = H264Encoder(
                width = WIDTH,
                height = HEIGHT,
                frameRate = FPS,
                onFormat = { format -> Log.i(TAG, "H.264 format negotiated: $format") },
                onFrame = { _, info ->
                    val keyFrame = (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                    Log.v(TAG, "Encoded frame size=${info.size} pts=${info.presentationTimeUs} key=$keyFrame")
                }
            ).also { it.start() }

            virtualDisplay = projection!!.createVirtualDisplay(
                "DARKI-Dex",
                WIDTH,
                HEIGHT,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                encoder!!.inputSurface,
                null,
                null
            ) ?: error("VirtualDisplay could not be created")

            Log.i(TAG, "Capture pipeline started: ${WIDTH}x${HEIGHT}@${FPS}fps")
            startForeground(NOTIFICATION_ID, notification("Screen capture + H.264 encoder running"))
        }.onFailure { error ->
            Log.e(TAG, "Capture pipeline failed", error)
            stopCapture()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun stopCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        encoder?.stop()
        encoder = null
        projection?.stop()
        projection = null
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "DARKI-Dex capture", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(text: String): Notification = Notification.Builder(this, CHANNEL_ID)
        .setContentTitle("DARKI-Dex")
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_menu_view)
        .setOngoing(true)
        .build()
}

@Suppress("DEPRECATION")
private fun Intent.parcelableIntentExtra(key: String): Intent? =
    if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(key, Intent::class.java)
    else getParcelableExtra(key)
