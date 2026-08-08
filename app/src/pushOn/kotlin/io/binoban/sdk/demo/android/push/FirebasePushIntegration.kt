package io.binoban.sdk.demo.android.push

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import io.binoban.sdk.core.Notification

/**
 * pushOn flavor [PushIntegration] backed by Firebase Cloud Messaging.
 *
 * Every Firebase call site is guarded: if the integrating developer selected the
 * pushOn variant without dropping in `google-services.json` (so FirebaseApp is
 * not initialized), these degrade to `null`/`false` instead of crashing — the
 * Push tab then surfaces the remediation steps.
 */
class FirebasePushIntegration(private val app: Application) : PushIntegration {

    override fun requestExistingToken(onResult: (String?) -> Unit) {
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        // Register/refresh with the SDK so it emits
                        // bb_notification_registered. Safe to call repeatedly.
                        Notification.refreshToken(token)
                        onResult(token)
                    } else {
                        onResult(null)
                    }
                }
        } catch (e: Exception) {
            // FirebaseApp not initialized (google-services.json missing) or
            // FirebaseMessaging unavailable — surface as "no token" in the UI.
            onResult(null)
        }
    }

    override fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
