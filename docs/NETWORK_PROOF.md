# Network Proof Milestone

## Goal

Prove that the Z10x host and Lenovo Tab 6 client can establish a DARKI-Dex session over the same Wi-Fi network before adding video transport.

## Sequence

```text
Tab 6                         Z10x
  |                             |
  | ---- TCP connect ----------> |
  | <---- HELLO_ACK ------------ |
  | ---- PING ----------------> |
  | <---- PONG ----------------- |
  |                             |
  |        session ready        |
```

## Rules

- The protocol is versioned.
- Every packet starts with the DARK magic value.
- Control packets are length-prefixed.
- Control payloads are capped at 64 KiB.
- Video frames will use a larger, separately bounded payload path.
- No authentication or pairing secret is implemented yet; this milestone is local-network proof only.

## Next implementation

1. Host TCP listener.
2. Client discovery/manual host address entry.
3. HELLO / HELLO_ACK handshake.
4. PING / PONG keepalive.
5. Connection state and clean reconnect.
6. Only after this passes: H.264 frame transport.
