package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: atoms — a single Material 3 control

@Composable
fun ConditionalWrapperRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val nodes = props.children()
    val condition = (props as? JsonObject)?.get("condition")
    val visible = (condition as? JsonPrimitive)?.booleanOrNull ?: true
    key(props.jsonObject("context")) {
        if (visible) {
            Column(modifier = modifier) { nodes.forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) } }
        } else {
            val fallback = (props as? JsonObject)?.get("fallback")
            if (fallback != null && fallback !is JsonNull) RenderChild(fallback, modifier, onEvent)
        }
    }
}
