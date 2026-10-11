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

@Composable
fun MasterDetailLayoutRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val hasSelection = props.bool("hasSelection")
    val density = LocalAlmadarDensity.current
    val masterWidth = props.string("masterWidth", "350px").removeSuffix("px").toFloatOrNull()?.dp ?: (density.buttonHeightLg * 8)
    val slot: @Composable (String, Modifier) -> Unit = { key, slotModifier ->
        when (val node = (props as? JsonObject)?.get(key)) {
            is JsonObject -> RenderChild(node, slotModifier, onEvent)
            is JsonArray -> Column(modifier = slotModifier) { node.forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) } }
            is JsonPrimitive -> RenderChild(node, slotModifier, onEvent)
            else -> Unit
        }
    }
    val detail: @Composable (Modifier) -> Unit = { detailModifier ->
        if (hasSelection) {
            slot("detail", detailModifier)
        } else if ((props as? JsonObject)?.get("emptyDetail") != null) {
            slot("emptyDetail", detailModifier)
        } else {
            Box(modifier = detailModifier, contentAlignment = Alignment.Center) {
                AlmadarText("Select an item to view details", variant = "body2", color = "muted", align = "center")
            }
        }
    }
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxSize().almadarLabel(props.string("aria-label"))) {
        if (maxWidth >= density.buttonHeightLg * 15) {
            Row(modifier = Modifier.fillMaxSize()) {
                slot("master", Modifier.width(masterWidth).fillMaxHeight())
                AlmadarDivider(Modifier.fillMaxHeight(), vertical = true)
                detail(Modifier.weight(1f).fillMaxHeight())
            }
        } else if (hasSelection) {
            detail(Modifier.fillMaxSize())
        } else {
            slot("master", Modifier.fillMaxSize())
        }
    }
}
