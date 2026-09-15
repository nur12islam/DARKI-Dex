# DARKI Protocol — Draft v0

This is the initial application-level protocol for DARKI-Dex. It is deliberately independent of scrcpy's internal protocol so the project can evolve its own compatibility and feature model.

## 1. Design goals

- versioned
- capability-driven
- transport-independent
- low overhead for video/input
- human-readable control messages during early development
- explicit errors
- reconnectable sessions
- authenticated before control

## 2. Session sequence

```text
CLIENT → HELLO
HOST   → HELLO_ACK
CLIENT → PAIR/AUTH
HOST   → AUTH_OK
CLIENT → CAPABILITIES
HOST   → CAPABILITIES_ACK
CLIENT ↔ HEARTBEAT
HOST   → READY
```

## 3. Message envelope

Initial control messages may use JSON objects with:

```json
{
  "version": 1,
  "type": "HELLO",
  "requestId": "...",
  "timestamp": 0,
  "payload": {}
}
```

Video/input payloads should not remain JSON once performance testing begins. They should move to compact binary framing while retaining the same logical message types.

## 4. Core messages

### HELLO

Contains:

- protocol version
- client build version
- device model
- Android API level
- supported codecs
- transport capabilities

### CAPABILITIES

Feature flags negotiated by both sides.

Example capability names:

- `video.h264`
- `video.av1`
- `input.mouse`
- `input.touch`
- `input.keyboard`
- `input.accessibility`
- `input.ime`
- `clipboard.text`
- `audio.output`
- `notifications`
- `media.session`
- `files`
- `recording`

A capability must never be inferred from the Android version alone. The running device reports actual availability.

### INPUT

Normalized input events:

- mouse move
- mouse button
- wheel
- touch down/move/up
- key down/up
- text commit
- gesture

Coordinates should be expressed in a negotiated logical coordinate space so client resolution changes do not alter protocol semantics.

### CONTROL

Examples:

- pause/resume stream
- request keyframe
- set quality
- rotate
- clipboard get/set
- launch app
- media command
- screenshot
- start/stop recording

### VIDEO

Initial encoding target: H.264 elementary stream carried in framed packets.

Each packet needs enough metadata to recover from loss/reconnect, including at minimum:

- stream id
- frame id
- timestamp
- packet sequence
- keyframe flag
- payload length

Exact framing will be finalized after the first encoder/decoder prototype.

### HEARTBEAT

Both endpoints periodically send heartbeat messages. Missed heartbeats move the session into a reconnecting/degraded state rather than immediately destroying pairing state.

## 5. Error model

Errors are structured:

```json
{
  "type": "ERROR",
  "code": "UNSUPPORTED_CAPABILITY",
  "message": "...",
  "recoverable": true
}
```

Initial codes:

- `BAD_VERSION`
- `AUTH_FAILED`
- `PERMISSION_REQUIRED`
- `UNSUPPORTED_CAPABILITY`
- `CODEC_UNAVAILABLE`
- `STREAM_FAILED`
- `INPUT_UNAVAILABLE`
- `RATE_LIMITED`
- `INVALID_MESSAGE`
- `SESSION_EXPIRED`

## 6. Reconnection

Pairing identity should survive a temporary connection loss. Session keys/tokens must not be treated as permanent credentials.

After reconnect:

`CONNECT → AUTH → CAPABILITY_RENEGOTIATION → REQUEST_KEYFRAME → READY`

## 7. Protocol versioning

The first integer version is the major protocol version. Incompatible changes increment the major version. Optional fields and capabilities can be added without breaking older clients.

## 8. Future transport options

The protocol must not assume TCP forever. Candidate transports:

- TCP over local Wi-Fi — first implementation
- QUIC — later evaluation
- USB local transport — later evaluation

The protocol layer must remain independent of the transport implementation.
