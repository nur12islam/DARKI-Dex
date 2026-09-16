package com.darki.link.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.PublicKey

/** Helpers for the authenticated, human-confirmed DARKI Link pairing handshake. */
object PairingProtocol {
    const val HELLO = "auth_hello"
    const val PROOF = "auth_proof"
    const val ACCEPT = "auth_accept"
    const val REJECT = "auth_reject"

    fun transcript(localId: String, peerId: String, localPublicKey: ByteArray, peerPublicKey: ByteArray, localNonce: String, peerNonce: String): ByteArray {
        val parts = listOf(localId to localPublicKey, peerId to peerPublicKey).sortedBy { it.first }
        val ids = parts.joinToString("|") { it.first }
        val keys = parts.joinToString("|") { Base64.encodeToString(it.second, Base64.NO_WRAP) }
        val nonces = listOf(localNonce, peerNonce).sorted().joinToString("|")
        return "DARKI-LINK-AUTH-v1\n$ids\n$keys\n$nonces".toByteArray(StandardCharsets.UTF_8)
    }

    fun confirmationCode(transcript: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(transcript)
        val number = ((digest[0].toInt() and 0xff) shl 16) or ((digest[1].toInt() and 0xff) shl 8) or (digest[2].toInt() and 0xff)
        return "%06d".format(number % 1_000_000)
    }

    fun verifyProof(transcript: ByteArray, signature: ByteArray, peerPublicKey: PublicKey): Boolean =
        SessionCrypto.verify(transcript, signature, peerPublicKey)
}
