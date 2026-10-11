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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

private class GanttTask(
    val id: String,
    val label: String,
    val status: String,
    val group: String,
    val start: java.time.LocalDate,
    val end: java.time.LocalDate,
    val reversed: Boolean
)

private fun ganttDay(raw: JsonElement?): java.time.LocalDate? {
    val primitive = raw as? JsonPrimitive ?: return null
    val text = primitive.content
    if (text.isEmpty() || primitive is JsonNull) return null
    if (!primitive.isString) {
        return text.toDoubleOrNull()?.let { java.time.Instant.ofEpochMilli(it.toLong()).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    }
    return almadarParseDateTime(text)?.toLocalDate()
}

private fun Modifier.ganttBottom(color: Color, width: Dp): Modifier = this.drawBehind {
    drawRect(color, topLeft = Offset(0f, size.height - width.toPx()), size = Size(size.width, width.toPx()))
}

private fun Modifier.ganttStart(color: Color, width: Dp): Modifier = this.drawBehind {
    drawRect(color, size = Size(width.toPx(), size.height))
}

private fun Modifier.ganttEnd(color: Color, width: Dp): Modifier = this.drawBehind {
    drawRect(color, topLeft = Offset(size.width - width.toPx(), 0f), size = Size(width.toPx(), size.height))
}

@Composable
fun GanttRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val titleField = props.string("titleField", "title")
    val startField = props.string("startField", "start")
    val endField = props.string("endField", "end")
    val statusField = props.string("statusField", "status")
    val durationField = props.string("durationField")
    val groupField = props.string("groupField")
    val tasks = almadarRows(root?.get("tasks")).mapIndexedNotNull { index, row ->
        val declaredEnd = ganttDay(almadarNested(row, endField))
        val declaredStart = ganttDay(almadarNested(row, startField)) ?: declaredEnd ?: return@mapIndexedNotNull null
        var start = declaredStart
        val end: java.time.LocalDate
        var reversed = false
        if (declaredEnd != null) {
            reversed = declaredEnd.isBefore(declaredStart)
            if (reversed) start = declaredEnd
            end = (if (reversed) declaredStart else declaredEnd).plusDays(1)
        } else {
            val days = if (durationField.isEmpty()) null else almadarScalar(almadarNested(row, durationField))?.toDoubleOrNull()
            end = start.plusDays(if (days != null && days > 0) days.toLong() else 1L)
        }
        GanttTask(
            id = almadarScalar(row["id"]) ?: index.toString(),
            label = almadarScalar(almadarNested(row, titleField)) ?: "",
            status = almadarScalar(almadarNested(row, statusField)) ?: "",
            group = if (groupField.isEmpty()) "" else almadarScalar(almadarNested(row, groupField)) ?: "",
            start = start, end = end, reversed = reversed
        )
    }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "grid", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier)
    } else if (tasks.isEmpty()) {
        EmptyStateRenderer(almadarJson("title" to "No data available", "sourceTrait" to almadarTrait(props)), modifier, onEvent)
    } else {
        val dayWidth = (root?.get("dayWidth") as? JsonPrimitive)?.floatOrNull ?: 28f
        val lo = ganttDay(root?.get("rangeStart")) ?: tasks.minOf { it.start }.minusDays(2)
        val rawHi = ganttDay(root?.get("rangeEnd")) ?: tasks.maxOf { it.end }.plusDays(2)
        val hi = if (rawHi.isAfter(lo)) rawHi else lo.plusDays(1)
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(lo, hi).toInt().coerceAtLeast(1)
        val chartWidth = (dayWidth * totalDays).dp
        val rowHeight = almadarSpace("space9")
        val headerHeight = almadarSpace("space11")
        val labelWidth = AlmadarSizes.w48
        val hairline = geometry.borderHairline
        val standard = geometry.borderStandard
        val offsetOf: (java.time.LocalDate) -> Float = { date -> java.time.temporal.ChronoUnit.DAYS.between(lo, date).toFloat() * dayWidth }
        val rows: List<Pair<String?, GanttTask?>> = if (groupField.isEmpty()) tasks.map { null to it } else {
            val seen = mutableSetOf<String>()
            buildList {
                tasks.forEach { task ->
                    if (seen.add(task.group)) add((task.group.ifEmpty { "—" }) to null)
                    add(null to task)
                }
            }
        }
        val barEvent = props.string("barClickEvent")
        val today = (props as? JsonObject)?.get("at")?.let { (it as? JsonPrimitive)?.doubleOrNull }?.let { java.time.Instant.ofEpochMilli(it.toLong()).atZone(java.time.ZoneId.systemDefault()).toLocalDate() } ?: java.time.LocalDate.now()
        val surface = if (props.string("surface", "auto") == "none") Color.Transparent else palette.card
        val totalHeight = headerHeight + rowHeight * rows.size
        val barShape = RoundedCornerShape(LocalAlmadarGeometry.current.radiusInteractive / 2)
        val geometryById = mutableMapOf<String, Triple<Float, Float, Float>>()
        rows.forEachIndexed { index, (_, task) ->
            if (task != null) {
                val x0 = offsetOf(task.start)
                geometryById[task.id] = Triple(x0, maxOf(offsetOf(task.end), x0 + dayWidth / 2f), (headerHeight + rowHeight * index + rowHeight / 2).value)
            }
        }
        val links = (root?.get("links") as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
        val arrowColor = palette.mutedForeground
        val reach = almadarSpace("space2").value
        val half = almadarSpace("space1").value
        Row(modifier = modifier.fillMaxWidth().background(surface).almadarLabel(props.string("aria-label"))) {
            Column(modifier = Modifier.width(labelWidth).ganttEnd(palette.border, standard)) {
                Box(modifier = Modifier.size(labelWidth, headerHeight).ganttBottom(palette.border, standard))
                rows.forEach { (group, task) ->
                    if (group != null) {
                        Box(
                            modifier = Modifier.size(labelWidth, rowHeight).background(palette.muted.copy(alpha = 0.3f)).ganttBottom(palette.border, standard).padding(horizontal = almadarSpace("space3")),
                            contentAlignment = Alignment.CenterStart
                        ) { AlmadarText(group, variant = "caption", weight = "semibold") }
                    } else if (task != null) {
                        Box(
                            modifier = Modifier.size(labelWidth, rowHeight).ganttBottom(palette.border.copy(alpha = 0.5f), standard).padding(horizontal = almadarSpace("space3")),
                            contentAlignment = Alignment.CenterStart
                        ) { AlmadarText(task.label, variant = "small", truncate = true) }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                Box(modifier = Modifier.width(chartWidth).height(totalHeight)) {
                    Column {
                        Row(modifier = Modifier.ganttBottom(palette.border, standard)) {
                            repeat(totalDays) { index ->
                                val date = lo.plusDays(index.toLong())
                                val weekend = date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY
                                Box(
                                    modifier = Modifier
                                        .size(dayWidth.dp, headerHeight)
                                        .background(if (weekend) palette.muted.copy(alpha = 0.4f) else Color.Transparent)
                                        .ganttStart(palette.border.copy(alpha = 0.5f), hairline)
                                        .padding(bottom = almadarSpace("space1")),
                                    contentAlignment = Alignment.BottomCenter
                                ) { if (dayWidth >= 20f) AlmadarText(date.dayOfMonth.toString(), variant = "caption", color = "secondary") }
                            }
                        }
                        rows.forEach { (group, task) ->
                            if (group != null) {
                                Box(modifier = Modifier.size(chartWidth, rowHeight).background(palette.muted.copy(alpha = 0.3f)).ganttBottom(palette.border, standard))
                            } else if (task != null) {
                                val fill = when ((root?.get("statusColorMap") as? JsonObject)?.get(task.status)?.let { almadarScalar(it) } ?: "") {
                                    "secondary" -> palette.secondary.copy(alpha = 0.8f)
                                    "success" -> palette.success.copy(alpha = 0.8f)
                                    "warning" -> palette.warning.copy(alpha = 0.8f)
                                    "danger", "error", "destructive" -> palette.error.copy(alpha = 0.8f)
                                    "info" -> palette.info.copy(alpha = 0.8f)
                                    "neutral" -> palette.mutedForeground.copy(alpha = 0.5f)
                                    else -> palette.primary.copy(alpha = 0.8f)
                                }
                                val barWidth = (offsetOf(task.end) - offsetOf(task.start)).dp
                                Box(
                                    modifier = Modifier.size(chartWidth, rowHeight).ganttBottom(palette.border.copy(alpha = 0.5f), standard),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val bar = Modifier.offset(x = offsetOf(task.start).dp).size(barWidth, almadarSpace("space4"))
                                    if (task.reversed) {
                                        val dash = almadarSpace("space1")
                                        Box(
                                            modifier = bar.background(palette.error.copy(alpha = 0.3f), barShape).drawBehind {
                                                drawRoundRect(
                                                    palette.error,
                                                    style = Stroke(width = standard.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx()))),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(geometry.radiusInteractive.toPx() / 2f)
                                                )
                                            }.semantics { contentDescription = task.label + ", Ends before it starts — check its dates" }
                                        )
                                    } else {
                                        Box(
                                            modifier = bar.background(fill, barShape)
                                                .then(if (barEvent.isNotEmpty()) Modifier.almadarActionTag(barEvent, props) else Modifier)
                                                .clickable(enabled = barEvent.isNotEmpty()) { onEvent(barEvent, eventEnvelope(props, almadarJson("id" to task.id))) }
                                                .almadarLabel(task.label)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (links.isNotEmpty()) {
                        Canvas(modifier = Modifier.size(chartWidth, totalHeight)) {
                            links.forEach { link ->
                                val from = geometryById[link.string("from")]
                                val to = geometryById[link.string("to")]
                                if (from != null && to != null) {
                                    val midX = from.second + maxOf(reach, (to.first - from.second) / 2f)
                                    val line = Path().apply {
                                        moveTo(from.second * density, from.third * density)
                                        lineTo(midX * density, from.third * density)
                                        lineTo(midX * density, to.third * density)
                                        lineTo(to.first * density, to.third * density)
                                    }
                                    drawPath(line, arrowColor, style = Stroke(width = standard.toPx()))
                                    val head = Path().apply {
                                        moveTo(to.first * density, to.third * density)
                                        lineTo((to.first - reach) * density, (to.third - half) * density)
                                        lineTo((to.first - reach) * density, (to.third + half) * density)
                                        close()
                                    }
                                    drawPath(head, arrowColor)
                                }
                            }
                        }
                    }
                    if (props.bool("showToday", true) && !today.isBefore(lo) && !today.isAfter(hi)) {
                        Box(modifier = Modifier.offset(x = offsetOf(today).dp).size(AlmadarSizes.w0_5, totalHeight).background(palette.error.copy(alpha = 0.7f)))
                    }
                }
            }
        }
    }
}
