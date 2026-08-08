# Binoban Android SDK — Example App

A minimal Jetpack Compose reference app showing how to integrate the **Binoban SDK** into
an Android project.

Binoban is enterprise CDXP infrastructure for customer data, activation, retail media,
advertising, and decisioning. This example demonstrates the client-side event and
identity APIs you use to send customer signals into a Binoban deployment.

> **Status:** Reference example · tested against Binoban Android SDK **1.1.0**
> (`io.binoban.sdk:sdk-android`).

## Prerequisites

- Android Studio Meerkat or later
- **Binoban Android SDK:** Android **5.0 (API 21)+**. This example app is also configured
  with `minSdk = 21`, so it runs on any device the SDK supports.
- Your credentials from Binoban customer support:
  - **API Key** (`apiKey`)
  - **Source Identifier** (`sourceIdentifier`)
  - **API Host** (`apiHost`)

Contact **support@binoban.io** to obtain these values for your account.

## Setup

1. Open the project in Android Studio.
2. In `app/src/main/java/io/binoban/sdk/demo/android/MainApplication.kt`, replace the placeholder credentials:

```kotlin
binoban = Binoban("YOUR_API_KEY", "YOUR_SOURCE_IDENTIFIER") {
    application = app
    apiHost = "YOUR_API_HOST"
}
```

3. Run the app on a device or emulator.

## What the demo shows

| Feature | Description |
|---|---|
| **Track** | Send a named event with custom key-value properties |
| **Identify** | Identify a user by ID with custom traits |
| **Flush** | Immediately dispatch any buffered events to the server |
| **Reset** | Clear the current user identity and reset SDK state |
| **Push** | Opt-in Firebase Cloud Messaging integration — token registration, notification display, and interaction reporting (tap / dismiss / customData) |
| **Settings** | Toggle debug logging, enable/disable the SDK, adjust flush thresholds at runtime |
| **JSON display** | See the exact payload sent for each call |

## SDK integration pattern (Kotlin)

Add the dependency to your `build.gradle.kts`:

```kotlin
implementation("io.binoban.sdk:sdk-android:<version>")
```

**Application class**

```kotlin
import io.binoban.sdk.core.Binoban

class MyApplication : Application() {
    lateinit var binoban: Binoban

    override fun onCreate() {
        super.onCreate()
        binoban = Binoban("YOUR_API_KEY", "YOUR_SOURCE_IDENTIFIER") {
            application = this@MyApplication
            apiHost = "YOUR_API_HOST"
        }
    }
}
```

**Key methods**

```kotlin
// Track an event
binoban.track("purchase", buildJsonObject {
    put("item", "shoes")
    put("price", "49.99")
})

// Identify a user
binoban.identify("user-123", buildJsonObject {
    put("email", "user@example.com")
})

// Flush buffered events
binoban.flush()

// Reset user identity
binoban.reset()
```

## Java integration

The SDK is fully usable from Java. Add the dependency the same way; only the call syntax differs.

**Application class**

```java
import io.binoban.sdk.core.Binoban;
import io.binoban.sdk.core.BinobanKt;

public class MyApplication extends Application {
    private static Binoban binoban;

    public static Binoban getBinoban() { return binoban; }

    @Override
    public void onCreate() {
        super.onCreate();
        final Application app = this;
        binoban = BinobanKt.Binoban(
            "YOUR_API_KEY",
            "YOUR_SOURCE_IDENTIFIER",
            config -> {
                config.setApplication(app);
                config.setApiHost("YOUR_API_HOST");
                return kotlin.Unit.INSTANCE;
            }
        );
        Binoban.Companion.setDebugLogsEnabled(true);
    }
}
```

**Key method calls**

```java
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;
import java.util.HashMap;
import java.util.Map;

// Build properties map
Map<String, JsonElement> propsMap = new HashMap<>();
propsMap.put("item", new JsonPrimitive("shoes"));
propsMap.put("price", new JsonPrimitive("49.99"));
JsonObject props = new JsonObject(propsMap);

// Track an event
binoban.track("purchase", props);

// Identify a user
Map<String, JsonElement> traitsMap = new HashMap<>();
traitsMap.put("email", new JsonPrimitive("user@example.com"));
binoban.identify("user-123", new JsonObject(traitsMap));

// Flush buffered events
binoban.flush();

// Reset user identity
binoban.reset();
```

