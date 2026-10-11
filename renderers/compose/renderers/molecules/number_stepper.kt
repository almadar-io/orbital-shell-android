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
fun NumberStepperRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val min = almadarScalar(root?.get("min"))?.toFloatOrNull()
    val max = almadarScalar(root?.get("max"))?.toFloatOrNull()
    val step = props.float("step", 1f).let { if (it > 0f) it else 1f }
    val incoming = props.float("value", 0f)
    var value by remember(incoming) { mutableStateOf(incoming) }
    val action = props.string("action")
    val size = props.string("size", "md")
    val disabled = props.bool("disabled")
    val atMin = min != null && value <= min
    val atMax = max != null && value >= max
    val change = { raw: Float ->
        val next = (Math.round(raw / step) * step).coerceIn(min ?: -Float.MAX_VALUE, max ?: Float.MAX_VALUE)
        value = next
        if (action.isNotEmpty()) {
            val merged = JsonObject((props.jsonObject("actionPayload") ?: JsonObject(emptyMap())) + mapOf("value" to JsonPrimitive(next)))
            onEvent(action, eventEnvelope(props.merging(mapOf("actionPayload" to merged))))
        }
    }
    Row(
        modifier = modifier.almadarLabel(props.string("aria-label", props.string("label", "Number stepper"))),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlmadarButton(
            props, "", action.ifEmpty { ALMADAR_LOCAL_ACTION }, { _, _ -> if (!atMin) change(value - step) },
            variant = "secondary", icon = "minus", size = size, disabled = disabled || atMin, ariaLabel = "Decrease", local = action.isEmpty()
        )
        AlmadarText(
            formatFieldValue(JsonPrimitive(value), "number"),
            modifier = Modifier.widthIn(min = AlmadarSizes.w14),
            variant = if (size == "lg") "h4" else if (size == "sm") "small" else "body1", weight = "medium", align = "center"
        )
        AlmadarButton(
            props, "", action.ifEmpty { ALMADAR_LOCAL_ACTION }, { _, _ -> if (!atMax) change(value + step) },
            variant = "secondary", icon = "plus", size = size, disabled = disabled || atMax, ariaLabel = "Increase", local = action.isEmpty()
        )
    }
}
