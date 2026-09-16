# DARKI Link Security Model

## Current milestone

DARKI Link now has the cryptographic primitives required for an authenticated encrypted session:

- persistent EC identity in Android Keystore;
- ECDH shared-secret derivation;
- HMAC-SHA-256 session-key derivation;
- AES-256-GCM encryption with a fresh IV per message;
- ECDSA signatures for proof of key possession;
- SHA-256 public-key fingerprints;
- encrypted JSON protocol envelopes.

The existing LAN connection proof remains intentionally separate. Until the handshake is wired into `PeerConnection`, privileged commands must not be treated as trusted merely because a socket is connected.

## Intended handshake

```text
A                                  B
│                                  │
│  secure_hello(pubA, nonceA)      │
├─────────────────────────────────►│
│                                  │
│  secure_hello(pubB, nonceB)      │
│◄─────────────────────────────────┤
│                                  │
│  secure_auth(signatureA)         │
├─────────────────────────────────►│
│                                  │
│  secure_auth(signatureB)         │
│◄─────────────────────────────────┤
│                                  │
│      authenticated + ECDH        │
│      AES-GCM encrypted traffic   │
│◄════════════════════════════════►│
```

The signed transcript must bind both public keys and both nonces to prevent replay and peer-confusion attacks.

## Trust model

Pairing is explicit. A peer's public-key fingerprint should be shown to the user during first pairing. Future sessions should verify the stored peer key rather than silently trusting a changed identity.

Revoking a pairing must remove the stored peer trust record. A changed peer key should require explicit re-pairing.

## Transport rule

LAN, Wi-Fi Direct and Bluetooth are transports, not trust boundaries. DARKI Link security remains enabled regardless of which transport carries the session.

## Next security work

1. wire the handshake into the connection manager;
2. persist trusted peer public keys;
3. add QR/numeric pairing confirmation;
4. reject commands before authentication;
5. add replay/sequence protection to encrypted messages;
6. add clean key revocation and re-pairing UI.
