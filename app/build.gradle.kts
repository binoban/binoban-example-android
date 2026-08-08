plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Push notifications are opt-in, and your `google-services.json` is the only switch:
// drop it into `app/` and select the **pushOn** build variant in Android Studio.
// Without it the default (pushOff) build still builds and runs as-is — it has no
// Firebase dependency and never reads a credential. See README § Push notifications.
val googleServicesJson = layout.projectDirectory.file("google-services.json").asFile
val hasFirebaseConfig = googleServicesJson.exists()

android {
    namespace = "io.binoban.sdk.demo.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.binoban.sdk.demo.android"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Product flavor "push" splits Firebase-touching code into the pushOn source
    // set. pushOff (alphabetically first) is the default variant — it builds
    // without Firebase and exposes the opt-in instructions in the Push tab.
    flavorDimensions += "push"
    productFlavors {
        create("pushOff") {
            dimension = "push"
            buildConfigField("boolean", "PUSH_ENABLED", "false")
        }
        create("pushOn") {
            dimension = "push"
            buildConfigField("boolean", "PUSH_ENABLED", "true")
            // Firebase Cloud Messaging 25.x requires API 23. The default pushOff
            // flavor keeps minSdk 21 (the SDK's floor); only pushOn raises it.
            minSdk = 23
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// The google-services Gradle plugin reads app/google-services.json to wire
// FirebaseApp auto-initialization. It fails the build when that file is absent,
// so it is applied only once you have provided one — keeping the default build
// runnable with no Firebase setup at all.
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.lifecycle(
        "[binoban] No app/google-services.json — building without Firebase. " +
            "The pushOff variant is unaffected; the pushOn variant will report " +
            "\"token unavailable\". See README § Push notifications."
    )
}

dependencies {

    implementation(libs.binoban)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)

    // Firebase Cloud Messaging is only on the pushOn flavor's classpath. The
    // pushOff flavor never references Firebase types, so the default build has
    // no Firebase dependency at all.
    "pushOnImplementation"(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
