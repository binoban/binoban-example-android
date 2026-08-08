package io.binoban.sdk.demo.android

import android.app.Application
import io.binoban.sdk.core.Binoban
import io.binoban.sdk.demo.android.push.initPush

class MainApplication : Application() {
    companion object {
        lateinit var binoban: Binoban
    }

    override fun onCreate() {
        super.onCreate()
        val app = this
        binoban = Binoban("YOUR_API_KEY", "YOUR_SOURCE_IDENTIFIER") {
            application = app
            apiHost = "YOUR_API_HOST"
        }
        Binoban.debugLogsEnabled = true

        // Flavor-resolved push wiring. pushOff (default) installs a disabled stub;
        // pushOn initializes the SDK notification layer + Firebase-backed integration.
        // Resolved at compile time to the matching flavor's top-level `initPush`.
        initPush(app)
    }
}
