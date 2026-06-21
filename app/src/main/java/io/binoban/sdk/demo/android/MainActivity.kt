package io.binoban.sdk.demo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.binoban.sdk.core.Binoban
import io.binoban.sdk.demo.android.ui.theme.BinobanAndroidSdkDemoTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class DemoTab(val label: String) {
    TRACK("Track"), IDENTIFY("Identify"), SETTINGS("Settings")
}

data class Field(val id: Int, val key: String = "", val value: String = "")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BinobanAndroidSdkDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    MainScreen(
                        analytics = MainApplication.analytics,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(analytics: Binoban, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(DemoTab.TRACK) }
    var eventName by remember { mutableStateOf("") }
    val fields = remember { mutableStateListOf<Field>() }
    var fieldIdCounter by remember { mutableIntStateOf(0) }
    var lastEventJson by remember { mutableStateOf("") }

    var debugLogs by remember { mutableStateOf(Binoban.debugLogsEnabled) }
    var sdkEnabled by remember { mutableStateOf(analytics.enabled) }
    var flushAt by remember { mutableIntStateOf(analytics.configuration.flushAt) }
    var flushInterval by remember { mutableIntStateOf(analytics.configuration.flushInterval) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        // Tab row: TRACK / IDENTIFY / SETTINGS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DemoTab.entries.forEach { tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedTab == tab) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable {
                            if (tab == DemoTab.SETTINGS) {
                                debugLogs = Binoban.debugLogsEnabled
                                sdkEnabled = analytics.enabled
                                flushAt = analytics.configuration.flushAt
                                flushInterval = analytics.configuration.flushInterval
                            } else {
                                eventName = ""
                                fields.clear()
                                fieldIdCounter = 0
                                lastEventJson = ""
                            }
                            selectedTab = tab
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label.uppercase(),
                        color = if (selectedTab == tab) Color.White
                                else MaterialTheme.colorScheme.primary,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold
                                     else FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            DemoTab.SETTINGS -> SettingsPanel(
                analytics = analytics,
                debugLogs = debugLogs,
                onDebugLogsChange = {
                    debugLogs = it
                    Binoban.debugLogsEnabled = it
                },
                sdkEnabled = sdkEnabled,
                onSdkEnabledChange = {
                    sdkEnabled = it
                    analytics.enabled = it
                },
                flushAt = flushAt,
                onFlushAtChange = {
                    if (it in 1..200) {
                        flushAt = it
                        analytics.configuration.flushAt = it
                    }
                },
                flushInterval = flushInterval,
                onFlushIntervalChange = {
                    if (it in 5..600) {
                        flushInterval = it
                        analytics.configuration.flushInterval = it
                    }
                }
            )

            else -> {
                // FLUSH / RESET action row
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = { analytics.flush() }) { Text("FLUSH") }
                    OutlinedButton(onClick = { analytics.reset() }) { Text("RESET") }
                }

                Spacer(Modifier.height(8.dp))

                // Event name or User ID
                OutlinedTextField(
                    value = eventName,
                    onValueChange = { eventName = it },
                    label = {
                        Text(if (selectedTab == DemoTab.TRACK) "Event Name" else "User ID")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // Dynamic key-value property rows
                fields.forEachIndexed { index, field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = field.key,
                            onValueChange = { fields[index] = field.copy(key = it) },
                            label = { Text("Key") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field.value,
                            onValueChange = { fields[index] = field.copy(value = it) },
                            label = { Text("Value") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { fields.removeAll { it.id == field.id } }) {
                            Text(
                                "−",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                TextButton(
                    onClick = { fields.add(Field(id = ++fieldIdCounter)) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Add Field")
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        val props = buildJsonObject {
                            for (field in fields) {
                                if (field.key.isNotEmpty()) put(field.key, field.value)
                            }
                        }
                        val payload: JsonObject = when (selectedTab) {
                            DemoTab.TRACK -> {
                                analytics.track(eventName, props)
                                buildJsonObject {
                                    put("type", "track")
                                    put("event", eventName)
                                    put("properties", props)
                                }
                            }
                            DemoTab.IDENTIFY -> {
                                analytics.identify(eventName, props)
                                buildJsonObject {
                                    put("type", "identify")
                                    put("userId", eventName)
                                    put("traits", props)
                                }
                            }
                            else -> return@Button
                        }
                        lastEventJson = Json { prettyPrint = true }
                            .encodeToString(JsonObject.serializer(), payload)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Send ${selectedTab.label}")
                }

                Spacer(Modifier.height(8.dp))

                // JSON payload display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .heightIn(min = 140.dp)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (lastEventJson.isEmpty())
                            "Payload will appear here after sending…"
                        else
                            lastEventJson,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        color = if (lastEventJson.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SettingsPanel(
    analytics: Binoban,
    debugLogs: Boolean, onDebugLogsChange: (Boolean) -> Unit,
    sdkEnabled: Boolean, onSdkEnabledChange: (Boolean) -> Unit,
    flushAt: Int, onFlushAtChange: (Int) -> Unit,
    flushInterval: Int, onFlushIntervalChange: (Int) -> Unit
) {
    Column {
        SectionHeader("Runtime Controls")

        SettingsToggleRow("Debug Logs", debugLogs, onDebugLogsChange)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SettingsToggleRow("SDK Enabled", sdkEnabled, onSdkEnabledChange)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        StepperRow(
            label = "Flush At",
            value = flushAt,
            unit = "events",
            onDecrement = { onFlushAtChange(flushAt - 1) },
            onIncrement = { onFlushAtChange(flushAt + 1) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        StepperRow(
            label = "Flush Interval",
            value = flushInterval,
            unit = "s",
            onDecrement = { onFlushIntervalChange(flushInterval - 1) },
            onIncrement = { onFlushIntervalChange(flushInterval + 1) }
        )

        SectionHeader("Read-only Info")

        InfoRow("API Host", analytics.configuration.apiHost)
        InfoRow("Collect Device ID", analytics.configuration.collectDeviceId.toString())
        InfoRow("Track Lifecycle Events", analytics.configuration.trackApplicationLifecycleEvents.toString())
        InfoRow("Anonymous ID", analytics.anonymousId())

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun StepperRow(label: String, value: Int, unit: String, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: $value $unit", style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onIncrement) {
                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
