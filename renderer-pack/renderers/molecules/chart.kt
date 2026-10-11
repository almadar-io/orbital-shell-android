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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChartRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val cycle = listOf(palette.primary, palette.success, palette.warning, palette.error, palette.info, palette.accent)
    val colorFor: (String, Int) -> Color = { raw, index ->
        when (raw.removePrefix("var(--color-").removeSuffix(")")) {
            "primary" -> palette.primary
            "secondary" -> palette.secondary
            "success" -> palette.success
            "warning" -> palette.warning
            "error" -> palette.error
            "info" -> palette.info
            "accent" -> palette.accent
            else -> cycle[index % cycle.size]
        }
    }
    val look = props.string("look").ifEmpty {
        when (props.string("chartType", "bar")) { "bar" -> "bar-vertical"; else -> props.string("chartType", "bar") }
    }
    val stack = props.string("stack", "none")
    val timeAxis = props.bool("timeAxis")
    val period = props.string("period")
    val showValues = props.bool("showValues")
    val showLegend = props.bool("showLegend", true)
    val drillEvent = props.string("drillEvent")
    val chartHeight = props.float("height", 200f).dp
    val declared = props.array("series").mapNotNull { it as? JsonObject }
    val simple = props.array("data").mapNotNull { it as? JsonObject }
    val seriesList: List<JsonObject> = declared.ifEmpty { if (simple.isEmpty()) emptyList() else listOf(almadarJson("name" to "default", "data" to JsonArray(simple))) }
    val points: (JsonObject) -> List<JsonObject> = { s -> s.array("data").mapNotNull { it as? JsonObject } }
    val scatter = props.array("scatterData").mapNotNull { it as? JsonObject }
    val hasContent = if (look == "scatter") scatter.isNotEmpty() else seriesList.any { points(it).isNotEmpty() }
    val categories = seriesList.flatMap { s -> points(s).map { it.string("label") } }.distinct()
    val valueAt: (JsonObject, String) -> Float = { s, label -> points(s).firstOrNull { it.string("label") == label }?.float("value") ?: 0f }
    val totals = categories.map { label -> seriesList.sumOf { valueAt(it, label).toDouble() }.toFloat() }
    val maxValue = if (stack == "normalize") 100f else if (stack == "stack") maxOf(totals.maxOrNull() ?: 1f, 1f) else maxOf(seriesList.flatMap { s -> points(s).map { it.float("value") } }.maxOrNull() ?: 1f, 1f)
    val shownLabel: (String) -> String = { raw -> if (timeAxis) almadarTimeLabel(raw, period) else raw }
    val seriesColor: (JsonObject, Int) -> Color = { s, index -> colorFor(s.string("color"), index) }
    val drill: (String, Float, String) -> Unit = { label, value, seriesName ->
        if (drillEvent.isNotEmpty()) {
            onEvent(drillEvent, eventEnvelope(props, almadarJson("label" to label, "value" to value, "seriesLabel" to (if (seriesName == "default") "" else seriesName))))
        }
    }
    val tap: (String, Float, String) -> Modifier = { label, value, seriesName ->
        if (drillEvent.isEmpty()) Modifier else Modifier.clickable { drill(label, value, seriesName) }.almadarActionTag(drillEvent, props)
    }
    val actions = props.array("actions").mapNotNull { it as? JsonObject }.filter { almadarActionEvent(it).isNotEmpty() }
    val title = props.string("title")
    val subtitle = props.string("subtitle")
    val lineSeries = seriesList.mapIndexed { index, s ->
        AlmadarChartSeries(s.string("name"), points(s).map { it.string("label") }, points(s).map { it.float("value") }, seriesColor(s, index), s.bool("dashed"))
    }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "card", modifier)
    } else if (props.jsonObject("error") != null) {
        ErrorStateRenderer(almadarJson("title" to "Chart error", "message" to (props.jsonObject("error")?.string("message") ?: "")), modifier, AlmadarNoEvent)
    } else if (!hasContent) {
        EmptyStateRenderer(almadarJson("title" to "No data"), modifier, AlmadarNoEvent)
    } else {
        Column(
            modifier = modifier.fillMaxWidth().padding(almadarSpaceFor("lg")).almadarLabel(props.string("aria-label", title)),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
        ) {
            if (title.isNotEmpty() || subtitle.isNotEmpty() || actions.isNotEmpty()) {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        if (title.isNotEmpty()) AlmadarText(title, variant = "h5")
                        if (subtitle.isNotEmpty()) AlmadarText(subtitle, variant = "small", color = "secondary")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        actions.forEach { action ->
                            AlmadarButton(
                                props, action.string("label"), almadarActionEvent(action), onEvent,
                                payload = almadarActionPayload(action, null), variant = "ghost", size = "sm"
                            )
                        }
                    }
                }
            }
            if (look == "bar-vertical" || look == "histogram") {
                val gapless = look == "histogram"
                val grouped = if (gapless) "none" else stack
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (gapless) Dp.Hairline else almadarSpaceFor("sm")),
                    verticalAlignment = Alignment.Bottom
                ) {
                    categories.forEachIndexed { catIndex, label ->
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.fillMaxWidth().height(chartHeight), contentAlignment = Alignment.BottomCenter) {
                                if (grouped == "none") {
                                    Row(modifier = Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(if (gapless) Dp.Hairline else almadarSpaceFor("xs")), verticalAlignment = Alignment.Bottom) {
                                        seriesList.forEachIndexed { sIndex, s ->
                                            val value = valueAt(s, label)
                                            Box(
                                                modifier = Modifier.weight(1f).fillMaxHeight((value / maxValue).coerceIn(0.005f, 1f))
                                                    .background(seriesColor(s, sIndex), if (gapless) androidx.compose.ui.graphics.RectangleShape else MaterialTheme.shapes.small)
                                                    .then(tap(label, value, s.string("name")))
                                            )
                                        }
                                    }
                                } else {
                                    val total = totals[catIndex]
                                    Column(
                                        modifier = Modifier.fillMaxWidth().fillMaxHeight(if (grouped == "normalize") 1f else (total / maxValue).coerceIn(0.005f, 1f)),
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        seriesList.asReversed().forEachIndexed { reversed, s ->
                                            val sIndex = seriesList.size - 1 - reversed
                                            val value = valueAt(s, label)
                                            Box(
                                                modifier = Modifier.fillMaxWidth().weight(maxOf(value, 0.0001f))
                                                    .background(seriesColor(s, sIndex))
                                                    .then(tap(label, value, s.string("name")))
                                            )
                                        }
                                    }
                                }
                            }
                            AlmadarText(shownLabel(label), variant = "caption", color = "secondary", align = "center", truncate = true)
                            if (showValues && seriesList.size == 1) AlmadarText(formatFieldValue(JsonPrimitive(valueAt(seriesList[0], label)), "number"), variant = "caption", color = "secondary", align = "center")
                        }
                    }
                }
            } else if (look == "bar-horizontal") {
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = chartHeight), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    categories.forEachIndexed { catIndex, label ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                            AlmadarText(shownLabel(label), modifier = Modifier.width(almadarSpaceFor("2xl") * 2), variant = "caption", color = "secondary", align = "right", truncate = true)
                            val total = totals[catIndex].let { if (it == 0f) 1f else it }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                if (stack == "none") {
                                    seriesList.forEachIndexed { sIndex, s ->
                                        val value = valueAt(s, label)
                                        Box(
                                            modifier = Modifier.fillMaxWidth((value / maxValue).coerceIn(0.005f, 1f)).height(almadarSpaceFor("lg"))
                                                .background(seriesColor(s, sIndex), MaterialTheme.shapes.small)
                                                .then(tap(label, value, s.string("name")))
                                        )
                                    }
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth(if (stack == "normalize") 1f else (total / maxValue).coerceIn(0.005f, 1f)).height(almadarSpaceFor("lg"))) {
                                        seriesList.forEachIndexed { sIndex, s ->
                                            val value = valueAt(s, label)
                                            Box(
                                                modifier = Modifier.fillMaxHeight().weight(maxOf(value, 0.0001f))
                                                    .background(seriesColor(s, sIndex))
                                                    .then(tap(label, value, s.string("name")))
                                            )
                                        }
                                    }
                                }
                            }
                            if (showValues && seriesList.size == 1) {
                                AlmadarText(formatFieldValue(JsonPrimitive(valueAt(seriesList[0], label)), "number"), modifier = Modifier.width(almadarSpaceFor("xl") * 1.5f), variant = "caption", color = "secondary")
                            }
                        }
                    }
                }
            } else if (look == "line" || look == "area") {
                val all = lineSeries.flatMap { it.values }
                val low = minOf(0f, all.minOrNull() ?: 0f)
                val high = maxOf(all.maxOrNull() ?: 1f, low + 1f)
                val axisLabels = lineSeries.firstOrNull()?.labels ?: emptyList()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                    Column(modifier = Modifier.height(chartHeight), verticalArrangement = Arrangement.SpaceBetween) {
                        AlmadarText(formatFieldValue(JsonPrimitive(high), "number"), variant = "caption", color = "secondary")
                        AlmadarText(formatFieldValue(JsonPrimitive(low), "number"), variant = "caption", color = "secondary")
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        AlmadarLineCanvas(
                            lineSeries, low, high, chartHeight, look == "area", palette.border, LocalAlmadarGeometry.current.borderStandard,
                            { sIndex, column ->
                                val line = lineSeries[sIndex]
                                drill(line.labels.getOrElse(column) { "" }, line.values.getOrElse(column) { 0f }, line.name)
                            },
                            Modifier.then(if (drillEvent.isEmpty()) Modifier else Modifier.almadarActionTag(drillEvent, props))
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val stride = maxOf(1, (axisLabels.size + 5) / 6)
                            axisLabels.forEachIndexed { index, label ->
                                if (index % stride == 0 || index == axisLabels.lastIndex) AlmadarText(shownLabel(label), variant = "caption", color = "secondary", truncate = true)
                            }
                        }
                    }
                }
            } else if (look == "pie" || look == "donut") {
                val slices = seriesList.firstOrNull()?.let { points(it) } ?: emptyList()
                val values = slices.map { it.float("value") }
                val colors = slices.mapIndexed { index, point -> if (point.string("color").isEmpty()) cycle[index % cycle.size] else colorFor(point.string("color"), index) }
                val sum = values.sum().let { if (it > 0f) it else 1f }
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                    AlmadarPieCanvas(
                        values, colors, look == "donut", chartHeight, almadarSpaceFor("xl"),
                        { index -> drill(slices[index].string("label"), values[index], seriesList[0].string("name")) },
                        Modifier.then(if (drillEvent.isEmpty()) Modifier else Modifier.almadarActionTag(drillEvent, props))
                    )
                    if (showLegend) {
                        slices.forEachIndexed { index, point ->
                            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(almadarSpaceFor("md")).background(colors[index], MaterialTheme.shapes.large))
                                AlmadarText(point.string("label"), variant = "caption", color = "secondary")
                                AlmadarText(formatFieldValue(JsonPrimitive(values[index]), "number") + " (" + Math.round(values[index] / sum * 100f) + "%)", variant = "caption", weight = "medium")
                            }
                        }
                    }
                }
            } else if (look == "scatter") {
                AlmadarScatterCanvas(
                    scatter.map { it.float("x") }, scatter.map { it.float("y") },
                    scatter.map { (if (it.float("size") > 0f) it.float("size") else 4f) * LocalDensity.current.density },
                    scatter.mapIndexed { index, point -> if (point.string("color").isEmpty()) cycle[0] else colorFor(point.string("color"), index) },
                    chartHeight, palette.border, LocalAlmadarGeometry.current.borderStandard,
                    { index -> drill(scatter[index].string("label"), scatter[index].float("y"), "") },
                    Modifier.then(if (drillEvent.isEmpty()) Modifier else Modifier.almadarActionTag(drillEvent, props))
                )
            }
            if (showLegend && seriesList.size > 1) {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"), Alignment.CenterHorizontally)) {
                    seriesList.forEachIndexed { index, s ->
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(almadarSpaceFor("sm")).background(seriesColor(s, index), MaterialTheme.shapes.large))
                            AlmadarText(s.string("name"), variant = "caption", color = "secondary")
                        }
                    }
                }
            }
        }
    }
}
