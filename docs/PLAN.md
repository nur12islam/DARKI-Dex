# DARKI-Dex Master Plan

## 1. Mission

Build an Android-to-Android desktop environment in which:

- the **iQOO Z10x** is the host/computing device;
- the **Lenovo Tab 6 A101LV running LineageOS 15** is the desktop client;
- the tablet provides the desktop shell, windows, taskbar, launcher, keyboard/mouse UX and system controls;
- the phone provides Android application execution and host-side device capabilities.

The experience should target the breadth of features users expect from a DeX-style environment, inspired by Android-Dex, without copying its implementation.

## 2. Product principles

1. **Android-first:** no Windows/Linux dependency.
2. **No GApps dependency:** the target tablet must remain functional without Google Play Services.
3. **Feasibility before polish:** system capabilities are tested before UI work depends on them.
4. **Graceful fallback:** every high-risk feature needs a degraded mode where practical.
5. **Low latency:** interactive input and video take priority over maximum image quality.
6. **Secure by default:** no unauthenticated control endpoint on the LAN.
7. **Modular:** transport, protocol, capture, input and desktop UI must be replaceable independently.
8. **Measure instead of assume:** performance targets are validated on the actual Z10x/Tab 6 pair.

## 3. MVP definition

The first successful build is NOT a complete DeX clone. MVP means:

- host/client pair can discover or pair over Wi-Fi;
- user explicitly authorizes screen capture on the phone;
- phone screen is encoded and displayed on the tablet with usable latency;
- tablet keyboard/mouse input reaches the phone through an Android-supported mechanism;
- session survives normal temporary network interruptions;
- desktop shell can launch/control at least the initial supported interaction path;
- connection status and disconnect controls are visible.

If these gates are not stable, do not proceed to advanced window management.

## 4. Feature roadmap

### Phase 0 — Feasibility

- architecture
- protocol draft
- permissions model
- target Android APIs
- compatibility matrix
- risk register
- test plan

**Exit:** architecture reviewed and highest-risk assumptions identified.

### Phase 1 — Connection proof

- host app skeleton
- client app skeleton
- LAN discovery/manual pairing
- authenticated handshake
- device metadata exchange
- heartbeat
- clean disconnect/reconnect

**Exit:** two devices maintain a reliable DARKI session.

### Phase 2 — Display pipeline

- MediaProjection capture
- MediaCodec H.264 encoding
- packet framing/backpressure
- client decoder
- fullscreen rendering
- dynamic quality controls
- latency instrumentation

**Exit:** stable interactive stream on the target pair.

### Phase 3 — Input

- mouse movement
- clicks
- scroll
- touch/tap
- drag/swipe
- keyboard text entry
- navigation keys
- modifier keys

The implementation must distinguish capabilities that Android exposes directly from those requiring Accessibility, IME or privileged mechanisms.

**Exit:** normal tablet keyboard/mouse can operate the host reliably enough for desktop use.

### Phase 4 — Desktop shell

- wallpaper/workspace
- taskbar
- launcher/app drawer
- search
- status area
- connection indicator
- window containers
- minimize/maximize/fullscreen semantics
- recent apps
- settings

**Exit:** the tablet feels like a desktop instead of a remote viewer.

### Phase 5 — Integration

- clipboard
- notifications
- media controls
- file transfer
- file manager
- screenshots
- recording
- orientation handling
- power/performance profiles

**Exit:** core productivity workflow works end-to-end.

### Phase 6 — Advanced

- per-app/window streaming where technically possible
- stronger window management
- gaming mode
- key mapping
- controller support
- adaptive bitrate/resolution
- USB transport
- advanced discovery
- optional screen-off experiments

**Exit:** feature parity is evaluated feature-by-feature rather than claimed globally.

## 5. Success metrics

Initial targets, subject to measurement:

| Metric | Initial target |
|---|---|
| Pairing | < 10 seconds on same LAN |
| Stream startup | < 3 seconds after permission |
| Interactive latency | aim < 100 ms end-to-end |
| Baseline video | 720p/60 where hardware permits |
| Fallback video | 1080p/30 stable |
| Reconnect | automatic after transient LAN loss |
| Crash recovery | host/client can restart without corrupting pairing state |
| Security | authenticated session before control traffic |

Targets are engineering goals, not promises. Actual results will be recorded from the target devices.

## 6. Critical feasibility questions

These must be answered with prototypes:

1. Can the Z10x provide the required MediaProjection + MediaCodec pipeline continuously under load?
2. Can the Tab 6 decode the selected codec/profile smoothly on LineageOS 15?
3. What input injection can be achieved on an unrooted Z10x using Android-supported APIs?
4. Can an IME-based path provide acceptable keyboard/text behavior?
5. Can AccessibilityService provide enough gesture/global-action control for the intended UX?
6. Can Android expose notifications/media state reliably without Google services?
7. Can the desired multi-window experience be implemented without privileged access?
8. What happens when the host locks, rotates, sleeps, kills the app or revokes MediaProjection?
9. How much battery/thermal load is produced during sustained desktop sessions?
10. Which OriginOS restrictions interfere with background services, permissions or network sockets?

## 7. Fallback philosophy

If true per-app streaming is not feasible, use a hybrid desktop model rather than blocking the project:

- primary mode: one low-latency host display stream inside a desktop shell;
- enhanced mode: independent app/window streams when supported;
- input fallback: IME/accessibility-supported interactions;
- development fallback: ADB-assisted testing, never a production requirement;
- transport fallback: manual IP pairing if discovery fails;
- quality fallback: lower FPS/resolution/bitrate under congestion.

## 8. Explicit non-goals for MVP

- root requirement
- Google Play Services requirement
- copying proprietary Android-Dex source
- claiming universal Android compatibility
- perfect system-level key injection on every OEM
- true independent Android application processes on the tablet
- guaranteed screen-off remote control
- cloud relay as a first-release dependency

## 9. Development order

Always work from highest technical risk toward lower-risk polish:

**Protocol → connection → capture/encode → decode/render → input → shell → integration → advanced features → polish.**

Do not spend major effort on animations, themes or launcher polish while capture/input feasibility is unresolved.
