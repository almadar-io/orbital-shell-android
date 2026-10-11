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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StatCardRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val data = almadarRows((props as? JsonObject)?.get("entity"))
    val metrics = props.array("metrics").mapNotNull { it as? JsonObject }
    val tiles = metrics.map { metric ->
        val filter = metric.jsonObject("filter")
        val rows = if (filter == null) data else data.filter { almadarScalar(it[filter.string("field")]) == almadarScalar(filter["equals"]) }
        val declared = almadarScalar(metric["value"])
        val field = metric.string("field")
        val value: JsonElement = if (declared != null) JsonPrimitive(declared)
            else if (metric.string("aggregate") == "count") JsonPrimitive(rows.size)
            else if (field.isEmpty()) JsonPrimitive(0)
            else JsonPrimitive(rows.sumOf { (it[field] as? JsonPrimitive)?.doubleOrNull ?: 0.0 })
        Triple(metric.string("label"), value, metric.string("format", "number"))
    }
    val label = tiles.firstOrNull()?.first ?: props.string("label", props.string("title", "Stat"))
    val rawValue = (props as? JsonObject)?.get("value")
    val shownValue = tiles.firstOrNull()?.let { formatFieldValue(it.second, it.third) } ?: when (rawValue) {
        is JsonArray -> almadarScalar(rawValue.firstOrNull()) ?: rawValue.size.toString()
        is JsonPrimitive -> if (rawValue.floatOrNull != null) formatFieldValue(rawValue, "number") else rawValue.contentOrNull ?: "0"
        else -> "0"
    }
    val manual = almadarScalar((props as? JsonObject)?.get("trend"))?.toFloatOrNull()
    val previous = almadarScalar((props as? JsonObject)?.get("previousValue"))?.toFloatOrNull()
    val current = almadarScalar((props as? JsonObject)?.get("currentValue"))?.toFloatOrNull()
    val trend: Float? = manual ?: if (previous != null && current != null) {
        if (previous == 0f) (if (current > 0f) 100f else 0f) else (current - previous) / previous * 100f
    } else null
    val direction = props.string("trendDirection", if (trend == null || trend == 0f) "neutral" else if (trend > 0f) "up" else "down")
    val positive = if (props.bool("invertTrend")) direction == "down" else direction == "up"
    val action = props.jsonObject("action")
    val actionEvent = if (action == null) "" else almadarActionEvent(action)
    val density = LocalAlmadarDensity.current
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "card", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarPanel(modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(density.cardPaddingLg), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                AlmadarText(label, variant = "overline", color = "secondary")
                AlmadarErrorText(props)
            }
        }
    } else if (tiles.size > 1) {
        FlowRow(
            modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
        ) {
            tiles.forEach { tile ->
                AlmadarPanel(modifier = Modifier.weight(1f).widthIn(min = AlmadarSizes.w40)) {
                    Column(modifier = Modifier.padding(density.cardPaddingMd)) {
                        AlmadarText(tile.first, variant = "overline", color = "secondary")
                        AlmadarText(formatFieldValue(tile.second, tile.third), variant = "h4")
                    }
                }
            }
        }
    } else {
        AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
            Column(modifier = Modifier.padding(density.cardPaddingLg), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        AlmadarText(label, variant = "overline", color = "secondary")
                        AlmadarText(shownValue, variant = "h4")
                        if (trend != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                                AlmadarGlyph(
                                    if (direction == "up") "trending-up" else if (direction == "down") "trending-down" else "minus",
                                    color = if (positive) "success" else if (direction == "neutral") "muted" else "error", size = "sm"
                                )
                                AlmadarText(
                                    java.text.NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }.format(Math.abs(trend) / 100f),
                                    variant = "caption", color = if (positive) "success" else if (direction == "neutral") "muted" else "error", weight = "bold"
                                )
                                AlmadarText("vs last period", variant = "small", color = "secondary")
                            }
                        } else if (props.string("subtitle").isNotEmpty()) {
                            AlmadarText(props.string("subtitle"), variant = "small", color = "secondary")
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        if (props.string("icon").isNotEmpty()) AlmadarGlyph(props.string("icon"), size = "lg")
                        val spark = props.array("sparklineData")
                        if (spark.size > 1) SparklineRenderer(almadarJson("data" to JsonArray(spark), "color" to "auto"), Modifier, AlmadarNoEvent)
                    }
                }
                if (action != null && actionEvent.isNotEmpty()) {
                    AlmadarButton(
                        props, action.string("label") + " →", actionEvent, onEvent,
                        payload = almadarActionPayload(action, null), variant = "ghost", size = "sm"
                    )
                }
            }
        }
    }
}
