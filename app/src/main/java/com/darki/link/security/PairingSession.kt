package com.darki.link.security

import java.security.KeyFactory
import java.security.KeyPair
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.atomic.AtomicReference

/** In-memory state for one authenticated DARKI Link session. */
class PairingSession(private val localIdentity: KeyPair) {
    enum class State { IDLE, CHALLENGE_SENT, AUTHENTICATED, REJECTED }

    private val stateRef = AtomicReference(State.IDLE)
    private var peerKey: PublicKey? = null
    private var sessionKey: ByteArray? = null

    val state: State get() = stateRef.get()

    fun reset() {
        peerKey = null
        sessionKey = null
        stateRef.set(State.IDLE)
    }

    fun createChallenge(): String {
        stateRef.set(State.CHALLENGE_SENT)
        return SessionCrypto.publicKeyBase64(localIdentity)
    }

    fun acceptPeerPublicKey(encoded: ByteArray): String {
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(encoded))
        peerKey = key
        val shared = SessionCrypto.deriveSharedSecret(localIdentity.private, key)
        sessionKey = SessionCrypto.sessionKey(shared)
        return SessionCrypto.fingerprint(key.encoded)
    }

    fun authenticate(transcript: ByteArray, peerSignature: ByteArray): Boolean {
        val key = peerKey ?: return false
        val valid = SessionCrypto.verify(transcript, peerSignature, key)
        stateRef.set(if (valid) State.AUTHENTICATED else State.REJECTED)
        return valid
    }

    fun sign(transcript: ByteArray): ByteArray = SessionCrypto.sign(transcript, localIdentity)

    fun encrypt(plaintext: ByteArray): ByteArray = SessionCrypto.encrypt(plaintext, requireSessionKey())

    fun decrypt(packet: ByteArray): ByteArray = SessionCrypto.decrypt(packet, requireSessionKey())

    fun fingerprint(): String? = peerKey?.let { SessionCrypto.fingerprint(it.encoded) }

    private fun requireSessionKey(): ByteArray = check(state == State.AUTHENTICATED) {
        "DARKI Link session is not authenticated"
    }.let { sessionKey!! }
}
