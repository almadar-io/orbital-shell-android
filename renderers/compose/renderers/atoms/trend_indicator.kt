package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

@Composable
fun TrendIndicatorRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val value = props.float("value", Float.NaN)
    val hasValue = !value.isNaN()
    val given = props.string("direction")
    val direction = if (given.isNotEmpty()) given else if (!hasValue || value == 0f) "flat" else if (value > 0f) "up" else "down"
    if (hasValue || given.isNotEmpty()) {
        val good = if (props.bool("invert")) direction == "down" else direction == "up"
        val color = if (direction == "flat") palette.mutedForeground else if (good) palette.success else palette.error
        val size = props.string("size", "md")
        val style = almadarType(when (size) { "lg" -> "base"; "sm" -> "xs"; else -> "sm" })
        val iconSize = LocalAlmadarIconography.current.defaultSize * (when (size) { "lg" -> 1.25f; "sm" -> 0.75f; else -> 1f })
        val formatter = java.text.NumberFormat.getPercentInstance().apply { maximumFractionDigits = 2 }
        val formatted = if (hasValue) (if (value > 0f) "+" else "") + formatter.format(value / 100.0) else ""
        val description = props.string("label", direction + (if (formatted.isNotEmpty()) " " + formatted else ""))
        Row(
            modifier = modifier.almadarStatus(description),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
        ) {
            AlmadarIcon(name = if (direction == "up") "arrow-up" else if (direction == "down") "arrow-down" else "arrow-right", tint = color, modifier = Modifier.size(iconSize))
            if (props.bool("showValue", true) && formatted.isNotEmpty()) {
                Text(text = formatted, style = style.copy(fontWeight = FontWeight.Medium), color = color)
            }
        }
    }
}
