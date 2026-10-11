package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CalendarGridRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val density = LocalAlmadarDensity.current
    val events = almadarRows((props as? JsonObject)?.get("events"))
    val titleField = props.string("titleField", "title")
    val startField = props.string("startField", "startTime")
    val endField = props.string("endField", "endTime")
    val longPressEvent = props.string("longPressEvent")
    val slotClickEvent = props.string("onSlotClick")
    val dayClickEvent = props.string("onDayClick")
    val eventClickEvent = props.string("onEventClick")
    val isoOf = { day: java.time.LocalDate -> day.atStartOfDay(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_INSTANT) }
    val swipeLeftEvent = props.string("swipeLeftEvent")
    val swipeRightEvent = props.string("swipeRightEvent")
    val anchor = almadarParseDateTime(props.string("weekStart"))?.toLocalDate() ?: java.time.LocalDate.now()
    val weekStart = almadarWeekStart(anchor)
    val weekDays = (0L..6L).map { weekStart.plusDays(it) }
    val startOf = { row: JsonObject -> almadarParseDateTime(almadarScalar(almadarNested(row, startField)) ?: "") }
    val endOf = { row: JsonObject -> almadarParseDateTime(almadarScalar(almadarNested(row, endField)) ?: "") }
    val slots = almadarStrings((props as? JsonObject)?.get("timeSlots")).ifEmpty {
        var first = 9
        var last = 17
        events.forEach { row ->
            val start = startOf(row)
            if (start != null) {
                first = minOf(first, start.hour)
                last = maxOf(last, start.hour)
                val end = endOf(row)
                if (end != null && end.toLocalDate() == start.toLocalDate()) last = maxOf(last, if (end.minute == 0 && end.second == 0) end.hour - 1 else end.hour)
            }
        }
        (first..last).map { "%02d:00".format(it) }
    }
    val swipeThreshold = with(LocalDensity.current) { almadarSpaceFor("2xl").toPx() * 4f }
    var dragX by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(0) }
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        val auto = if (maxWidth < AlmadarBreakpoints.md) 1 else if (maxWidth < AlmadarBreakpoints.lg) 3 else 7
        val visible = props.string("dayWindow", "auto").toIntOrNull() ?: auto
        val first = offset.coerceIn(0, 7 - visible)
        val days = weekDays.subList(first, first + visible)
        val canPrev = first > 0
        val canNext = first + visible < 7
        val range = java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.getDefault())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .almadarTag(VerificationIds.ids(almadarGestureId(swipeLeftEvent, props, VerificationIds::swipeLeft), almadarGestureId(swipeRightEvent, props, VerificationIds::swipeRight)))
                .then(
                    if (swipeLeftEvent.isEmpty() && swipeRightEvent.isEmpty()) Modifier else Modifier.pointerInput(swipeLeftEvent, swipeRightEvent) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragX <= -swipeThreshold && swipeLeftEvent.isNotEmpty()) onEvent(swipeLeftEvent, eventEnvelope(props))
                                if (dragX >= swipeThreshold && swipeRightEvent.isNotEmpty()) onEvent(swipeRightEvent, eventEnvelope(props))
                                dragX = 0f
                            }
                        ) { change, delta -> change.consume(); dragX += delta }
                    }
                )
        ) {
            if (visible < 7) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("sm"), vertical = almadarSpaceFor("xs")),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlmadarLocalButton(
                        props, "Previous", { offset = maxOf(0, first - visible) },
                        variant = "ghost", size = "sm", icon = "chevron-left",
                        disabled = !canPrev, ariaLabel = "Previous days"
                    )
                    AlmadarText(
                        if (days.first() == days.last()) range.format(days.first()) else range.format(days.first()) + " – " + range.format(days.last()),
                        variant = "small", color = "muted"
                    )
                    AlmadarLocalButton(
                        props, "Next", { offset = minOf(7 - visible, first + visible) },
                        variant = "ghost", size = "sm", icon = "chevron-right",
                        disabled = !canNext, ariaLabel = "Next days"
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("xs"))) {
                Spacer(modifier = Modifier.width(AlmadarSizes.w14))
                days.forEach { day ->
                    DayCellRenderer(
                        almadarJson(
                            "date" to day.toString(), "isToday" to (day == java.time.LocalDate.now()), "onClick" to dayClickEvent,
                            "actionPayload" to almadarJson("day" to isoOf(day)), "sourceTrait" to almadarTrait(props)
                        ),
                        Modifier.weight(1f),
                        onEvent
                    )
                }
            }
            AlmadarDivider()
            slots.forEach { slot ->
                val hour = slot.substringBefore(":").toIntOrNull() ?: 0
                Row(modifier = Modifier.fillMaxWidth().heightIn(min = density.rowHeightNormal)) {
                    AlmadarText(slot, modifier = Modifier.width(AlmadarSizes.w14).padding(almadarSpaceFor("xs")), variant = "caption", color = "muted")
                    days.forEach { day ->
                        val inSlot = events.filter { row ->
                            val start = startOf(row)
                            start != null && start.toLocalDate() == day && start.hour == hour
                        }
                        val cell = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(LocalAlmadarGeometry.current.borderHairline, palette.border)
                            .padding(almadarSpaceFor("xs"))
                        val emit = {
                            onEvent(
                                longPressEvent,
                                eventEnvelope(
                                    props,
                                    JsonObject(
                                        (props.jsonObject("longPressPayload") ?: JsonObject(emptyMap())) +
                                            mapOf("date" to JsonPrimitive(day.atStartOfDay().toString()), "time" to JsonPrimitive(slot))
                                    )
                                )
                            )
                        }
                        Column(
                            modifier = if (longPressEvent.isEmpty() && slotClickEvent.isEmpty()) cell else cell
                                .combinedClickable(
                                    onClick = { if (slotClickEvent.isNotEmpty()) onEvent(slotClickEvent, eventEnvelope(props, almadarJson("day" to isoOf(day), "time" to slot))) },
                                    onLongClick = { if (longPressEvent.isNotEmpty()) emit() }
                                )
                                .almadarTag(VerificationIds.ids(almadarActionId(slotClickEvent, props), almadarGestureId(longPressEvent, props, VerificationIds::longPress))),
                            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                        ) {
                            inSlot.forEachIndexed { index, row ->
                                val chip = Modifier.fillMaxWidth().verificationContainer(VerificationIds.row(row.string("id", day.toString() + "-" + slot + "-" + index)))
                                AlmadarPanel(muted = true, modifier = if (eventClickEvent.isEmpty()) chip else chip.clickable { onEvent(eventClickEvent, eventEnvelope(props, almadarJson("event" to row))) }.almadarActionTag(eventClickEvent, props)) {
                                    AlmadarText(
                                        formatFieldValue(almadarNested(row, titleField), ""),
                                        modifier = Modifier.padding(almadarSpaceFor("xs")), variant = "small", weight = "medium", truncate = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
