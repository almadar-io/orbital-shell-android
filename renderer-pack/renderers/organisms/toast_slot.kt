package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

private const val TOAST_DISMISS_EVENT: String = "DISMISS"
private const val TOAST_CLOSE_EVENT: String = "CLOSE"

@Composable
fun ToastSlotRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val raw = (props as? JsonObject)?.get("children")
    val child = when (raw) {
        is JsonArray -> raw.firstOrNull()
        else -> raw
    }
    val message = when (child) {
        is JsonPrimitive -> child.contentOrNull ?: ""
        is JsonObject -> child.string("message")
        else -> ""
    }
    if (child == null) {
        Box(modifier = modifier)
    } else if (message.isEmpty() && child is JsonObject) {
        Box(modifier = modifier.fillMaxWidth()) { RenderChild(child, Modifier.fillMaxWidth(), onEvent) }
    } else {
        val look = child as? JsonObject
        NotificationRenderer(
            almadarJson(
                "variant" to props.string("variant", look?.string("variant", "info") ?: "info"),
                "title" to props.string("title", look?.string("title") ?: ""),
                "message" to message.ifEmpty { "Notification" },
                "duration" to props.int("duration", 5000),
                "dismissEvent" to TOAST_DISMISS_EVENT,
                "sourceTrait" to almadarTrait(props)
            ),
            modifier.fillMaxWidth()
        ) { name, _ ->
            onEvent(name, eventEnvelope(props))
            if (name == TOAST_DISMISS_EVENT) onEvent(TOAST_CLOSE_EVENT, eventEnvelope(props))
        }
    }
}
