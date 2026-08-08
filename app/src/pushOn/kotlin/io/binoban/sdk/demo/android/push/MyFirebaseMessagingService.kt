package io.binoban.sdk.demo.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.binoban.sdk.core.Notification

/**
 * Receives FCM messages and routes Binoban ones into the SDK. Binoban sends
 * **data-only** messages, so this service always runs (FCM's automatic display
 * never fires) — `Notification.notify` is what renders the notification and
 * reports delivery, taps, and dismissals.
 *
 * Every Binoban payload carries `source: "binoban"`. The branch keeps your own
 * (non-Binoban) pushes from falling into `Notification.notify` too — though
 * `Notification.notify` already drops anything whose source is not `"binoban"`,
 * so calling it unconditionally is still safe.
 *
 * Mirrors the recipe in https://docs.binoban.io/developers/engage/mobile-push-android
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data["source"] == "binoban") {
            Notification.notify(message.data)
        }
        // else: your own push handling, if any
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Works even with no live SDK instance — cached and replayed on next launch.
        Notification.refreshToken(token)
    }
}
