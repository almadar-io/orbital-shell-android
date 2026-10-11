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

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun PopoverRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val position = props.string("position", "bottom")
    var internal by remember { mutableStateOf(false) }
    val open = if (root?.get("open") != null) props.bool("open") else internal
    val content = root?.get("content")
    val anchor = when (position) {
        "top" -> Alignment.TopCenter
        "left" -> Alignment.CenterStart
        "right" -> Alignment.CenterEnd
        else -> Alignment.BottomCenter
    }
    Box(modifier = modifier.almadarLabel(props.string("aria-label"))) {
        Box(modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { internal = !internal }) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                props.children().forEach { child ->
                    val target = (child as? JsonObject)?.merging(mapOf("action" to JsonPrimitive(ALMADAR_LOCAL_ACTION), "local" to JsonPrimitive(true))) ?: child
                    RenderChild(target, Modifier) { _, _ -> internal = !internal }
                }
            }
        }
        if (open) {
            Popup(alignment = anchor, onDismissRequest = { internal = false }, properties = PopupProperties(focusable = true)) {
                Surface(
                    modifier = Modifier.verificationWindowRoot().almadarSurfaceEnter("popoverEnter", AlmadarEnterTiming.Popover),
                    shape = MaterialTheme.shapes.medium,
                    color = palette.card,
                    contentColor = palette.cardForeground,
                    border = androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderStandard, palette.border),
                    shadowElevation = LocalAlmadarElevation.current.popoverElevation.radius
                ) {
                    AlmadarNode(content, onEvent, Modifier.widthIn(max = almadarSpaceFor("2xl") * 5).padding(almadarSpaceFor("md")))
                }
            }
        }
    }
}
