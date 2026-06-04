package com.makro.app

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

object VisionMatcher {

    // Eşleşme eşiği: %80 ve üzeri eşleşme kabul edilir
    private const val THRESHOLD = 0.80

    /**
     * Ekran görüntüsünde hedef görseli ara.
     * Bulunursa merkez koordinatını (x, y) döndür, bulamazsa null.
     */
    fun findTemplate(screen: Bitmap, template: Bitmap): Pair<Int, Int>? {
        // Bitmap → OpenCV Mat
        val screenMat   = bitmapToMat(screen)
        val templateMat = bitmapToMat(template)

        // Gri tonlamaya çevir (daha hızlı ve güvenilir eşleşme)
        val screenGray   = Mat()
        val templateGray = Mat()
        Imgproc.cvtColor(screenMat, screenGray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_RGBA2GRAY)

        // Template matching
        val result = Mat()
        Imgproc.matchTemplate(screenGray, templateGray, result, Imgproc.TM_CCOEFF_NORMED)

        // En iyi eşleşmeyi bul
        val mmResult = Core.minMaxLoc(result)
        val maxVal   = mmResult.maxVal
        val maxLoc   = mmResult.maxLoc

        // Belleği temizle
        screenMat.release()
        templateMat.release()
        screenGray.release()
        templateGray.release()
        result.release()

        return if (maxVal >= THRESHOLD) {
            // Eşleşen alanın merkezi
            val centerX = (maxLoc.x + templateMat.cols() / 2).toInt()
            val centerY = (maxLoc.y + templateMat.rows() / 2).toInt()
            Pair(centerX, centerY)
        } else {
            null // Bulunamadı
        }
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }
}
