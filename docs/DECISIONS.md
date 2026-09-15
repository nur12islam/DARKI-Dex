# DARKI-Dex Architecture Decisions

## ADR-001 — Android-to-Android architecture

**Decision:** Build two Android components: Host on the phone and Desktop Client on the tablet.

**Reason:** The target experience is Android-to-Android and the tablet should provide the desktop UX while the phone supplies application execution and device capabilities.

## ADR-002 — Do not make ADB a production dependency

**Decision:** ADB may be used for development diagnostics, but normal operation must use the DARKI protocol and Android-supported APIs.

**Reason:** A permanent ADB requirement would make the product fragile, less user-friendly and overly dependent on developer tooling.

## ADR-003 — Own protocol

**Decision:** DARKI-Dex uses a versioned DARKI protocol rather than copying an internal third-party protocol.

**Reason:** It keeps the architecture controllable and lets us model Android-to-Android capabilities directly.

## ADR-004 — Whole-display stream before true per-app streams

**Decision:** MVP starts with a whole-display stream. Independent app/window streams are an advanced capability.

**Reason:** Independent Android app streaming is one of the highest-risk parts of the project. A whole-display path gives us a usable foundation without making the entire project depend on it.

## ADR-005 — Capability negotiation

**Decision:** Every optional system capability is negotiated at runtime.

**Reason:** Android version, OEM and LineageOS differences mean static assumptions are unsafe.

## ADR-006 — Local-first transport

**Decision:** Start with same-LAN Wi-Fi. USB and remote/cloud transports come later.

**Reason:** Local Wi-Fi minimizes infrastructure and latency while matching the primary use case.

## ADR-007 — Security before control

**Decision:** No control messages are accepted before authentication/pairing.

**Reason:** DARKI-Dex can transmit screen content and input events, so an exposed unauthenticated control endpoint would be unacceptable.

## ADR-008 — Platform APIs first

**Decision:** Prefer Android platform APIs for capture, encoding, media, notifications and storage; add external dependencies only when justified.

**Reason:** The project must remain compatible with a GApps-free LineageOS client and minimize dependency risk.
