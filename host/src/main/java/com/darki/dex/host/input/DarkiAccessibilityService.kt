package com.darki.dex.host.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class DarkiAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "DARKI-Accessibility"

        const val NAV_BACK = 1
        const val NAV_HOME = 2
        const val NAV_RECENTS = 3
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
            DarkiInput.MOUSE_MOVE -> Unit
            DarkiInput.MOUSE_UP -> Unit
            else -> Log.w(TAG, "Unknown mouse action=${event.action}")
        }
    }

    fun dispatchKey(event: DarkiInput.KeyEvent) {
        Log.d(TAG, "Key action=${event.action} code=${event.keyCode} meta=${event.metaState} unicode=${event.unicodeChar}")
    }

    fun dispatchText(text: String) {
        Log.d(TAG, "Text input length=${text.length}")
    }

    fun dispatchNavigation(action: Int) {
        val globalAction = when (action) {
            NAV_BACK -> GLOBAL_ACTION_BACK
            NAV_HOME -> GLOBAL_ACTION_HOME
            NAV_RECENTS -> GLOBAL_ACTION_RECENTS
            else -> null
        }
        if (globalAction != null) {
            performGlobalAction(globalAction)
        } else {
            Log.w(TAG, "Unknown navigation action=$action")
        }
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 40L)
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
