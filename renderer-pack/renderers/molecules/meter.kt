package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.background

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MeterRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val value = props.float("value")
    val min = props.float("min", 0f)
    val max = props.float("max", 100f)
    val label = props.string("label")
    val unit = props.string("unit")
    val variant = props.string("variant", "linear")
    val segments = props.int("segments", 5).coerceAtLeast(1)
    val showValue = props.bool("showValue", true)
    val thresholds: List<Pair<Float, String>> = props.array("thresholds").mapNotNull { it as? JsonObject }
        .map { it.float("value") to it.string("color").removePrefix("var(--color-").removeSuffix(")") }
        .ifEmpty { listOf(30f to "error", 70f to "warning", 100f to "success") }
    val toneFor: (Float) -> String = { at ->
        val percent = if (max > 0f) at / max * 100f else 0f
        (thresholds.firstOrNull { percent <= it.first } ?: thresholds.last()).second
    }
    val range = max - min
    val percentage = if (range <= 0f) 0f else ((value - min) / range * 100f).coerceIn(0f, 100f)
    val formatted = if (value == value.toLong().toFloat()) value.toLong().toString() else "%.1f".format(value)
    val display = formatted + unit
    val actions = props.array("actions").mapNotNull { it as? JsonObject }.filter { it.string("event").isNotEmpty() }
    val description = props.string("aria-label", label).let { if (it.isEmpty()) display else it + ": " + display }
    val actionRow: @Composable () -> Unit = {
        if (actions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                actions.forEach { action ->
                    AlmadarButton(
                        props, action.string("label"), action.string("event"), onEvent,
                        payload = almadarJson("value" to value), variant = "ghost", size = "sm"
                    )
                }
            }
        }
    }
    if (props.bool("isLoading")) {
        LoadingStateRenderer(almadarJson("message" to "Loading..."), modifier, AlmadarNoEvent)
    } else if (props.jsonObject("error") != null) {
        ErrorStateRenderer(almadarJson("title" to "Meter error", "message" to (props.jsonObject("error")?.string("message") ?: "")), modifier, AlmadarNoEvent)
    } else {
        AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(description)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("md")),
                horizontalAlignment = if (variant == "radial") Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) {
                if (variant == "radial") {
                    if (label.isNotEmpty()) AlmadarText(label, variant = "small", color = "secondary", weight = "medium")
                    Box(contentAlignment = Alignment.Center) {
                        ProgressBarRenderer(
                            almadarJson("value" to percentage, "max" to 100f, "color" to toneFor(value), "progressType" to "circular", "size" to props.string("size", "md")),
                            Modifier, AlmadarNoEvent
                        )
                        if (showValue) AlmadarText(display, variant = "h5", align = "center")
                    }
                    actionRow()
                } else {
                    if (label.isNotEmpty() || showValue) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            if (label.isNotEmpty()) AlmadarText(label, variant = "small", color = "secondary", weight = "medium")
                            if (showValue) AlmadarText(display, variant = "small", weight = "bold")
                        }
                    }
                    if (variant == "segmented") {
                        val lit = Math.round(percentage / 100f * segments)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                            for (index in 0 until segments) {
                                Box(
                                    modifier = Modifier.weight(1f).height(almadarSpaceFor("sm"))
                                        .background(if (index < lit) almadarTone(toneFor((index + 1).toFloat() / segments * max)).solid else palette.muted, MaterialTheme.shapes.small)
                                )
                            }
                        }
                        val labels = props.array("thresholds").mapNotNull { it as? JsonObject }
                        if (labels.any { it.string("label").isNotEmpty() }) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                labels.forEach { AlmadarText(it.string("label"), variant = "caption", color = "secondary") }
                            }
                        }
                    } else {
                        ProgressBarRenderer(
                            almadarJson("value" to percentage, "max" to 100f, "color" to toneFor(value), "progressType" to "linear", "size" to "lg"),
                            Modifier.fillMaxWidth(), AlmadarNoEvent
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            AlmadarText(formatFieldValue(JsonPrimitive(min), "number") + (if (unit.isNotEmpty()) " " + unit else ""), variant = "caption", color = "secondary")
                            AlmadarText(formatFieldValue(JsonPrimitive(max), "number") + (if (unit.isNotEmpty()) " " + unit else ""), variant = "caption", color = "secondary")
                        }
                    }
                    actionRow()
                }
            }
        }
    }
}
