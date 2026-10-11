package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: molecules — a grouped container styled by tokens only

@Composable
fun FloatingToolbarRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val position = props.string("position", "bottom-center")
    val extras = props.children()
    val top = position.startsWith("top")
    val alignment = when {
        position.endsWith("left") -> if (top) Alignment.TopStart else Alignment.BottomStart
        position.endsWith("right") -> if (top) Alignment.TopEnd else Alignment.BottomEnd
        else -> if (top) Alignment.TopCenter else Alignment.BottomCenter
    }
    Box(modifier = modifier.fillMaxSize().padding(almadarSpaceFor("lg")).almadarLabel(props.string("aria-label")), contentAlignment = alignment) {
        AlmadarPanel {
            Row(
                modifier = Modifier.padding(almadarSpaceFor("xs")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val event = item.string("event")
                    val action = item.string("action")
                    val declared = item["actionPayload"]
                    AlmadarButton(
                        props, "", action.ifEmpty { event }, { name, envelope ->
                            onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload")))
                            if (action.isNotEmpty() && event.isNotEmpty()) onEvent(event, eventEnvelope(props, almadarJson("actionId" to item.string("id"))))
                        },
                        payload = if (action.isNotEmpty()) declared else almadarJson("actionId" to item.string("id")),
                        variant = if (item.bool("active")) "primary" else "ghost",
                        icon = item.string("icon"), size = "sm", disabled = item.bool("disabled"),
                        ariaLabel = item.string("label"), local = action.isEmpty() && event.isEmpty()
                    )
                }
                if (extras.isNotEmpty() && items.isNotEmpty()) AlmadarDivider(Modifier.height(almadarSpaceFor("lg")), vertical = true)
                extras.forEach { RenderChild(it, Modifier, onEvent) }
            }
        }
    }
}
