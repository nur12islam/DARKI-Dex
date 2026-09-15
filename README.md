# DARKI Link

**Private Android-to-Android personal ecosystem for connecting your devices.**

> Status: Milestone 0 — foundation implementation.

DARKI Link is the new direction of this repository. The original DARKI-Dex screen-streaming experiment has been retired; the repository is now being developed as a peer-to-peer device ecosystem.

## Target devices

- **iQOO Z10x (I2404)**
- **Lenovo Tab 6 (A101LV)** running LineageOS 15 without Google Play Services

The same app is installed on both devices. Neither device is permanently the master.

## Vision

Use either device to interact with the other without physically touching it:

- secure device pairing
- bidirectional device control
- notification synchronization
- instant file sharing
- clipboard synchronization
- media control
- device status
- screenshots
- device finder/ring
- Wi-Fi Direct and Bluetooth fallback
- optional Internet/relay mode later

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
              └──────────┴──────────────────┴──────────────────┘
                                            │
                                    DARKI Protocol v1
                                            │
                         ┌──────────────────┼─────────────────┐
                         │                  │                 │
                        LAN             Wi-Fi Direct      Bluetooth
                         │                  │                 │
                         └──────────────────┴─────────────────┘
```

## Development order

**Protocol → pairing/security → LAN connection → capability exchange → command/status → notifications → file transfer → clipboard/media → Wi-Fi Direct → Bluetooth → Internet/advanced controls → polish.**

## Android reality

DARKI Link does not assume that an Android API exists simply because a desktop OS can perform the action. Some controls require explicit user permissions or system UI, and some are unavailable to ordinary third-party apps on particular OEM builds.

For example, a remote request to enable a phone's normal Internet-sharing hotspot may be permission-gated or unsupported. The app will report that accurately and provide a supported fallback instead of pretending the command succeeded.

The core product does not require Google Play Services.

## Documentation

- [`docs/DARKI-LINK-PLAN.md`](docs/DARKI-LINK-PLAN.md) — product roadmap and feasibility gates
- [`docs/PROTOCOL-V1.md`](docs/PROTOCOL-V1.md) — protocol draft
- [`docs/PLAN.md`](docs/PLAN.md) — legacy DARKI-Dex plan retained for historical context

## Current milestone

**Milestone 0 — Foundation**

The current code establishes the single-app Android structure, persistent device identity, protocol primitives, capability model, initial LAN transport and notification-listener foundation. Pairing and encrypted communication are the next gate.
