# DARKI Link Protocol v1

## Purpose

DARKI Link uses a small, versioned, transport-independent protocol for secure peer communication between Android devices.

## Frame

Every message is a UTF-8 JSON object followed by a newline during the initial TCP implementation.

Required fields:

```json
{
  "version": 1,
  "type": "ping",
  "id": "unique-request-id",
  "timestamp": 0,
  "payload": {}
}
```

`timestamp` is informational; authentication/session freshness must not rely on device clocks alone.

## Message types

### Session

- `hello`
- `auth`
- `ping`
- `pong`
- `goodbye`

### Device

- `device.status`
- `device.capabilities`

### Commands

- `command`
- `command.result`

### Notifications

- `notification`
- `notification.remove`

### Files

- `file.offer`
- `file.accept`
- `file.chunk`
- `file.complete`
- `file.cancel`

### Integration

- `clipboard`
- `media.state`
- `media.command`
- `screenshot.offer`

## Request/response

Requests have an `id`. A result must echo that ID.

```json
{
  "version": 1,
  "type": "command",
  "id": "8f21",
  "timestamp": 0,
  "payload": {
    "name": "volume.set",
    "arguments": {"level": 5}
  }
}
```

```json
{
  "version": 1,
  "type": "command.result",
  "id": "8f21",
  "timestamp": 0,
  "payload": {
    "success": true
  }
}
```

Failure example:

```json
{
  "version": 1,
  "type": "command.result",
  "id": "8f21",
  "timestamp": 0,
  "payload": {
    "success": false,
    "reason": "SYSTEM_PERMISSION_REQUIRED"
  }
}
```

## Capability identifiers

Capabilities are strings and may be added without breaking older peers.

Initial identifiers:

- `notification.read`
- `notification.dismiss`
- `file.send`
- `file.receive`
- `clipboard.send`
- `clipboard.receive`
- `media.control`
- `volume.control`
- `screenshot`
- `hotspot.control`
- `brightness.control`
- `device.status`

A peer must not infer a capability that was not advertised.

## Command result reasons

Recommended stable reasons:

- `UNSUPPORTED`
- `CAPABILITY_NOT_ADVERTISED`
- `SYSTEM_PERMISSION_REQUIRED`
- `USER_ACTION_REQUIRED`
- `DEVICE_BUSY`
- `INVALID_ARGUMENT`
- `TIMEOUT`
- `NOT_CONNECTED`
- `INTERNAL_ERROR`

## File transfer

The initial design uses metadata followed by bounded chunks.

Offer:

```json
{
  "version": 1,
  "type": "file.offer",
  "id": "file-123",
  "timestamp": 0,
  "payload": {
    "name": "notes.pdf",
    "size": 123456,
    "mime": "application/pdf",
    "sha256": "..."
  }
}
```

The receiver responds with `file.accept` or `file.cancel`. Chunks must carry an offset/sequence so interrupted transfers can later support resume.

## Security

Transport discovery is not authentication. A device must authenticate before processing control traffic.

The production implementation must establish an encrypted session using the persistent identities created during pairing. Sensitive payloads must never be sent in plaintext merely because both devices are on the same Wi-Fi network.

## Compatibility

Unknown message types must be safely ignored or rejected with a protocol error. Unknown payload fields should be ignored unless the message definition marks them mandatory.

Protocol version changes must be explicit. Additive fields and message types should remain compatible with older peers wherever practical.
