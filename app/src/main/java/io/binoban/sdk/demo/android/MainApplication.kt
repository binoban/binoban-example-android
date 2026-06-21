package io.binoban.sdk.demo.android

import android.app.Application
import io.binoban.sdk.core.Binoban

class MainApplication : Application() {
    companion object {
        lateinit var analytics: Binoban
    }

    override fun onCreate() {
        super.onCreate()
        val app = this
        analytics = Binoban("YOUR_API_KEY", "YOUR_SOURCE_IDENTIFIER") {
            application = app
            apiHost = "YOUR_API_HOST"
        }
        Binoban.debugLogsEnabled = true
    }
}
