package com.darki.link

import android.content.Context
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import org.json.JSONObject

object DeviceStatus {
    fun snapshot(context: Context): JSONObject {
        val battery = context.getSystemService(BatteryManager::class.java)
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capacity = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val charging = battery?.isCharging ?: false
        val network = connectivity?.activeNetwork != null

        return JSONObject()
            .put("model", Build.MODEL)
            .put("manufacturer", Build.MANUFACTURER)
            .put("android", Build.VERSION.RELEASE)
            .put("sdk", Build.VERSION.SDK_INT)
            .put("batteryPercent", capacity)
            .put("charging", charging)
            .put("networkConnected", network)
    }
}
