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
fun SimpleGridRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val gap = almadarSpaceFor(props.string("gap", "md"))
    val fixed = props.int("cols", 0)
    val maxCols = props.int("maxCols", 0)
    val minWidthRaw = (props as? JsonObject)?.get("minChildWidth")?.let { (it as? JsonPrimitive)?.contentOrNull }?.removeSuffix("px")?.toFloatOrNull() ?: 250f
    val children = props.children()
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        val gapPx = gap.value
        val fit = ((maxWidth.value + gapPx) / (minWidthRaw + gapPx)).toInt().coerceAtLeast(1)
        val columns = if (fixed > 0) fixed else if (maxCols > 0) minOf(fit, maxCols) else fit
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            children.chunked(columns).forEach { rowChildren ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowChildren.forEach { RenderChild(it, Modifier.weight(1f), onEvent) }
                    repeat(columns - rowChildren.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}
