package io.binoban.sdk.demo.android.push

import androidx.compose.runtime.mutableStateListOf

/**
 * Flavor-neutral contract for the push integration. The `pushOn` flavor backs it
 * with Firebase Cloud Messaging; the `pushOff` flavor returns a disabled stub.
 *
 * Kept free of Firebase types so the default (`pushOff`) build compiles with no
 * Firebase dependency on its classpath.
 */
interface PushIntegration {
    /**
     * Ask the platform for the current push token, register/refresh it with the
     * SDK via [io.binoban.sdk.core.Notification.refreshToken], and hand it back.
     * `null` when push is disabled or the token is unavailable.
     */
    fun requestExistingToken(onResult: (String?) -> Unit)

    /** Whether the runtime POST_NOTIFICATIONS permission is currently granted. */
    fun hasPermission(): Boolean
}

/**
 * Holder for the active-flavor [PushIntegration]. Assigned once from
 * [initPush] (a top-level function each flavor provides) in `Application.onCreate`.
 */
object PushIntegrations {
    lateinit var current: PushIntegration
}

/**
 * Compose-observable ring buffer of recent notification interactions, recorded
 * by the pushOn flavor's [LoggingInteractionHandler]. The UI reads from this to
 * show delivery / click / dismiss events as they happen.
 */
object PushInteractionLog {
    private const val MAX = 10
    val interactions = mutableStateListOf<String>()

    fun add(entry: String) {
        interactions.add(0, entry)
        while (interactions.size > MAX) interactions.removeAt(interactions.size - 1)
    }

    fun clear() = interactions.clear()
}
