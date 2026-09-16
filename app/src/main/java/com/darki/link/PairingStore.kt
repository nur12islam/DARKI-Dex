package com.darki.link

import android.content.Context

/** Stores the peer identity and address after explicit pairing. */
class PairingStore(context: Context) {
    private val prefs = context.getSharedPreferences("darki_link_pairing", Context.MODE_PRIVATE)

    fun save(peerId: String, host: String, port: Int) {
        prefs.edit()
            .putString(KEY_PEER_ID, peerId)
            .putString(KEY_HOST, host)
            .putInt(KEY_PORT, port)
            .apply()
    }

    fun peerId(): String? = prefs.getString(KEY_PEER_ID, null)
    fun host(): String? = prefs.getString(KEY_HOST, null)
    fun port(): Int = prefs.getInt(KEY_PORT, DEFAULT_PORT)
    fun isPaired(): Boolean = peerId() != null

    fun clear() = prefs.edit().clear().apply()

    companion object {
        const val DEFAULT_PORT = 45821
        private const val KEY_PEER_ID = "peer_id"
        private const val KEY_HOST = "peer_host"
        private const val KEY_PORT = "peer_port"
    }
}
