package io.binoban.sdk.demo.android.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.binoban.sdk.demo.android.BuildConfig
import io.binoban.sdk.demo.android.InfoRow
import io.binoban.sdk.demo.android.SectionHeader
import io.binoban.sdk.demo.android.push.PushInteractionLog
import io.binoban.sdk.demo.android.push.PushIntegrations

/**
 * Push tab. Two modes:
 *  - `BuildConfig.PUSH_ENABLED == false` (default `pushOff` variant): shows the
 *    two opt-in steps so a developer reading the demo knows exactly how to
 *    turn push on — no Firebase credential is needed to reach this screen.
 *  - `PUSH_ENABLED == true`: live status — FCM token, POST_NOTIFICATIONS state,
 *    and the recent notification interactions recorded by the pushOn flavor's
 *    [io.binoban.sdk.demo.android.push.LoggingInteractionHandler].
 */
@Composable
fun PushPanel() {
    if (!BuildConfig.PUSH_ENABLED) {
        PushDisabledPanel()
        return
    }
    PushEnabledPanel()
}

@Composable
private fun PushDisabledPanel() {
    // No Modifier.verticalScroll here — MainScreen already wraps every tab in a
    // scrolling Column. Nesting two vertical scrolls throws an infinite-height
    // constraint at measure time.
    Column {
        InfoCard(
            title = "Push notifications are off in this build",
            body = "This default (pushOff) variant ships with no Firebase dependency and no " +
                "google-services.json, so it builds and runs as-is. To see the live push " +
                "integration, enable it in two steps:"
        )
        Spacer(Modifier.height(12.dp))
        NumberedStep(1, "Place your Firebase google-services.json in app/. " +
            "(Never commit it — it's gitignored.)")
        NumberedStep(2, "Select the pushOn build variant in Android Studio " +
            "(Build → Select Build Variant).")
        Spacer(Modifier.height(12.dp))
        InfoCard(
            title = "What the pushOn variant wires",
            body = "Notification.initialize + NotificationPlatformConfiguration.Android in " +
                "Application.onCreate, a FirebaseMessagingService forwarding data-only " +
                "Binoban pushes to Notification.notify, onNewToken → Notification.refreshToken, " +
                "the existing-token bootstrap, and a DefaultNotificationInteractionHandler " +
                "subclass that surfaces interactions here. Mirrors the recipe at " +
                "docs.binoban.io/developers/engage/mobile-push-android."
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PushEnabledPanel() {
    var token by remember { mutableStateOf<String?>(null) }
    var tokenFetched by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        mutableStateOf(PushIntegrations.current.hasPermission())
    }

    // Fetch the existing FCM token once so already-installed devices register too
    // (onNewToken only fires on generation/rotation). See
    // https://docs.binoban.io/developers/engage/mobile-push-android
    LaunchedEffect(Unit) {
        if (!tokenFetched) {
            PushIntegrations.current.requestExistingToken { result ->
                token = result
                tokenFetched = true
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    Column {
        SectionHeader("Push status")
        InfoRow("Variant", "pushOn")
        InfoRow(
            "POST_NOTIFICATIONS",
            if (permissionGranted) "granted" else "not granted"
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        InfoRow(
            "FCM token",
            token ?: if (tokenFetched) "unavailable — add app/google-services.json" else "fetching…"
        )

        Spacer(Modifier.height(12.dp))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(if (permissionGranted) "Re-request permission" else "Request notification permission")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                tokenFetched = false
                PushIntegrations.current.requestExistingToken { result ->
                    token = result
                    tokenFetched = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text("Re-fetch FCM token")
        }

        SectionHeader("Recent interactions")
        InfoCard(
            title = "How to test",
            body = "Send a test campaign from the Binoban panel. A notification appears; " +
                "bb_notification_delivered follows; tapping it produces bb_notification_clicked. " +
                "Each interaction the SDK reports is mirrored here by the demo's " +
                "LoggingInteractionHandler (which calls super, so SDK tracking still fires)."
        )
        Spacer(Modifier.height(8.dp))

        if (PushInteractionLog.interactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .heightIn(min = 80.dp)
                    .padding(8.dp)
            ) {
                Text(
                    text = "No interactions yet — send a test push and tap it.",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = { PushInteractionLog.clear() }) { Text("Clear") }
            }
            PushInteractionLog.interactions.forEach { entry ->
                InteractionEntry(entry)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InteractionEntry(entry: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        Text(
            text = entry,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "$number.",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
