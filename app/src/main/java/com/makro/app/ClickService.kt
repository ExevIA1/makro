package com.makro.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class ClickService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Verilen koordinata tıkla.
     * AccessibilityService ile root'suz çalışır.
     */
    fun performClick(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,   // gecikme yok
                    100L  // 100ms basılı tut
                )
            )
            .build()

        dispatchGesture(gesture, null, null)
    }

    companion object {
        // MacroService buradan erişir
        var instance: ClickService? = null
    }
}
