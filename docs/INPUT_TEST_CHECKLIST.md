# DARKI-Dex Input Test Checklist

## Connection
- [ ] Host and client are on the same Wi-Fi network.
- [ ] Client discovers the iQOO Z10x host.
- [ ] Client connects and receives the video configuration.
- [ ] Screen stream remains stable while input is active.

## Pointer
- [ ] Tap reaches the expected screen coordinate.
- [ ] Scroll reaches the expected area.
- [ ] Primary button state is preserved on down/move/up.
- [ ] Secondary button state is distinguishable from primary.
- [ ] Cancel releases the tracked button state.

## Navigation
- [ ] Back returns to the previous Android screen.
- [ ] Home returns to the Android launcher.
- [ ] Recents opens the recent-apps screen.

## Keyboard
- [ ] Key-down packets are received by the host.
- [ ] Key-up packets are received by the host.
- [ ] Modifier state is preserved.
- [ ] Unicode character information is preserved.

## Known limitation
Android's AccessibilityService does not provide unrestricted system-wide injection of arbitrary hardware keyboard events. Keyboard input therefore needs a dedicated IME/text-input path before it can be considered production-ready.
