package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun TextHighlightRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val type = props.string("highlightType")
    val active = props.bool("isActive")
    val action = props.string("action")
    val hoverEvent = props.string("hoverEvent")
    val annotationId = props.string("annotationId")
    val nodes = props.children()
    val accent = if (type == "note") palette.warning else palette.primary
    val payload = JsonObject(if (annotationId.isNotEmpty()) mapOf("annotationId" to JsonPrimitive(annotationId)) else emptyMap())
    var interactive: Modifier = Modifier.semantics { selected = active }
    if (action.isNotEmpty()) {
        interactive = interactive
            .clickable(role = Role.Button) { onEvent(action, eventEnvelope(props, payload)) }
            .almadarTag(almadarActionId(action, props))
    }
    if (hoverEvent.isNotEmpty()) {
        interactive = interactive.almadarHover(hoverEvent) { hovered ->
            onEvent(hoverEvent, eventEnvelope(props, JsonObject(payload + mapOf("hovered" to JsonPrimitive(hovered)))))
        }
    }
    Surface(
        modifier = modifier,
        color = if (active) accent.copy(alpha = 0.1f) else palette.card,
        border = if (active) androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderHeavy, accent) else null
    ) {
        Column(modifier = interactive) {
            nodes.forEach { RenderChild(it, Modifier, onEvent) }
            HorizontalDivider(color = accent, thickness = LocalAlmadarGeometry.current.borderHeavy)
        }
    }
}
