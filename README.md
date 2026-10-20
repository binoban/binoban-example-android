<img src="https://raw.githubusercontent.com/binoban/.github/63ddb277b487b407613e98205a324d4fc6b1019c/profile/binoban-star-indigo-on-white.png" alt="Binoban indigo star" width="72" height="72">

# Binoban Android Reference App

Explore customer event tracking, user identification, and optional push integration in a native Android app built with Jetpack Compose.

[Example app guide](https://docs.binoban.io/developers/reference/example-apps) · [Android SDK guide](https://docs.binoban.io/developers/reference/sdks/native-sdk-android) · [Push guide](https://docs.binoban.io/developers/engage/mobile-push-android)

## What this repository provides

A reference app that consumes `io.binoban.sdk:sdk-android:1.3.0`. Use it to inspect client calls and configuration, then apply the integration pattern to your own app.

## Requirements

| Component | Requirement |
| --- | --- |
| IDE | Android Studio Meerkat or later |
| Default `pushOff` variant | Android API 21+ |
| Optional `pushOn` variant | Android API 23+; Firebase Cloud Messaging 25.x |
| Build configuration | Compile/target SDK 35; JVM target 11 |
| Configuration | Source API key, source identifier, and deployment API host |

## Get started

### 1. Open the project

Clone this repository and open it in Android Studio. Let Gradle resolve the checked-in dependencies.

### 2. Configure your deployment

Request your API key, source identifier, and API host from [tech@binoban.io](mailto:tech@binoban.io). Replace the placeholders in [MainApplication.kt](app/src/main/java/io/binoban/sdk/demo/android/MainApplication.kt):

```kotlin
binoban = Binoban("YOUR_API_KEY", "YOUR_SOURCE_IDENTIFIER") {
    application = app
    apiHost = "YOUR_API_HOST"
}
```

### 3. Run the app

Select `pushOffDebug` in Android Studio and run on a device or emulator. To install it from the repository root:

```sh
./gradlew :app:installPushOffDebug
```

Firebase setup is optional. The default variant contains no Firebase dependency.

## Explore the example

| Feature | What you can explore |
| --- | --- |
| Track | Named events with custom properties |
| Identify | User IDs and custom traits |
| Flush | Request delivery of buffered events |
| Reset | Clear the current identity and reset SDK state |
| Settings | Debug logs, SDK enablement, and flush thresholds |
| JSON display | Inspect the inputs for each call |
| Push | Optional FCM token registration, display, and interaction reporting |

## Verify your integration

Use **Track** to send an event, then **Flush** to request delivery. Check the SDK logs and confirm event acceptance using the [Developer Quick Start](https://docs.binoban.io/developers/quickstart). The JSON display shows call inputs; it is not a delivery receipt.

If no event arrives, check the source credentials and `apiHost`. If push is unavailable, confirm the selected variant and Firebase configuration described below.

## SDK integration pattern (Kotlin)

Add the dependency to your `build.gradle.kts`:

```kotlin
implementation("io.binoban.sdk:sdk-android:1.3.0")
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
binoban.track("Order Completed", buildJsonObject {
    put("item", "shoes")
    put("price", 49.99)
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
import android.app.Application;
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
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonElementKt;
import java.util.HashMap;
import java.util.Map;

// Build properties map
Map<String, JsonElement> propsMap = new HashMap<>();
propsMap.put("item", JsonElementKt.JsonPrimitive("shoes"));
propsMap.put("price", JsonElementKt.JsonPrimitive(49.99));
JsonObject props = new JsonObject(propsMap);

// Track an event
binoban.track("Order Completed", props);

// Identify a user
Map<String, JsonElement> traitsMap = new HashMap<>();
traitsMap.put("email", JsonElementKt.JsonPrimitive("user@example.com"));
binoban.identify("user-123", new JsonObject(traitsMap));

// Flush buffered events
binoban.flush();

// Reset user identity
binoban.reset();
```

The credential placeholders (`YOUR_API_KEY`, `YOUR_SOURCE_IDENTIFIER`, `YOUR_API_HOST`) are obtained the same way as the Kotlin integration: contact **tech@binoban.io**.

## Push notifications

Push runs on Firebase Cloud Messaging and is **optional**. The default build (the
`pushOff` flavor) has no Firebase dependency and no `google-services.json`, so it
builds and runs as-is: the **Push** tab shows the enable steps. Turning it on
requires your own Firebase project; Binoban does not wrap Firebase.

> The integration here mirrors the recipe in the public docs:
> [App push on Android](https://docs.binoban.io/developers/engage/mobile-push-android).
> That page is the source of truth: read it for the why behind each step.

### Enable push in this example

1. **Add your Firebase config.** Download `google-services.json` for the Android
   app (package `io.binoban.sdk.demo.android`) from your Firebase console and
   place it in `app/`. It is gitignored: never commit it.

   The build applies the `com.google.gms.google-services` plugin only when this
   file is present, so its presence is the single switch that turns Firebase on.

2. **Select the `pushOn` build variant** in Android Studio (Build → Select Build
   Variant) and run.

Build the non-push variant explicitly with `./gradlew :app:assemblePushOffDebug` when Firebase is not configured. Selecting `pushOn` without the config file also
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
   hands it to `Notification.refreshToken`: covering already-installed devices.

A `DefaultNotificationInteractionHandler` subclass (`LoggingInteractionHandler`)
calls `super` (so SDK tracking and callbacks still fire) and mirrors each
interaction into the Push tab's "Recent interactions" list. For your own app,
route `interaction.uri` and `interaction.customData` through your navigation
instead of just logging: see
[Taps and dismissals](https://docs.binoban.io/developers/engage/mobile-push-android#taps-and-dismissals).

### Verify it worked

1. After launch, your next flushed batch carries a `bb_notification_registered`
   event with the token (tap **Re-fetch FCM token** in the Push tab to force it).
2. Send a test campaign from the Binoban panel. The notification appears,
   `bb_notification_delivered` follows, and tapping it produces
   `bb_notification_clicked`: the Push tab mirrors each interaction.

See [Push events](https://docs.binoban.io/developers/reference/events/push-events)
for the full event list and payload keys.

## Documentation and support

- [Integration guide](https://docs.binoban.io/developers/reference/example-apps)
- [Developer Quick Start](https://docs.binoban.io/developers/quickstart)
- [Report a bug or request a feature](https://github.com/binoban/binoban-example-android/issues)
- [Contribution guide](CONTRIBUTING.md)
- Deployment access, credentials, and host configuration: [tech@binoban.io](mailto:tech@binoban.io).

## Security

Report vulnerabilities privately through the [security policy](SECURITY.md). Use placeholders when sharing configuration or reproduction steps.

## License

MIT. See [LICENSE](LICENSE).

## About Binoban

Binoban provides enterprise Customer Data Experience Platform (CDXP) infrastructure that connects customer data with prediction, decisioning, and activation across customer engagement, retail media, and advertising.

**Seer. Protector. Builder.** See the signal. Recognize risks and opportunities. Build what comes next.

[Binoban](https://binoban.io) · [Developer documentation](https://docs.binoban.io) · [GitHub organization](https://github.com/binoban)

**Future In Your Hands**
