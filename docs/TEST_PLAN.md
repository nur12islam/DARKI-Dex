# DARKI-Dex Test Plan

## 1. Connection tests

- host and client on same Wi-Fi
- manual IP pairing
- repeated connect/disconnect
- Wi-Fi off/on during session
- router/AP change
- duplicate clients
- invalid pairing/authentication
- stale session recovery

## 2. Streaming tests

Measure:

- startup time
- average FPS
- dropped frames
- encode time
- decode time
- end-to-end latency
- bitrate
- CPU usage
- battery drain
- thermal behavior

Test at:

- 720p/30
- 720p/60 where available
- 1080p/30
- reduced bitrate/congested network

## 3. Input tests

### Mouse

- move
- left/right click
- double click
- drag
- wheel
- fast movement

### Keyboard

- letters/numbers
- Shift/Ctrl/Alt combinations
- Enter/Escape/Tab
- Backspace/Delete
- arrows/Home/End
- function keys where supported
- long press/repeat
- text entry into multiple Android apps

### Touch/gesture

- tap
- long press
- swipe
- drag
- two-finger scroll
- pinch where supported
- back/home/recent actions where supported

## 4. Lifecycle tests

- host screen lock
- host unlock
- host rotation
- client rotation
- app background/foreground
- process restart
- MediaProjection revocation
- permission denial
- low-memory pressure
- battery saver

## 5. Desktop shell tests

- launcher opens
- app search
- taskbar state
- window move
- resize
- minimize/maximize
- fullscreen
- recent applications
- keyboard focus
- pointer focus
- disconnect/reconnect state

## 6. Integration tests

Later phases:

- clipboard text
- notification delivery
- media play/pause/next/previous
- file transfer integrity
- screenshot
- recording start/stop
- audio synchronization

## 7. Network fault tests

Introduce controlled:

- latency
- packet loss
- bandwidth limits
- temporary disconnect

The system should degrade quality before becoming unusable and should recover without requiring manual re-pairing.

## 8. Security tests

- unauthenticated client rejected
- wrong pairing code rejected
- expired session rejected
- revoked client rejected
- control messages rejected before authentication
- malformed message does not crash host/client
- no sensitive payloads written to ordinary logs

## 9. Acceptance gate for MVP

The MVP is accepted only when the actual Z10x → A101LV pair can:

1. pair securely;
2. establish a session;
3. stream the phone display;
4. maintain usable interactive latency;
5. receive at least the supported keyboard/mouse input subset;
6. recover from a short Wi-Fi interruption;
7. expose a clear disconnect/revoke action.
