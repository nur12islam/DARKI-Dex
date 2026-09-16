package com.darki.link.security

import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Small platform-only crypto layer for DARKI Link sessions. */
object SessionCrypto {
    private const val KEY_SIZE = 256
    private const val GCM_TAG_BITS = 128
    private const val IV_SIZE = 12

    fun newIdentityKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply {
            initialize(256)
        }.generateKeyPair()

    fun publicKeyBase64(keyPair: KeyPair): String =
        Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

    fun deriveSharedSecret(privateKey: java.security.PrivateKey, peerPublicKey: java.security.PublicKey): ByteArray =
        KeyAgreement.getInstance("ECDH").apply {
            init(privateKey)
            doPhase(peerPublicKey, true)
        }.generateSecret()

    fun sessionKey(sharedSecret: ByteArray, context: ByteArray = "DARKI-LINK-v1".toByteArray()): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(sharedSecret, "HmacSHA256"))
        return mac.doFinal(context).copyOf(KEY_SIZE / 8)
    }

    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return ByteBuffer.allocate(iv.size + ciphertext.size).put(iv).put(ciphertext).array()
    }

    fun decrypt(packet: ByteArray, key: ByteArray): ByteArray {
        require(packet.size > IV_SIZE) { "Invalid encrypted packet" }
        val iv = packet.copyOfRange(0, IV_SIZE)
        val ciphertext = packet.copyOfRange(IV_SIZE, packet.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun fingerprint(publicKeyEncoded: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(publicKeyEncoded)
            .joinToString("") { "%02x".format(it) }

    fun sign(data: ByteArray, keyPair: KeyPair): ByteArray =
        Signature.getInstance("SHA256withECDSA").apply {
            initSign(keyPair.private)
            update(data)
        }.sign()

    fun verify(data: ByteArray, signature: ByteArray, publicKey: java.security.PublicKey): Boolean =
        runCatching {
            Signature.getInstance("SHA256withECDSA").apply {
                initVerify(publicKey)
                update(data)
            }.verify(signature)
        }.getOrDefault(false)
}
