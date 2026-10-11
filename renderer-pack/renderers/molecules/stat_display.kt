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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

// tier: molecules — a grouped container styled by tokens only

@Composable
fun StatDisplayRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val density = LocalAlmadarDensity.current
    val label = props.string("label")
    val rawValue = props.string("value")
    val numeric = rawValue.toDoubleOrNull()
    val format = props.string("format")
    val max = props.float("max", 0f)
    val target = props.float("target", 0f)
    val trend = props.float("trend", 0f)
    val clickEvent = props.string("clickEvent")
    val variant = props.string("variant", "default")
    val size = props.string("size", "md")
    val shown = formatFieldValue(JsonPrimitive(rawValue), format).let { formatted ->
        val body = if (max > 0f) formatted + " / " + max.toInt() else formatted
        props.string("prefix") + body + props.string("suffix")
    }
    val trendUp = trend > 0f
    val trendGood = if (props.string("trendPolarity", "higher-is-better") == "lower-is-better") !trendUp else trendUp
    val trendText = if (trend == 0f) "" else (if (trendUp) "↑ " else "↓ ") + Math.abs(trend).let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } + (if (props.string("trendFormat") == "percent") "%" else "")
    val valueVariant = when (size) { "sm" -> "h5"; "lg" -> "h3"; else -> "h4" }
    val valueColor = when (variant) { "success", "warning", "error", "info" -> variant; else -> "primary" }
    val sparkline = props.array("sparklineData")
    val icon = props.string("icon")
    val progress = if (target > 0f && numeric != null) (numeric.toFloat() / target * 100f).coerceIn(0f, 100f) else -1f
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "stats", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier)
    } else if (props.bool("compact")) {
        Row(
            modifier = modifier.let { if (clickEvent.isEmpty()) it else it.clickable { onEvent(clickEvent, eventEnvelope(props, almadarJson("metricLabel" to label))) } },
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon.isNotEmpty()) AlmadarGlyph(icon, size = "sm")
            AlmadarText(label, variant = "caption", color = "secondary")
            AlmadarText(shown, variant = valueVariant, color = valueColor)
            if (trendText.isNotEmpty()) AlmadarText(trendText, variant = "caption", color = if (trendGood) "success" else "error", weight = "semibold")
            if (sparkline.size > 1) SparklineRenderer(almadarJson("data" to JsonArray(sparkline), "color" to "auto"), Modifier, onEvent)
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth()
                .background(palette.card, MaterialTheme.shapes.medium)
                .border(LocalAlmadarGeometry.current.borderStandard, palette.border, MaterialTheme.shapes.medium)
                .let { if (clickEvent.isEmpty()) it else it.clickable { onEvent(clickEvent, eventEnvelope(props, almadarJson("metricLabel" to label))) } }
                .padding(density.cardPaddingMd)
                .almadarLabel(props.string("aria-label")),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                AlmadarText(label, variant = "overline", color = "secondary")
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.Bottom) {
                    AlmadarText(shown, variant = valueVariant, color = valueColor)
                    if (trendText.isNotEmpty()) AlmadarText(trendText, variant = "caption", color = if (trendGood) "success" else "error", weight = "semibold")
                }
                if (progress >= 0f) AlmadarProgress(progress, color = if (variant == "default") "primary" else variant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                if (icon.isNotEmpty()) {
                    Box(modifier = Modifier.background(palette.muted, MaterialTheme.shapes.medium).padding(almadarSpaceFor("sm"))) { AlmadarGlyph(icon) }
                }
                if (sparkline.size > 1) SparklineRenderer(almadarJson("data" to JsonArray(sparkline), "color" to "auto"), Modifier, onEvent)
            }
        }
    }
}
