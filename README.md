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

Push notification support requires a Firebase project with FCM configured. Contact Binoban customer support for setup instructions once your Firebase configuration is ready.

## Documentation

- Developer documentation: [docs.binoban.io](https://docs.binoban.io)
- Website: [binoban.io](https://binoban.io)

## Security

Please report security issues privately to **security@binoban.io**. See
[SECURITY.md](SECURITY.md). Do not open public issues for vulnerabilities.

## License

Released under the [MIT License](LICENSE), matching the Binoban Android SDK.
