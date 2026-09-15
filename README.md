# DARKI-Dex

**Android-to-Android desktop environment for turning an Android tablet into a DeX-like workspace powered by an Android phone.**

> Status: Architecture / feasibility phase — implementation has not started yet.

## Vision

DARKI-Dex uses the **iQOO Z10x as the host/computing device** and the **Lenovo Tab 6 (A101LV, LineageOS 15) as the desktop client**. The tablet should feel like a real desktop environment rather than a simple mirrored-screen viewer.

The target experience is inspired by the feature set of [Android-Dex](https://github.com/Shrey113/Android-Dex), while DARKI-Dex will use its own Android-to-Android architecture and protocol rather than copying its closed-source implementation.

## Target experience

- Desktop workspace and wallpaper
- Taskbar, launcher and app search
- Window-style app containers
- Keyboard and mouse control
- Touch, swipe and scroll input
- Android app launching/control
- Notifications and media controls
- Clipboard synchronization
- File manager and file transfer
- Screenshots and screen recording
- Gaming controls and key mapping
- Wi-Fi pairing and reconnect
- Performance/quality controls
- Dark/light desktop themes

## Architecture

```text
                 DARKI-Dex session
                       │
          ┌────────────┴────────────┐
          │                         │
     iQOO Z10x                 Lenovo Tab 6
       HOST                       CLIENT
          │                         │
  Screen / Apps              Desktop Shell
  Media / Audio              Windows / Taskbar
  Device State               Keyboard / Mouse
  Input Endpoint             Notifications
          │                         │
          └──── DARKI Protocol ─────┘
                 Wi-Fi / USB
```

## Important principle

DARKI-Dex will not assume that Android permits a feature merely because a desktop operating system does. Each system-level capability will be tested on the target devices before the UI is built around it.

## Documentation

- [`docs/PLAN.md`](docs/PLAN.md) — roadmap, scope and success gates
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — system architecture
- [`docs/PROTOCOL.md`](docs/PROTOCOL.md) — protocol draft
- [`docs/RISKS.md`](docs/RISKS.md) — risks, blockers and fallback strategies
- [`docs/COMPATIBILITY.md`](docs/COMPATIBILITY.md) — target-device compatibility matrix
- [`docs/TEST_PLAN.md`](docs/TEST_PLAN.md) — validation and performance testing
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — architectural decisions

## Current milestone

**Milestone 0: Architecture and feasibility.**

No production feature should be implemented until the feasibility plan and highest-risk assumptions are validated.
