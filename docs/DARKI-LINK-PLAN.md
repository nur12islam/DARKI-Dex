# DARKI Link — Ecosystem Plan

DARKI Link repurposes the DARKI-Dex repository into a private Android-to-Android personal ecosystem for the iQOO Z10x and Lenovo Tab 6 A101LV.

## Mission

Build one app that can be installed on both devices so they can cooperate as peers rather than treating one device as a permanent master.

The first target pair is:

- iQOO Z10x (I2404)
- Lenovo Tab 6 A101LV running LineageOS 15 without GApps

The app should provide secure device pairing, bidirectional commands, notification synchronization, instant file transfer, clipboard sharing, media control, device status, screenshots and a capability-aware control surface.

## Core principles

1. **Peer-to-peer:** either device can initiate an action.
2. **No GApps dependency:** the core must work on the LineageOS tablet without Google Play Services.
3. **Capabilities before UI:** only expose controls the current device can actually perform.
4. **Secure by default:** no unauthenticated LAN command server.
5. **Transport-agnostic:** LAN first, Wi-Fi Direct and Bluetooth as fallbacks, Internet/relay later.
6. **Graceful failure:** when Android blocks an operation, report the reason and provide the supported user-action fallback instead of pretending it worked.
7. **Measure on the real pair:** compatibility is established on the Z10x/Tab 6 before broad Android claims.

## Architecture

```text
                         DARKI LINK
                             │
              ┌──────────────┴──────────────┐
              │                             │
          App UI                        Core Engine
              │                             │
              │          ┌──────────────────┼──────────────────┐
              │          │                  │                  │
              │       Pairing          Capability         Connection
              │        Engine             Engine             Manager
              │          │                  │                  │
              │          └──────────────────┴──────────────────┘
              │                             │
              └────────────────────── DARKI Protocol ────────────
                                            │
                         ┌──────────────────┼─────────────────┐
                         │                  │                 │
                        LAN             Wi-Fi Direct      Bluetooth
                         │                  │                 │
                         └──────────────────┴─────────────────┘
                                            │
                           ┌────────────────┴────────────────┐
                           │                                 │
                        iQOO Z10x                       Lenovo Tab 6
                           │                                 │
                     Local services                    Local services
```

## Transport order

### 1. LAN / same Wi-Fi

Primary transport for the MVP. Use a long-lived authenticated TCP connection first because implementation and debugging are straightforward.

### 2. Wi-Fi Direct

Use `WifiP2pManager` for device-to-device communication when the devices are not on the same LAN. File transfers should prefer this path when available.

### 3. Bluetooth

Use Android 12+ Bluetooth permissions and a small control/data channel as a fallback. Bluetooth is primarily for discovery, pairing and lightweight control; large files should prefer Wi-Fi.

### 4. Internet / relay

Future milestone only. Do not make cloud infrastructure a dependency of the core product.

## Pairing and security

First-run pairing should require explicit consent on both devices.

Preferred flow:

1. Device A opens **Pair device**.
2. Device A displays a QR code and short numeric code.
3. Device B scans/enters the code.
4. Both users confirm the device identity.
5. The devices exchange persistent public keys and device metadata.
6. A secure authenticated session is established.
7. The pairing is stored locally for future reconnects.

The protocol must reject control requests from unknown device identities.

## DARKI Protocol draft

Messages should be framed, versioned and request/response capable.

Initial message types:

- `PAIR`
- `AUTH`
- `PING`
- `DEVICE_STATUS`
- `DEVICE_CAPABILITIES`
- `COMMAND`
- `COMMAND_RESULT`
- `NOTIFICATION`
- `NOTIFICATION_REMOVE`
- `FILE_OFFER`
- `FILE_ACCEPT`
- `FILE_CHUNK`
- `FILE_COMPLETE`
- `CLIPBOARD`
- `MEDIA_STATE`
- `MEDIA_COMMAND`
- `SCREENSHOT`

Example request:

