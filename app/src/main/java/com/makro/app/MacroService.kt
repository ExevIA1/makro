package com.makro.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File

class MacroService : Service() {

    private val CHANNEL_ID   = "MacroChannel"
    private val NOTIF_ID     = 1

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    // WakeLock: ekran açık kalsın
    private var wakeLock: PowerManager.WakeLock? = null

    private val loopRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                captureAndMatch()
                handler.postDelayed(this, 500) // her 500ms
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        acquireWakeLock()
        setupProjection()
        return START_STICKY
    }

    // WakeLock al: ekranı açık tut
    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Makro::WakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // max 10 dakika (gerekirse yenile)
    }

    private fun setupProjection() {
        // MediaProjection iznini SharedPreferences'tan oku
        // (MacroService yeniden başlarsa diye saklıyoruz)
        val prefs = getSharedPreferences("makro", Context.MODE_PRIVATE)
        val resultCode = prefs.getInt("projectionResultCode", -1)

        // Not: Gerçek projekte MediaProjection intentini servis başlatılırken
        // Intent extras ile geçiriyoruz. Burada örnek olarak direkt alıyoruz.
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)

        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888, 2
        )

        // mediaProjection burada servis başlatılırken dışarıdan set edilmeli
        // Aşağıdaki statik referansı kullanıyoruz
        mediaProjection = activeProjection

        if (mediaProjection == null) {
            stopSelf()
            return
        }

        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "MacroCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )

        isRunning = true
        handler.post(loopRunnable)
    }

    private fun captureAndMatch() {
        val image = imageReader?.acquireLatestImage() ?: return

        try {
            val planes    = image.planes
            val buffer    = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride   = planes[0].rowStride

            val metrics = DisplayMetrics()
            val wm = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
            wm.defaultDisplay.getRealMetrics(metrics)
            val width  = metrics.widthPixels
            val height = metrics.heightPixels
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            val screen = Bitmap.createBitmap(bitmap, 0, 0, width, height)

            // Hedef görselleri kontrol et
            val dir = File(filesDir, "targets")
            dir.listFiles()?.filter { it.extension == "png" }?.forEach { file ->
                val target = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                val result = VisionMatcher.findTemplate(screen, target)
                if (result != null) {
                    // Bulundu! Tıkla
                    ClickService.instance?.performClick(result.first, result.second)
                }
            }
        } finally {
            image.close()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Makro Servisi",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MacroService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Makro çalışıyor")
            .setContentText("Görsel aranıyor...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_media_pause, "Durdur", stopIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(loopRunnable)
        virtualDisplay?.release()
        mediaProjection?.stop()
        wakeLock?.release()
        imageReader?.close()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        // MainActivity'den MediaProjection buraya set edilir
        var activeProjection: MediaProjection? = null
    }
}
