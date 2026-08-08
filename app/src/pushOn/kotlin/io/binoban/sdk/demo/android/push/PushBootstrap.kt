package io.binoban.sdk.demo.android.push

import android.app.Application
import com.google.firebase.FirebaseApp
import io.binoban.sdk.core.Notification
import io.binoban.sdk.core.platform.notifier.notification.NotificationInteractionManager
import io.binoban.sdk.core.platform.notifier.notification.configuration.NotificationPlatformConfiguration
import io.binoban.sdk.demo.android.R

/**
 * pushOn flavor wiring. Runs in `Application.onCreate` (called from
 * `MainApplication`) so the notification layer is ready before any push can
 * arrive — including in a cold FCM process. Mirrors the recipe in
 * https://docs.binoban.io/developers/engage/mobile-push-android
 */
fun initPush(app: Application) {
    // 0. Initialize Firebase once. A no-op when the google-services plugin has
    //    already auto-initialized it; returns null (rather than throwing) when no
    //    google-services.json was provided, which the Push tab reports as an
    //    unavailable token.
    FirebaseApp.initializeApp(app)

    // 1. Configure + initialize the SDK notification layer. Persisted, so a cold
    //    FCM process (no UI) can bootstrap from it.
    Notification.initialize(
        NotificationPlatformConfiguration.Android(
            notificationIconResId = R.drawable.ic_launcher_foreground,
            notificationChannelData = NotificationPlatformConfiguration.Android.NotificationChannelData(
                id = "binoban_engage",
                name = "Updates",
                description = "Binoban Engage push notifications"
            )
        )
    )

    // 2. Install the demo's interaction handler (keeps SDK tracking + logs to UI).
    NotificationInteractionManager.setHandler(LoggingInteractionHandler())

    // 3. Provide the flavor-backed Firebase integration to the UI.
    PushIntegrations.current = FirebasePushIntegration(app)
}
