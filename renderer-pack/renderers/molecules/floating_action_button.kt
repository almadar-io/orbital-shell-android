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
fun FloatingActionButtonRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val actions = props.array("actions").mapNotNull { it as? JsonObject }
    val action = props.string("action")
    val icon = props.string("icon")
    val variant = props.string("variant", "primary")
    val label = props.string("label")
    val position = props.string("position", "bottom-right")
    val alignment = if (position.endsWith("left")) Alignment.Start else if (position.endsWith("center")) Alignment.CenterHorizontally else Alignment.End
    val vertical = if (position.startsWith("top")) Alignment.Top else Alignment.Bottom
    var expanded by remember { mutableStateOf(false) }
    val sourcePayload = (props as? JsonObject)?.get("actionPayload")
    Column(
        modifier = modifier.fillMaxSize().padding(almadarSpaceFor("lg")),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"), vertical)
    ) {
        if (actions.isEmpty()) {
            if (icon.isNotEmpty()) {
                AlmadarFab(props, action, onEvent, icon, payload = sourcePayload, variant = variant, ariaLabel = label.ifEmpty { "Action" })
            }
        } else {
            if (expanded && actions.size > 1) {
                actions.forEach { item ->
                    AlmadarButton(
                        props, item.string("label"), item.string("event"),
                        { _, _ ->
                            expanded = false
                            onEvent(item.string("event"), eventEnvelope(props, almadarJson("actionId" to item.string("id"))))
                        },
                        payload = almadarJson("actionId" to item.string("id")),
                        variant = item.string("variant", "primary"), icon = item.string("icon"), size = "lg"
                    )
                }
            }
            if (actions.size == 1) {
                val only = actions[0]
                AlmadarFab(props, only.string("event"), onEvent, only.string("icon"), payload = almadarJson("actionId" to only.string("id")), variant = only.string("variant", "primary"), ariaLabel = only.string("label"))
            } else {
                AlmadarFab(
                    props, ALMADAR_LOCAL_ACTION, { _, _ -> expanded = !expanded }, if (expanded) "x" else "plus",
                    variant = if (expanded) "secondary" else "primary", local = true,
                    ariaLabel = if (expanded) "Close actions" else "Open actions"
                )
            }
        }
    }
}
