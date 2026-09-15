package com.darki.link.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Receives local notifications after the user explicitly grants notification
 * listener access. Forwarding to a peer will be wired into the secure session.
 */
class DarkiNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Milestone 3: sanitize and send notification metadata to the paired peer.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Milestone 3: send notification removal event to the paired peer.
    }
}
