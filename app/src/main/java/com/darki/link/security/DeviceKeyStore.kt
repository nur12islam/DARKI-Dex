package com.darki.link.security

import android.content.Context
import android.util.Base64
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.KeyPairGenerator
import javax.security.auth.x500.X500Principal

/** Persistent EC identity backed by Android Keystore. */
class DeviceKeyStore(private val context: Context) {
    companion object {
        private const val STORE = "AndroidKeyStore"
        private const val ALIAS = "darki-link-identity-v1"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(STORE).apply { load(null) }

    fun keyPair(): KeyPair {
        if (!keyStore.containsAlias(ALIAS)) generate()
        val entry = keyStore.getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry
        return KeyPair(entry.certificate.publicKey, entry.privateKey)
    }

    fun publicKeyBase64(): String =
        Base64.encodeToString(keyPair().public.encoded, Base64.NO_WRAP)

    fun fingerprint(): String = SessionCrypto.fingerprint(keyPair().public.encoded)

    private fun generate() {
        val generator = KeyPairGenerator.getInstance("EC", STORE)
        generator.initialize(
            android.security.keystore.KeyGenParameterSpec.Builder(
                ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_SIGN or
                    android.security.keystore.KeyProperties.PURPOSE_AGREE_KEY
            )
                .setDigests(android.security.keystore.KeyProperties.DIGEST_SHA256)
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                .setUserAuthenticationRequired(false)
                .build()
        )
        generator.generateKeyPair()
    }
}
