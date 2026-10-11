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

// tier: atoms — a single Material 3 control

@Composable
fun AtlasPanelRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    // `AtlasPanel`: with children, they lay out inside the border and the Rust renderer draws the
    // nine-slice frame (or the tiled pattern) behind them at the box's size.
    val children = props.children()
    if (children.isEmpty()) {
        LearningCanvasRenderer("atlas-panel", props, modifier, onEvent, fixedSize = true)
        return
    }
    val border = if (props.string("mode") == "repeat") Dp.Hairline else props.float("borderWidth", 16f).dp
    // A `style` width/height in pixels (a number or `"<n>px"`), as the web span takes it.
    fun stylePx(key: String): Float? {
        val value = ((props as? JsonObject)?.get("style") as? JsonObject)?.get(key) as? JsonPrimitive ?: return null
        return value.floatOrNull ?: value.content.takeIf { it.endsWith("px") }?.removeSuffix("px")?.trim()?.toFloatOrNull()
    }
    var boxed: Modifier = modifier
    stylePx("width")?.let { boxed = boxed.width(it.dp) }
    stylePx("height")?.let { boxed = boxed.height(it.dp) }
    Box(modifier = boxed) {
        AlmadarBoxWithConstraints(Modifier.matchParentSize()) {
            val fields = (props as? JsonObject)?.toMutableMap() ?: mutableMapOf()
            fields.remove("children")
            val style = (fields["style"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
            style["width"] = JsonPrimitive(maxWidth.value)
            style["height"] = JsonPrimitive(maxHeight.value)
            fields["style"] = JsonObject(style)
            LearningCanvasRenderer("atlas-panel", JsonObject(fields), Modifier, onEvent, fixedSize = true)
        }
        Column(Modifier.padding(border)) { children.forEach { RenderChild(it, Modifier, onEvent) } }
    }
}