The credential placeholders (`YOUR_API_KEY`, `YOUR_SOURCE_IDENTIFIER`, `YOUR_API_HOST`) are obtained the same way as the Kotlin integration — contact **support@binoban.io**.

## Push notifications

Push runs on Firebase Cloud Messaging and is **opt-in**. The default build (the
`pushOff` flavor) has no Firebase dependency and no `google-services.json`, so it
builds and runs as-is — the **Push** tab shows the enable steps. Turning it on
requires your own Firebase project; Binoban does not wrap Firebase.

> The integration here mirrors the recipe in the public docs:
> [App push on Android](https://docs.binoban.io/developers/engage/mobile-push-android).
> That page is the source of truth — read it for the why behind each step.

### Enable push in this example

1. **Add your Firebase config.** Download `google-services.json` for the Android
   app (package `io.binoban.sdk.demo.android`) from your Firebase console and
   place it in `app/`. It is gitignored — never commit it.

   The build applies the `com.google.gms.google-services` plugin only when this
   file is present, so its presence is the single switch that turns Firebase on.

2. **Select the `pushOn` build variant** in Android Studio (Build → Select Build
   Variant) and run.

The pushOff variant remains the default; `./gradlew assembleDebug` still builds
it without any Firebase setup. Selecting `pushOn` without the config file also
builds, but Firebase cannot initialize at runtime and the Push tab reports the
token as unavailable.

### How the pushOn variant is wired

The push-specific code lives in `app/src/pushOn/` (`pushOff` holds a disabled
stub), so the default build never references Firebase types. The wiring follows
the [four documented steps](https://docs.binoban.io/developers/engage/mobile-push-android):

1. **Permission.** `POST_NOTIFICATIONS` is declared in the pushOn flavor's
   `app/src/pushOn/AndroidManifest.xml` (so the default build never requests it) and
   requested at runtime from the Push tab on Android 13+. Without it the SDK
   reports a `failed` interaction instead of displaying.
2. **Initialize in `Application.onCreate`.** `MainApplication` calls into the
   pushOn flavor's `initPush`, which calls:
   ```kotlin
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
   ```
   The channel is created at `IMPORTANCE_HIGH`. Use a fresh channel id if you
   already have one at lower importance (Android won't raise it later).
3. **Forward FCM callbacks** in `MyFirebaseMessagingService`:
   ```kotlin
   override fun onMessageReceived(message: RemoteMessage) {
       super.onMessageReceived(message)
       if (message.data["source"] == "binoban") {
           Notification.notify(message.data)
       }
   }

   override fun onNewToken(token: String) {
       super.onNewToken(token)
       Notification.refreshToken(token)
   }
   ```
   Binoban sends **data-only** messages, so your service always runs and
   `Notification.notify` is what displays the notification.
4. **Bootstrap the token you already have.** `onNewToken` only fires on token
   generation/rotation, so the Push tab fetches the existing FCM token once and
   hands it to `Notification.refreshToken` — covering already-installed devices.

A `DefaultNotificationInteractionHandler` subclass (`LoggingInteractionHandler`)
calls `super` (so SDK tracking and callbacks still fire) and mirrors each
interaction into the Push tab's "Recent interactions" list. For your own app,
route `interaction.uri` and `interaction.customData` through your navigation
instead of just logging — see
[Taps and dismissals](https://docs.binoban.io/developers/engage/mobile-push-android#taps-and-dismissals).

### Verify it worked

1. After launch, your next flushed batch carries a `bb_notification_registered`
   event with the token (tap **Re-fetch FCM token** in the Push tab to force it).
2. Send a test campaign from the Binoban panel. The notification appears,
   `bb_notification_delivered` follows, and tapping it produces
   `bb_notification_clicked` — the Push tab mirrors each interaction.

See [Push events](https://docs.binoban.io/developers/reference/events/push-events)
for the full event list and payload keys.

## Documentation

- Developer documentation: [docs.binoban.io](https://docs.binoban.io)
- Website: [binoban.io](https://binoban.io)

## Security

Please report security issues privately to **security@binoban.io**. See
[SECURITY.md](SECURITY.md). Do not open public issues for vulnerabilities.

## License

Released under the [MIT License](LICENSE), matching the Binoban Android SDK.
