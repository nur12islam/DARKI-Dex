package com.darki.dex.host.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class DarkiAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "DARKI-Accessibility"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        DarkiInputDispatcher.accessibilityService = this
        Log.i(TAG, "Accessibility input service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (DarkiInputDispatcher.accessibilityService === this) {
            DarkiInputDispatcher.accessibilityService = null
        }
        super.onDestroy()
    }

    fun dispatchMouse(event: DarkiInput.MouseEvent) {
        when (event.action) {
            DarkiInput.MOUSE_DOWN -> tap(event.x, event.y)
            DarkiInput.MOUSE_SCROLL -> scroll(event.x, event.y, event.scrollY)
            DarkiInput.MOUSE_MOVE -> move(event.x, event.y)
            DarkiInput.MOUSE_UP -> Unit
            else -> Log.w(TAG, "Unknown mouse action=${event.action}")
        }
    }

    fun dispatchKey(event: DarkiInput.KeyEvent) {
        // AccessibilityService cannot inject arbitrary hardware key events into every app.
        // Keep the event visible in logs until the dedicated IME/text path is implemented.
        Log.d(TAG, "Key action=${event.action} code=${event.keyCode} meta=${event.metaState} unicode=${event.unicodeChar}")
    }

    fun dispatchText(text: String) {
        Log.d(TAG, "Text input length=${text.length}")
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 1L)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun move(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 1L)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun scroll(x: Float, y: Float, amount: Float) {
        val distance = amount.coerceIn(-600f, 600f)
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y + distance)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 250L)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }
}
