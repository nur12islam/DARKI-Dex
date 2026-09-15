# DARKI-Dex Compatibility Matrix

## Target devices

| Role | Device | OS | Priority |
|---|---|---|---|
| Host | iQOO Z10x (I2404) | OriginOS / Android | Primary |
| Client | Lenovo Tab 6 (A101LV) | LineageOS 15 / Android 15 | Primary |

## Compatibility philosophy

The project will target Android versions rather than individual OEM skins, but the Z10x and A101LV are the release gates for the first usable version.

No feature should be marked supported merely because an API exists. It must pass an on-device test.

## Feature matrix

| Feature | Host requirement | Client requirement | MVP |
|---|---|---|---|
| Wi-Fi connection | network permission/socket | network permission/socket | Yes |
| Pairing | secure local storage | secure local storage | Yes |
| Screen capture | MediaProjection | decoder/render surface | Yes |
| H.264 encoding | hardware MediaCodec | H.264 decoder | Yes |
| Mouse | supported input path | mouse event capture | Yes |
| Keyboard | IME/input mechanism | physical keyboard capture | Yes |
| Touch | supported gesture/input path | touchpad/touch events | Yes |
| Clipboard | clipboard access rules | clipboard manager | Later |
| Notifications | notification listener/user grant | desktop notification UI | Later |
| Media controls | MediaSession | desktop controls | Later |
| File transfer | storage/file APIs | storage/file APIs | Later |
| Audio | supported capture/output path | audio playback | Later |
| Recording | capture/encoding pipeline | file/storage | Later |
| Independent app streams | unknown; requires prototype | window compositor | Experimental |
| Screen-off control | OEM/device dependent | client unaffected | Experimental |
| USB | Android device/USB transport feasibility | Android USB host/accessory feasibility | Later |

## Android API baseline

The first codebase should choose a baseline that comfortably supports MediaProjection, MediaCodec, foreground services and modern lifecycle APIs. The exact `minSdk` will be chosen during project bootstrap after checking the selected AndroidX versions and required APIs.

## OEM-specific concerns

### iQOO / OriginOS

Test explicitly for:

- background execution
- foreground service behavior
- battery optimization
- notification permissions
- network behavior while screen is off
- MediaProjection lifetime
- overlay/accessibility permissions
- process killing under memory pressure

### LineageOS 15 / Android 15

Test explicitly for:

- fullscreen/immersive behavior
- pointer/mouse APIs
- hardware decoder support
- keyboard event delivery
- notification UI behavior
- display density/scaling
- orientation
- storage permissions

## Emulator policy

Emulators may be used for fast UI/protocol tests, but they do not count as device-level proof for streaming performance, input injection, thermal behavior or OEM restrictions.
