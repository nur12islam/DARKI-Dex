# DARKI-Dex Architecture

## 1. System topology

```text
                 ┌────────────────────────────┐
                 │        iQOO Z10x           │
                 │          HOST              │
                 │                            │
                 │ Host Service               │
                 │ ├─ Session Manager         │
                 │ ├─ Screen Capture          │
                 │ ├─ H.264 Encoder           │
                 │ ├─ Input Controller        │
                 │ ├─ Clipboard Bridge        │
                 │ ├─ Media/Notification Hub  │
                 │ └─ Transport               │
                 └─────────────┬──────────────┘
                               │
                        DARKI Protocol
                         Wi-Fi / future USB
                               │
                 ┌─────────────┴──────────────┐
                 │       Lenovo Tab 6         │
                 │        CLIENT              │
                 │        LineageOS           │
                 │                            │
                 │ Desktop Shell              │
                 │ ├─ Workspace               │
                 │ ├─ Window Manager          │
                 │ ├─ Taskbar                 │
                 │ ├─ Launcher/Search         │
                 │ ├─ Notification Center     │
                 │ └─ Settings                │
                 │                            │
                 │ Input Adapter              │
                 │ ├─ Keyboard                │
                 │ ├─ Mouse                   │
                 │ ├─ Touch                   │
                 │ └─ Gestures                │
                 │                            │
                 │ Stream Decoder/Renderer     │
                 └────────────────────────────┘
```

## 2. Host

The host owns Android application execution and privileged-by-Android capabilities available to the user-authorized application.

### Host components

**Session Manager**
- pairing state
- authentication
- capability negotiation
- connection lifecycle
- heartbeat
- reconnect

**Capture Engine**
- MediaProjection lifecycle
- VirtualDisplay/Surface
- frame acquisition
- orientation changes
- resolution changes

**Encoder**
- MediaCodec
- H.264 first
- bitrate/FPS/keyframe controls
- congestion adaptation

**Input Controller**
- receives normalized DARKI input events
- maps them to the strongest available Android mechanism
- reports unsupported capabilities rather than pretending success

Potential mechanisms include AccessibilityService, IME/InputConnection and other Android-supported APIs. ADB may be used during development diagnostics but is not a production dependency.

**Integration Hub**
- clipboard
- MediaSession/media metadata
- notifications
- files
- device state

## 3. Client

The tablet is the user-facing desktop environment.

### Desktop Shell

The shell should be a normal Android application running fullscreen/immersive where permitted. It should not require replacing the LineageOS launcher in the first release.

Responsibilities:

- draw desktop background
- manage virtual desktop windows
- render stream surfaces
- taskbar and launcher
- keyboard/mouse capture
- connection status
- settings and diagnostics

## 4. Window model

DARKI-Dex will distinguish three concepts:

1. **Host Display Stream** — the complete phone display.
2. **Desktop Window** — a client-side container positioned by the tablet shell.
3. **App Stream** — a future capability representing an independently captured app/window.

MVP can use the host display stream inside one desktop window. Advanced releases may introduce independent app streams if Android permissions and lifecycle behavior permit them.

This prevents the entire project from depending on an unproven per-app capture mechanism.

## 5. Transport layers

The protocol is logically split into:

- control/session channel
- input channel
- video channel
- optional audio channel
- optional bulk/file channel

The physical transport can initially be a local TCP connection over Wi-Fi. The protocol should keep transport-specific details behind an interface so USB or another low-latency transport can be added later.

## 6. Threading model

Avoid blocking the UI thread.

Recommended boundaries:

- UI/main thread: rendering and user interaction
- capture thread/coroutine: MediaProjection pipeline
- codec worker: MediaCodec queue/dequeue
- network workers: read/write and backpressure
- control/session coroutine: lifecycle and state machine
- file worker: chunked transfer/checksum

Use structured concurrency and explicit cancellation when a session ends.

## 7. Lifecycle

### Connect

`DISCOVER → PAIR → AUTHENTICATE → NEGOTIATE → READY`

### Stream

`READY → CAPTURE_START → STREAMING`

### Temporary failure

`STREAMING → DEGRADED → RECONNECTING → READY`

### Permanent failure

`RECONNECTING → DISCONNECTED`

### Host permission loss

`STREAMING → PERMISSION_REQUIRED → READY`

## 8. Security boundary

The host must treat the tablet as an untrusted network peer until pairing/authentication succeeds.

Requirements:

- explicit pairing
- authenticated session
- session-specific credentials/keys
- no anonymous control
- visible active-client state
- disconnect/revoke capability
- no sensitive payload logging by default

## 9. Performance architecture

Interactive streaming should favor low latency over buffering.

Required mechanisms:

- bounded queues
- backpressure
- keyframe requests after recovery
- adaptive bitrate
- frame dropping when behind rather than unbounded buffering
- network/decoder telemetry

## 10. Dependency policy

Keep the foundation small. Prefer Android platform APIs for MediaProjection, MediaCodec, MediaSession, notifications and storage. External libraries should be introduced only when they solve a demonstrated problem.
