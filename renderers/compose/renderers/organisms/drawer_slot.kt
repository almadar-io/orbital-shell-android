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

private const val DRAWER_CLOSE_EVENT: String = "CLOSE"
private const val DRAWER_CANCEL_EVENT: String = "CANCEL"

@Composable
fun DrawerSlotRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val raw = (props as? JsonObject)?.get("children")
    val first = (raw as? JsonArray)?.firstOrNull() ?: raw
    val forward = { name: String, envelope: JsonElement? ->
        val origin = (envelope as? JsonObject)?.get("sourceTrait")
        onEvent(name, eventEnvelope(if (origin == null) props else props.merging(mapOf("sourceTrait" to origin)), envelope?.jsonObject("actionPayload")))
    }
    if (raw == null) {
        Box(modifier = modifier)
    } else {
        DrawerRenderer(
            almadarJson(
                "isOpen" to true,
                "title" to props.string("title", (first as? JsonObject)?.string("title") ?: ""),
                "position" to props.string("position", "right"),
                "width" to props.string("size", "md"),
                "closeEvent" to DRAWER_CLOSE_EVENT,
                "children" to raw,
                "sourceTrait" to almadarTrait(props)
            ),
            modifier
        ) { name, envelope ->
            forward(name, envelope)
            if (name == DRAWER_CLOSE_EVENT) onEvent(DRAWER_CANCEL_EVENT, eventEnvelope(props))
        }
    }
}
