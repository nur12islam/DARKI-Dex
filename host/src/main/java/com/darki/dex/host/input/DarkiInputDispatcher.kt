package com.darki.dex.host.input

import android.util.Log

/** Host-side bridge between DARKI network packets and Android input services. */
object DarkiInputDispatcher {
    private const val TAG = "DARKI-Input"

    @Volatile
    var accessibilityService: DarkiAccessibilityService? = null

    fun dispatchMouse(event: DarkiInput.MouseEvent) {
        accessibilityService?.dispatchMouse(event)
            ?: Log.d(TAG, "Mouse received but accessibility service is not enabled")
    }

    fun dispatchKey(event: DarkiInput.KeyEvent) {
        accessibilityService?.dispatchKey(event)
            ?: Log.d(TAG, "Key received but accessibility service is not enabled")
    }

    fun dispatchText(text: String) {
        accessibilityService?.dispatchText(text)
            ?: Log.d(TAG, "Text received but accessibility service is not enabled")
    }

    fun dispatchNavigation(action: Int) {
        accessibilityService?.dispatchNavigation(action)
            ?: Log.d(TAG, "Navigation received but accessibility service is not enabled")
    }
}