```json
{
  "version": 1,
  "type": "command",
  "id": "8f21",
  "command": "hotspot.enable"
}
```

Example failure:

```json
{
  "version": 1,
  "type": "command_result",
  "id": "8f21",
  "success": false,
  "reason": "SYSTEM_PERMISSION_REQUIRED"
}
```

## Capability model

Every device advertises capabilities at connection time.

Example:

```text
✓ notification.read
✓ notification.dismiss
✓ file.send
✓ file.receive
✓ clipboard.send
✓ clipboard.receive
✓ media.control
✓ volume.control
✓ screenshot
? hotspot.control
? brightness.control
✗ arbitrary.system.toggle
```

The UI must derive controls from this model rather than hard-coding assumptions about every Android build.

## Feature roadmap

### Milestone 0 — Foundation

- convert project documentation from DARKI-Dex to DARKI Link
- shared protocol module
- app skeleton that can run on both devices
- persistent device identity
- capability model
- basic diagnostics screen

**Exit gate:** both devices build and launch the same app architecture without GApps.

### Milestone 1 — Secure connection

- LAN discovery/manual IP pairing
- QR/numeric pairing
- authenticated handshake
- encrypted session
- heartbeat
- reconnect after transient network loss
- device metadata exchange

**Exit gate:** two real devices maintain a stable authenticated session.

### Milestone 2 — Bidirectional device control

- command request/response
- device status
- volume where supported
- media control
- screenshot request
- supported system settings shortcuts
- capability-aware fallbacks

### Milestone 3 — Notification sync

- `NotificationListenerService` on both devices
- notification metadata/content forwarding
- notification removal synchronization
- explicit permission setup screen
- safe sanitization controls

### Milestone 4 — File transfer

- file picker
- offer/accept workflow
- chunked streaming
- progress
- checksum
- cancel/reject
- resume support
- LAN optimization

### Milestone 5 — Clipboard + richer integration

- bidirectional clipboard transfer
- background/foreground restriction handling
- media state synchronization
- richer device dashboard

### Milestone 6 — Wi-Fi Direct + Bluetooth fallback

- peer discovery
- direct connection
- transport selection
- automatic fallback/reconnect

### Milestone 7 — Advanced ecosystem

- Internet/relay mode
- device finder/ring
- notification actions/replies where Android permits
- remote camera
- presentation mode
- shared gallery
- advanced automation

## Important Android constraints

The command transport does not imply unrestricted OS control.

In particular, silently enabling a phone's normal Internet-sharing hotspot from another third-party Android app may be blocked by modern Android/OEM restrictions. The app should therefore classify such operations as one of:

- direct capability available;
- user action required via system UI/settings;
- unsupported on this device.

Android's `startLocalOnlyHotspot()` should not be presented as equivalent to enabling the phone's cellular Internet hotspot.

Notification sync also requires the user to manually grant notification-listener access. The app cannot silently grant that permission.

Clipboard behavior may be constrained by Android foreground/background clipboard rules, so synchronization must be capability- and state-aware.

## Security requirements

- no unauthenticated command endpoint
- persistent device identity
- encrypted session traffic
- explicit first-time pairing approval
- request IDs for replay-safe request/response matching
- capability validation before executing commands
- bounded file-transfer sizes and timeouts
- checksum verification
- clean disconnect and key revocation support

## MVP success definition

The MVP is successful when the real Z10x and Tab 6 can:

1. discover or manually connect;
2. pair with explicit approval;
3. authenticate and encrypt the session;
4. reconnect after temporary network loss;
5. exchange battery/network/device status;
6. execute a basic supported command in either direction;
7. accurately report unsupported or permission-gated commands.

Only after this gate should notification sync and file transfer become release-blocking work.

## Development order

**Protocol → pairing/security → LAN connection → capability exchange → command/status → notifications → file transfer → clipboard/media → Wi-Fi Direct → Bluetooth → Internet/advanced controls → polish.**

Do not spend major implementation effort on visual polish before the connection and capability foundations are proven on the target devices.
