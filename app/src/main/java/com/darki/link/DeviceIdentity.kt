package com.darki.link

import android.content.Context
import java.util.UUID

/** Persistent identity for this DARKI Link installation. */
object DeviceIdentity {
    private const val PREFS = "darki_link_identity"
    private const val KEY_DEVICE_ID = "device_id"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }
}
