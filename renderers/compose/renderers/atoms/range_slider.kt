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
fun RangeSliderRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val minimum = props.float("min", 0f)
    val maximum = props.float("max", 100f).coerceAtLeast(minimum + 1f)
    val step = props.float("step", 1f)
    val disabled = props.bool("disabled")
    val incoming = props.float("value", minimum)
    var value by remember(incoming) { mutableStateOf(incoming.coerceIn(minimum, maximum)) }
    val action = props.string("action")
    val ticks = if (props.bool("showTicks") && step > 0f) (((maximum - minimum) / step).toInt() - 1).coerceAtLeast(0) else 0
    val shown = if (value % 1f == 0f) value.toInt().toString() else value.toString()
    val tagId = if (action.isNotEmpty()) almadarActionId(action, props) else ""
    val emit = {
        if (action.isNotEmpty()) {
            val merged = JsonObject((props.jsonObject("actionPayload") ?: JsonObject(emptyMap())) + mapOf("value" to JsonPrimitive(value)))
            onEvent(action, eventEnvelope(props.merging(mapOf("actionPayload" to merged))))
        }
    }
    Column(modifier = modifier.almadarLabel(props.string("aria-label"))) {
        if (props.bool("showTooltip")) {
            Text(text = shown, style = almadarType("xs").copy(fontWeight = FontWeight.Medium), color = palette.foreground)
        }
        Slider(
            value = value,
            onValueChange = { raw ->
                value = if (step > 0f) (Math.round((raw - minimum) / step) * step + minimum).coerceIn(minimum, maximum) else raw
            },
            onValueChangeFinished = { emit() },
            valueRange = minimum..maximum,
            steps = ticks,
            enabled = !disabled,
            colors = SliderDefaults.colors(
                thumbColor = palette.primary,
                activeTrackColor = palette.primary,
                inactiveTrackColor = palette.muted,
                activeTickColor = palette.primaryForeground,
                inactiveTickColor = palette.mutedForeground
            ),
            modifier = Modifier.fillMaxWidth().almadarTag(tagId)
        )
    }
}
