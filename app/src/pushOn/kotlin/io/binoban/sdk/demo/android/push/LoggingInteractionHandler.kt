package io.binoban.sdk.demo.android.push

import io.binoban.sdk.core.platform.notifier.notification.DefaultNotificationInteractionHandler
import io.binoban.sdk.core.platform.notifier.notification.NotificationInteraction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Subclasses [DefaultNotificationInteractionHandler] so the SDK's own tracking
 * (delivery / click / close / failed callbacks) still fires, and additionally
 * records a formatted snapshot of each interaction into [PushInteractionLog] for
 * the demo's Push tab to display.
 *
 * In production you would route [NotificationInteraction.uri] and
 * [NotificationInteraction.customData] through your own navigation instead of
 * just logging — see
 * https://docs.binoban.io/developers/engage/mobile-push-android#taps-and-dismissals
 */
class LoggingInteractionHandler : DefaultNotificationInteractionHandler() {

    private val json = Json { prettyPrint = true }

    override fun onNotificationInteraction(interaction: NotificationInteraction) {
        super.onNotificationInteraction(interaction) // keep SDK tracking + callbacks

        val snapshot = buildJsonObject {
            put("type", interaction.type.name)
            put("notificationUuid", interaction.notificationUuid)
            interaction.actionId?.let { put("actionId", it) }
            interaction.uri?.let { put("uri", it) }
            interaction.customData?.let { put("customData", it.toString()) }
            interaction.reason?.let { put("reason", it) }
        }
        PushInteractionLog.add(json.encodeToString(JsonObject.serializer(), snapshot))
    }
}
