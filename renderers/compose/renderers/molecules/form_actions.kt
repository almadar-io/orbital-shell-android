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
fun FormActionsRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val primary = props.jsonObject("primary")
    val secondary = props.array("secondary").mapNotNull { it as? JsonObject }
    val filters = props.array("filters").mapNotNull { it as? JsonObject }
    val vertical = props.string("orientation", "horizontal") == "vertical"
    val look = props.string("look", "right-aligned-buttons")
    val firstOnly = look == "dropdown-menu" || look == "command-palette-trigger"
    val gap = if (look == "inline-row") almadarSpaceFor("sm") else almadarSpace("space0")
    val nodes = props.children()
    val actions = (secondary + listOfNotNull(primary)).let { if (firstOnly) it.take(1) else it }
    val body: @Composable () -> Unit = {
        if (nodes.isNotEmpty()) {
            (if (firstOnly) nodes.take(1) else nodes).forEach { RenderChild(it, Modifier, onEvent) }
        } else if (filters.isNotEmpty()) {
            (if (firstOnly) filters.take(1) else filters).forEach { filter ->
                AlmadarLocalButton(props, filter.string("label"), {}, variant = "ghost")
            }
        } else {
            actions.forEach { action ->
                AlmadarButton(
                    props, action.string("label"), almadarActionEvent(action), onEvent,
                    payload = almadarActionPayload(action, null),
                    variant = action.string("variant", if (action === primary) "primary" else "ghost")
                )
            }
        }
    }
    val row: @Composable () -> Unit = {
        if (vertical) Column(verticalArrangement = Arrangement.spacedBy(gap)) { body() }
        else Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) { body() }
    }
    Box(modifier = modifier.almadarLabel(props.string("aria-label"))) {
        if (look == "floating-bar") {
            AlmadarPanel { Box(modifier = Modifier.padding(almadarSpaceFor("sm"))) { row() } }
        } else {
            row()
        }
    }
}
