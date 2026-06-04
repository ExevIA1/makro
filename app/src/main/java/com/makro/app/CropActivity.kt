package com.makro.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class CropActivity : AppCompatActivity() {

    private lateinit var ivScreenshot: ImageView
    private lateinit var selectionView: SelectionView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var screenshotBitmap: Bitmap? = null
    private var mediaProjection: MediaProjection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)

        ivScreenshot  = findViewById(R.id.ivScreenshot)
        selectionView = findViewById(R.id.selectionView)
        btnSave       = findViewById(R.id.btnSave)
        btnCancel     = findViewById(R.id.btnCancel)

        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener   { saveSelection() }

        // MediaProjection iznini al ve ekran görüntüsü çek
        val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data = intent.getParcelableExtra<Intent>("data")

        if (resultCode == Activity.RESULT_OK && data != null) {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mgr.getMediaProjection(resultCode, data)
            // Kısa gecikme: activity tam açılsın
            Handler(Looper.getMainLooper()).postDelayed({ captureScreen() }, 300)
        } else {
            Toast.makeText(this, "İzin alınamadı", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun captureScreen() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val width  = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        val virtualDisplay: VirtualDisplay = mediaProjection!!.createVirtualDisplay(
            "capture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        // Görüntü hazır olunca oku
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride   = planes[0].rowStride
            val rowPadding  = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            screenshotBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)

            image.close()
            imageReader.close()
            virtualDisplay.release()
            mediaProjection?.stop()

            // UI thread'de göster
            runOnUiThread {
                ivScreenshot.setImageBitmap(screenshotBitmap)
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun saveSelection() {
        val bitmap = screenshotBitmap ?: run {
            Toast.makeText(this, "Ekran görüntüsü bekleniyor...", Toast.LENGTH_SHORT).show()
            return
        }

        if (!selectionView.hasSelection()) {
            Toast.makeText(this, "Bir alan seç", Toast.LENGTH_SHORT).show()
            return
        }

        val rect = selectionView.getSelection()!!

        // View boyutu ile bitmap boyutunu oranla
        val scaleX = bitmap.width.toFloat()  / ivScreenshot.width
        val scaleY = bitmap.height.toFloat() / ivScreenshot.height

        val cropX = (rect.left   * scaleX).toInt().coerceAtLeast(0)
        val cropY = (rect.top    * scaleY).toInt().coerceAtLeast(0)
        val cropW = ((rect.width()  * scaleX).toInt()).coerceAtMost(bitmap.width  - cropX)
        val cropH = ((rect.height() * scaleY).toInt()).coerceAtMost(bitmap.height - cropY)

        if (cropW <= 0 || cropH <= 0) {
            Toast.makeText(this, "Geçersiz seçim", Toast.LENGTH_SHORT).show()
            return
        }

        val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)

        // targets/ klasörüne kaydet
        val dir = File(filesDir, "targets")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "target_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }

        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
    }
}
