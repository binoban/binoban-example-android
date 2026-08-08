package io.binoban.sdk.demo.android.push

import android.app.Application

/**
 * pushOff flavor wiring — push is disabled. No Firebase types are referenced, so
 * the default build compiles with no Firebase dependency on its classpath. The
 * Push tab shows the opt-in instructions instead of attempting any FCM call.
 *
 * [app] is unused here but keeps the signature identical to the pushOn flavor's
 * `initPush`, which is what lets `MainApplication` call it without knowing the flavor.
 */
@Suppress("UNUSED_PARAMETER")
fun initPush(app: Application) {
    PushIntegrations.current = object : PushIntegration {
        override fun requestExistingToken(onResult: (String?) -> Unit) {
            onResult(null)
        }

        override fun hasPermission(): Boolean = false
    }
}
