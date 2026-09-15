# DARKI-Dex Risk Register

| Risk | Severity | Likelihood | Mitigation / fallback |
|---|---:|---:|---|
| Android input injection is insufficient without root/ADB | Critical | High | Prototype Accessibility + IME first; define degraded interaction mode |
| OriginOS kills host service/background work | High | Medium | foreground service, user guidance, battery-optimization checks, reconnect logic |
| MediaProjection stops after lifecycle changes | High | Medium | explicit state machine, permission re-request, keyframe/restart path |
| H.264 encode load drains battery/causes heat | High | Medium | hardware codec, adaptive FPS/bitrate, performance profiles |
| Tab decoder cannot sustain target resolution/FPS | High | Medium | lower resolution/FPS, hardware codec capability query |
| Wi-Fi latency/packet loss makes input unpleasant | High | Medium | local-only first, bounded queues, adaptive quality, diagnostics |
| Android OEM differences break APIs | High | High | capability negotiation and device matrix |
| LineageOS behavior differs from stock Android | Medium | Medium | test against actual A101LV rather than emulator assumptions |
| Per-app independent streaming proves infeasible | Critical | High | hybrid whole-display + client-side window model |
| Audio capture is restricted or inconsistent | Medium | Medium | make audio optional; media-control-only fallback |
| Screen-off control is unreliable | High | Medium | keep screen-on as default; treat screen-off as experimental |
| Notification access requires user setup | Medium | Medium | explicit permission flow; feature remains optional |
| Accessibility restrictions limit gestures | High | Medium | use supported gesture subset and show capability state |
| Keyboard text injection differs by app | High | Medium | IME path for text; raw key path where available |
| Network endpoint exposed to LAN | Critical | Medium | authenticated pairing + encrypted session |
| Session reconnect creates stale state | Medium | Medium | session IDs, heartbeat, state reset and keyframe request |
| File transfer consumes excessive storage | Medium | Low | chunking, free-space checks, cancel/resume |
| Thermal throttling appears during long sessions | High | Medium | telemetry, quality profiles, warning thresholds |
| Desktop UI becomes too ambitious | Medium | High | ship in layers; MVP uses one stream before true windows |
| Dependency bloat complicates Android compatibility | Medium | Medium | platform APIs first, minimal third-party libraries |

## Highest-risk rule

The project is not considered technically validated until **input control** and **stable low-latency streaming** have both been demonstrated on the actual Z10x → Tab 6 pair.

## Known uncertainty

Android's security model intentionally limits arbitrary system-level input injection for ordinary applications. DARKI-Dex therefore treats input as a capability to negotiate, not an unconditional promise.

ADB can be a useful development/debugging path, but production architecture must not depend on an always-connected ADB channel.
