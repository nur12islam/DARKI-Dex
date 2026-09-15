package com.darki.dex.host.capture

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import com.darki.dex.host.net.DarkiHostServer

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

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection stopped by system/user")
            releaseCapture(stopProjection = false)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        try {
            startForeground(
                NOTIFICATION_ID,
                notification("Starting screen capture…"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to enter mediaProjection foreground service", t)
            showError("Foreground service failed: ${message(t)}")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (projection != null) return START_STICKY

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val resultData = intent?.parcelableIntentExtra(EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            showError("Screen capture permission data was missing")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val manager = getSystemService(MediaProjectionManager::class.java)
        try {
            projection = checkNotNull(manager.getMediaProjection(resultCode, resultData))
            projection?.registerCallback(projectionCallback, null)
            startCapture()
            updateNotification("Screen capture is running")
            Log.i(TAG, "DARKI-Dex screen capture started successfully")
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to start capture", t)
            showError("Capture failed: ${message(t)}")
            releaseCapture(stopProjection = true)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private fun startCapture() {
        val mediaProjection = checkNotNull(projection)
        val server = DarkiHostServer.current

        val newEncoder = H264Encoder(
            width = WIDTH,
            height = HEIGHT,
            frameRate = FPS,
            bitRate = BITRATE,
            onFormat = { format ->
                val csd0 = format.getByteBuffer("csd-0")?.let { copyBuffer(it) }
                val csd1 = format.getByteBuffer("csd-1")?.let { copyBuffer(it) }
                Log.i(TAG, "Encoder output format: $format")
                if (server != null && csd0 != null) {
                    server.broadcastVideoConfig(WIDTH, HEIGHT, csd0, csd1)
                }
            },
            onFrame = { buffer, info ->
                if (info.size <= 0) return@H264Encoder
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                server?.broadcastVideoFrame(bytes, info.presentationTimeUs, info.flags)
            }
        )
        newEncoder.start()
        encoder = newEncoder

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
            newEncoder.inputSurface,
            null,
            null
        ) ?: error("Android did not create the capture display")

        Log.i(TAG, "Capture started ${WIDTH}x${HEIGHT}@${FPS}fps, source=${metrics.widthPixels}x${metrics.heightPixels}")
    }

    private fun copyBuffer(buffer: java.nio.ByteBuffer): ByteArray {
        val duplicate = buffer.duplicate()
        val bytes = ByteArray(duplicate.remaining())
        duplicate.get(bytes)
        return bytes
    }

    private fun releaseCapture(stopProjection: Boolean) {
        virtualDisplay?.release()
        virtualDisplay = null
        encoder?.stop()
        encoder = null
        projection?.unregisterCallback(projectionCallback)
        if (stopProjection) projection?.stop()
        projection = null
    }

    override fun onDestroy() {
        releaseCapture(stopProjection = true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "DARKI-Dex capture", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    private fun showError(text: String) {
        Log.e(TAG, text)
        updateNotification(text)
    }

    private fun notification(text: String): Notification = Notification.Builder(this, CHANNEL_ID)
        .setContentTitle("DARKI-Dex")
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_menu_view)
        .setOngoing(true)
        .build()

    private fun message(t: Throwable): String =
        t.message?.take(160)?.ifBlank { t.javaClass.simpleName } ?: t.javaClass.simpleName
}

@Suppress("DEPRECATION")
private fun Intent.parcelableIntentExtra(key: String): Intent? = getParcelableExtra(key)
