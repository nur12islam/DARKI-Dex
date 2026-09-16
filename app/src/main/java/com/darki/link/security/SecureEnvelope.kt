package com.darki.link.security

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/** Encodes protocol JSON inside an authenticated AES-GCM envelope. */
object SecureEnvelope {
    private const val FIELD_CIPHER = "ciphertext"

    fun wrap(message: JSONObject, session: PairingSession): JSONObject {
        val encrypted = session.encrypt(message.toString().toByteArray(StandardCharsets.UTF_8))
        return JSONObject()
            .put("version", 1)
            .put("type", "secure")
            .put(FIELD_CIPHER, Base64.encodeToString(encrypted, Base64.NO_WRAP))
    }

    fun unwrap(envelope: JSONObject, session: PairingSession): JSONObject {
        require(envelope.optString("type") == "secure") { "Not a DARKI secure envelope" }
        val encoded = envelope.getString(FIELD_CIPHER)
        val plaintext = session.decrypt(Base64.decode(encoded, Base64.NO_WRAP))
        return JSONObject(String(plaintext, StandardCharsets.UTF_8))
    }
}
