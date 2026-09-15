package com.darki.dex.host.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
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
        private const val BITRATE = 6_000_000
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var encoder: H264Encoder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())

        if (projection != null) return START_STICKY

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val resultData = intent?.parcelableIntentExtra(EXTRA_RESULT_DATA) ?: return START_NOT_STICKY
        val manager = getSystemService(MediaProjectionManager::class.java)

        projection = manager.getMediaProjection(resultCode, resultData).also { mediaProjection ->
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection stopped by system/user")
                    stopCapture()
                    stopSelf()
                }
            }, null)
        }

        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        val mediaProjection = checkNotNull(projection)

        val encoder = H264Encoder(
            width = WIDTH,
            height = HEIGHT,
            frameRate = FPS,
            bitRate = BITRATE,
            onFormat = { format -> Log.i(TAG, "Encoder configured: $format") },
            onFrame = { _, info ->
                Log.d(TAG, "Encoded frame size=${info.size} flags=${info.flags} ptsUs=${info.presentationTimeUs}")
            }
        )
        encoder.start()
        this.encoder = encoder

        val metrics = DisplayMetrics().also {
            @Suppress("DEPRECATION")
            (getSystemService(DISPLAY_SERVICE) as android.view.WindowManager).defaultDisplay.getRealMetrics(it)
        }

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "DARKI-Dex",
            WIDTH,
            HEIGHT,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            encoder.inputSurface,
            null,
            null
        )

        Log.i(TAG, "Capture started ${WIDTH}x${HEIGHT}@${FPS}fps, source=${metrics.widthPixels}x${metrics.heightPixels}")
    }

    private fun stopCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        encoder?.stop()
        encoder = null
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()
        projection = null
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() = Unit
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "DARKI-Dex capture", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(): Notification = Notification.Builder(this, CHANNEL_ID)
        .setContentTitle("DARKI-Dex")
        .setContentText("Screen capture is running")
        .setSmallIcon(android.R.drawable.ic_menu_view)
        .setOngoing(true)
        .build()
}

@Suppress("DEPRECATION")
private fun Intent.parcelableIntentExtra(key: String): Intent? = getParcelableExtra(key)
