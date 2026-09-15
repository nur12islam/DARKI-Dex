package com.darki.link

/** Capabilities advertised by a DARKI Link peer. */
enum class Capability(val id: String) {
    NOTIFICATION_READ("notification.read"),
    NOTIFICATION_DISMISS("notification.dismiss"),
    FILE_SEND("file.send"),
    FILE_RECEIVE("file.receive"),
    CLIPBOARD_SEND("clipboard.send"),
    CLIPBOARD_RECEIVE("clipboard.receive"),
    MEDIA_CONTROL("media.control"),
    VOLUME_CONTROL("volume.control"),
    SCREENSHOT("screenshot"),
    HOTSPOT_CONTROL("hotspot.control"),
    BRIGHTNESS_CONTROL("brightness.control"),
    DEVICE_STATUS("device.status")
}
