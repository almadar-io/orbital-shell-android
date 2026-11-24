package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DataListRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val items = almadarRows((props as? JsonObject)?.get("entity"))
    val fields = almadarFields(props.array("fields").ifEmpty { props.array("columns") })
    val actions = props.array("itemActions").mapNotNull { it as? JsonObject }
    val swipeLeftActions = props.array("swipeLeftActions").mapNotNull { it as? JsonObject }
    val swipeRightActions = props.array("swipeRightActions").mapNotNull { it as? JsonObject }
    val itemClickEvent = props.string("itemClickEvent")
    val reorderEvent = props.string("reorderEvent")
    val swipeLeftEvent = props.string("swipeLeftEvent")
    val swipeRightEvent = props.string("swipeRightEvent")
    val longPressEvent = props.string("longPressEvent")
    val loadMoreEvent = props.string("loadMoreEvent")
    val variant = props.string("variant", "default")
    val groupBy = props.string("groupBy")
    val senderField = props.string("senderField")
    val senderLabelField = props.string("senderLabelField", senderField)
    val currentUser = props.string("currentUser")
    val sortBy = props.string("sortBy")
    val pageSize = props.int("pageSize", 0)
    val gap = when (props.string("gap", "md")) { "none" -> "none"; "sm" -> "xs"; "lg" -> "md"; else -> "sm" }
    val maxInline = props.int("maxInlineActions", Int.MAX_VALUE)
    var visibleCount by remember { mutableStateOf(if (pageSize > 0) pageSize else Int.MAX_VALUE) }
    val ordered = if (sortBy.isEmpty()) items else {
        val comparator = compareBy<JsonObject> { almadarNested(it, sortBy)?.let { v -> (v as? JsonPrimitive)?.contentOrNull } ?: "" }
        items.sortedWith(if (props.string("sortDirection", "asc") == "desc") comparator.reversed() else comparator)
    }
    val data = ordered.take(visibleCount)
    val remaining = ordered.size - data.size
    val message = variant == "message"
    val feed = variant == "feed"
    val senderAvatarField = props.string("senderAvatarField")
    val feedWindowMs = (props.float("feedGroupMinutes", 5f) * 60000f).toLong()
    val replyCountField = props.string("replyCountField")
    val repliersField = props.string("repliersField")
    val replyEvent = props.string("replyEvent")
    val contentField = (fields.firstOrNull { it.variant == "body" } ?: fields.firstOrNull { it.variant == "h3" || it.variant == "h4" } ?: fields.firstOrNull())?.path ?: ""
    val rowFields = if (message) fields.filter { it.path != contentField } else fields
    val timestampField = fields.firstOrNull { it.format == "date" || it.format == "datetime" || it.format == "time" }
    val feedMetaFields = fields.filter { it.path != contentField && it.path != timestampField?.path }
    val timeOf = { row: JsonObject -> timestampField?.let { almadarParseDateTime(formatFieldValue(almadarNested(row, it.path), "")) } }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "list", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")))
    } else if (data.isEmpty()) {
        AlmadarEmptySlot(props, props.string("emptyMessage", "No items"), onEvent, modifier)
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor(gap))) {
            var lastGroup: String? = null
            var previousRow: JsonObject? = null
            data.forEachIndexed { index, row ->
                val id = row.string("id", index.toString())
                val rowPayload = almadarJson("id" to (row["id"] ?: JsonPrimitive(id)), "row" to row)
                if (groupBy.isNotEmpty()) {
                    val group = formatFieldValue(almadarNested(row, groupBy), props.string("groupFormat"))
                    if (group != lastGroup) {
                        lastGroup = group
                        previousRow = null
                        AlmadarDivider(label = group)
                    }
                }
                val sender = if (senderField.isEmpty()) "" else formatFieldValue(almadarNested(row, senderField), "")
                val sent = message && currentUser.isNotEmpty() && sender == currentUser
                val shown = actions
                val inline = shown.take(maxInline)
                val overflow = shown.drop(maxInline) + swipeLeftActions.map { almadarJson("label" to it.string("label"), "icon" to it.string("icon"), "variant" to it.string("variant"), "event" to swipeLeftEvent) } +
                    swipeRightActions.map { almadarJson("label" to it.string("label"), "icon" to it.string("icon"), "variant" to it.string("variant"), "event" to swipeRightEvent) }
                var dragX by remember(id) { mutableStateOf(0f) }
                val swipeThreshold = with(LocalDensity.current) { almadarSpaceFor("2xl").toPx() * 4f }
                val rowModifier = Modifier
                    .fillMaxWidth()
                    .verificationContainer(
                        VerificationIds.ids(
                            VerificationIds.row(id),
                            almadarGestureId(swipeLeftEvent, props, VerificationIds::swipeLeft),
                            almadarGestureId(swipeRightEvent, props, VerificationIds::swipeRight),
                            almadarGestureId(longPressEvent, props, VerificationIds::longPress),
                        )
                    )
                    .let { m ->
                        if (swipeLeftEvent.isEmpty() && swipeRightEvent.isEmpty()) m else m.pointerInput(id) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragX <= -swipeThreshold && swipeLeftEvent.isNotEmpty()) onEvent(swipeLeftEvent, eventEnvelope(props, rowPayload))
                                    if (dragX >= swipeThreshold && swipeRightEvent.isNotEmpty()) onEvent(swipeRightEvent, eventEnvelope(props, rowPayload))
                                    dragX = 0f
                                }
                            ) { change, delta -> change.consume(); dragX += delta }
                        }
                    }
                    .let { m ->
                        if (longPressEvent.isNotEmpty()) m.combinedClickable(
                            onClick = { if (itemClickEvent.isNotEmpty()) onEvent(itemClickEvent, eventEnvelope(props, rowPayload)) },
                            onLongClick = { onEvent(longPressEvent, eventEnvelope(props, rowPayload)) }
                        ) else if (itemClickEvent.isNotEmpty()) m.clickable { onEvent(itemClickEvent, eventEnvelope(props, rowPayload)) } else m
                    }
                val rowActions: @Composable () -> Unit = {
                    if (shown.isNotEmpty() || overflow.isNotEmpty()) AlmadarRowActions(props, inline.map { almadarJson("label" to it.string("label"), "icon" to it.string("icon"), "variant" to it.string("variant", "ghost"), "event" to it.string("event"), "navigatesTo" to it.string("navigatesTo")) }, overflow, row, onEvent)
                }
                if (feed) {
                    val previous = previousRow
                    val senderOf = { r: JsonObject -> if (senderField.isEmpty()) "" else formatFieldValue(almadarNested(r, senderField), "") }
                    val continuation = previous != null && senderField.isNotEmpty() && senderOf(row) == senderOf(previous) && run {
                        val now = timeOf(row)
                        val before = timeOf(previous)
                        if (now == null || before == null) false else {
                            val elapsed = java.time.Duration.between(before, now).toMillis()
                            elapsed >= 0 && elapsed <= feedWindowMs
                        }
                    }
                    val authorName = formatFieldValue(almadarNested(row, senderLabelField), "").ifEmpty { sender }
                    val stampValue = timestampField?.let { formatFieldValue(almadarNested(row, it.path), "") } ?: ""
                    val stamp = if (stampValue.isEmpty()) "" else almadarParseDateTime(stampValue)?.let {
                        java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).format(it)
                    } ?: stampValue
                    val replyCount = if (replyCountField.isEmpty()) 0 else ((almadarNested(row, replyCountField) as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() ?: 0.0).toInt()
                    val repliers = if (repliersField.isEmpty()) emptyList() else ((almadarNested(row, repliersField) as? JsonArray) ?: JsonArray(emptyList())).mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    Box(modifier = rowModifier) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(
                                start = almadarSpaceFor("md"), end = almadarSpaceFor("md"),
                                top = almadarSpaceFor(if (continuation) "xs" else "sm"), bottom = almadarSpaceFor("xs")
                            ),
                            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
                        ) {
                            Box(modifier = Modifier.width(LocalAlmadarIconography.current.defaultSize * 2.5f)) {
                                if (!continuation) AvatarRenderer(almadarJson("name" to authorName, "src" to formatFieldValue(almadarNested(row, senderAvatarField), ""), "size" to "md"), Modifier, AlmadarNoEvent)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                if (!continuation) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.Bottom) {
                                        AlmadarText(authorName, variant = "small", weight = "semibold")
                                        if (stamp.isNotEmpty()) AlmadarText(stamp, variant = "caption", color = "secondary")
                                    }
                                }
                                AlmadarText(formatFieldValue(almadarNested(row, contentField), ""), variant = "body1")
                                if (feedMetaFields.isNotEmpty()) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                        feedMetaFields.forEach { field ->
                                            val text = formatFieldValue(almadarNested(row, field.path), field.format)
                                            if (text.isNotEmpty()) {
                                                if (field.variant == "badge") AlmadarFieldCell(field, row)
                                                else AlmadarText(text, variant = "caption", color = "secondary")
                                            }
                                        }
                                    }
                                }
                                if (replyCount > 0) {
                                    Row(
                                        modifier = Modifier.clickable { if (replyEvent.isNotEmpty()) onEvent(replyEvent, eventEnvelope(props, rowPayload)) },
                                        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repliers.forEach { AvatarRenderer(almadarJson("name" to it, "size" to "xs"), Modifier, AlmadarNoEvent) }
                                        AlmadarText(if (replyCount == 1) "1 reply" else replyCount.toString() + " replies", variant = "caption", color = "primary", weight = "semibold")
                                    }
                                }
                            }
                        }
                        if (actions.isNotEmpty()) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(end = almadarSpaceFor("md"))) {
                                AlmadarRowActions(props, emptyList(), actions, row, onEvent)
                            }
                        }
                    }
                    previousRow = row
                } else if (message) {
                    Row(modifier = rowModifier.padding(horizontal = almadarSpaceFor("md")), horizontalArrangement = if (sent) Arrangement.End else Arrangement.Start) {
                        Surface(
                            color = if (sent) palette.primary else palette.muted,
                            contentColor = if (sent) palette.primaryForeground else palette.foreground,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                if (!sent && senderField.isNotEmpty()) {
                                    AlmadarText(formatFieldValue(almadarNested(row, senderLabelField), "").ifEmpty { sender }, variant = "caption", weight = "semibold")
                                }
                                AlmadarText(formatFieldValue(almadarNested(row, contentField), ""), variant = "body1", color = if (sent) "inherit" else "primary")
                                AlmadarRecordFields(rowFields, row)
                                rowActions()
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = rowModifier.padding(horizontal = almadarSpaceFor(if (variant == "compact") "md" else "lg"), vertical = almadarSpaceFor(if (variant == "compact") "xs" else "sm")),
                        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlmadarRecordFields(fields, row, Modifier.weight(1f))
                        if (props.bool("reorderable") && reorderEvent.isNotEmpty()) {
                            val position = ordered.indexOf(row)
                            AlmadarButton(
                                props, "", reorderEvent, onEvent,
                                payload = almadarJson("fromIndex" to position, "toIndex" to maxOf(position - 1, 0)),
                                variant = "ghost", icon = "arrow-up", size = "sm", ariaLabel = "Move up", disabled = position <= 0
                            )
                            AlmadarButton(
                                props, "", reorderEvent, onEvent,
                                payload = almadarJson("fromIndex" to position, "toIndex" to minOf(position + 1, ordered.size - 1)),
                                variant = "ghost", icon = "arrow-down", size = "sm", ariaLabel = "Move down", disabled = position >= ordered.size - 1
                            )
                        }
                        rowActions()
                    }
                    if (variant == "card" || variant == "compact") AlmadarDivider()
                }
                previousRow = row
            }
            if (remaining > 0) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AlmadarLocalButton(
                        props, "Show more (" + remaining + " remaining)",
                        { visibleCount += if (pageSize > 0) pageSize else 5 },
                        variant = "ghost", icon = "chevron-down", size = "sm"
                    )
                }
            }
            if (props.bool("infiniteScroll") && props.bool("hasMore") && loadMoreEvent.isNotEmpty()) {
                LaunchedEffect(data.size) { onEvent(loadMoreEvent, eventEnvelope(props)) }
            }
        }
    }
}
