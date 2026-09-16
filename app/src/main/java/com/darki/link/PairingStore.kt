package com.darki.link

import android.content.Context

/** Stores the explicitly trusted peer identity, key and address. */
class PairingStore(context: Context) {
    private val prefs = context.getSharedPreferences("darki_link_pairing", Context.MODE_PRIVATE)

    fun save(peerId: String, host: String, port: Int, publicKey: String, fingerprint: String) {
        prefs.edit()
            .putString(KEY_PEER_ID, peerId)
            .putString(KEY_HOST, host)
            .putInt(KEY_PORT, port)
            .putString(KEY_PUBLIC_KEY, publicKey)
            .putString(KEY_FINGERPRINT, fingerprint)
            .apply()
    }

    fun peerId(): String? = prefs.getString(KEY_PEER_ID, null)
    fun host(): String? = prefs.getString(KEY_HOST, null)
    fun port(): Int = prefs.getInt(KEY_PORT, DEFAULT_PORT)
    fun publicKey(): String? = prefs.getString(KEY_PUBLIC_KEY, null)
    fun fingerprint(): String? = prefs.getString(KEY_FINGERPRINT, null)
    fun isPaired(): Boolean = peerId() != null && publicKey() != null

    fun clear() = prefs.edit().clear().apply()

    companion object {
        const val DEFAULT_PORT = 45821
        private const val KEY_PEER_ID = "peer_id"
        private const val KEY_HOST = "peer_host"
        private const val KEY_PORT = "peer_port"
        private const val KEY_PUBLIC_KEY = "peer_public_key"
        private const val KEY_FINGERPRINT = "peer_fingerprint"
    }
}
