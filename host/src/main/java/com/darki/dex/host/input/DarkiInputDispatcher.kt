package com.darki.dex.host.input

import android.util.Log
import android.view.KeyEvent

/**
 * Host-side input bridge. AccessibilityService owns the actual Android gesture/navigation
 * injection because ordinary applications cannot inject arbitrary touch events system-wide.
 */
object DarkiInputDispatcher {
    private const val TAG = "DARKI-Input"

    @Volatile
    var accessibilityService: DarkiAccessibilityService? = null

    fun dispatchMouse(event: DarkiInput.MouseEvent) {
        accessibilityService?.dispatchMouse(event)
            ?: Log.d(TAG, "Mouse received but accessibility service is not enabled")
    }

    fun dispatchKey(event: DarkiInput.KeyEvent) {
        val service = accessibilityService
        if (service == null) {
            Log.d(TAG, "Key received but accessibility service is not enabled")
            return
        }
        service.dispatchKey(event)
    }

    fun dispatchText(text: String) {
        accessibilityService?.dispatchText(text)
            ?: Log.d(TAG, "Text received but accessibility service is not enabled")
    }
}
