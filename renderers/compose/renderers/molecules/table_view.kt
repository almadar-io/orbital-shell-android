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
fun TableViewRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val items = almadarRows((props as? JsonObject)?.get("entity"))
    val rawColumns = props.array("columns").ifEmpty { props.array("fields") }
    val columns = almadarFields(rawColumns)
    val alignments = rawColumns.map { (it as? JsonObject)?.string("align") ?: "" }
    val actions = props.array("itemActions").mapNotNull { it as? JsonObject }
    val itemClickEvent = props.string("itemClickEvent")
    val selectEvent = props.string("selectEvent")
    val sortEvent = props.string("sortEvent")
    val sortColumn = props.string("sortColumn")
    val sortDirection = props.string("sortDirection", "asc")
    val selectable = props.bool("selectable")
    val groupBy = props.string("groupBy")
    val pageSize = props.int("pageSize", 0)
    val initialSelection = props.array("selectedIds").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet()
    var visibleCount by remember { mutableStateOf(if (pageSize > 0) pageSize else Int.MAX_VALUE) }
    var selected by remember(initialSelection) { mutableStateOf(initialSelection) }
    val data = items.take(visibleCount)
    val remaining = items.size - data.size
    val allIds = data.mapIndexed { index, row -> row.string("id", index.toString()) }
    val select = { next: Set<String> ->
        selected = next
        if (selectEvent.isNotEmpty()) onEvent(selectEvent, eventEnvelope(props, almadarSelectedPayload(next)))
    }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "table", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")))
    } else if (data.isEmpty()) {
        AlmadarEmptySlot(props, props.string("emptyMessage", "No rows"), onEvent, modifier)
    } else {
        Column(
            modifier = modifier.fillMaxWidth().border(LocalAlmadarGeometry.current.borderStandard, palette.border, MaterialTheme.shapes.medium),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().background(palette.muted).padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectable) {
                    AlmadarCheckbox(
                        checked = allIds.isNotEmpty() && allIds.all { selected.contains(it) },
                        event = selectEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                        onToggle = { on -> select(if (on) allIds.toSet() else emptySet()) },
                        local = selectEvent.isEmpty()
                    )
                }
                columns.forEach { column ->
                    val active = sortColumn == column.path
                    val nextDirection = if (active && sortDirection == "asc") "desc" else "asc"
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (column.sortable && sortEvent.isNotEmpty()) {
                            AlmadarButton(
                                props, column.label, sortEvent, onEvent,
                                payload = almadarJson("column" to column.path, "direction" to nextDirection),
                                variant = "ghost", size = "sm",
                                icon = if (active) (if (sortDirection == "asc") "chevron-up" else "chevron-down") else "chevrons-up-down"
                            )
                        } else {
                            AlmadarText(column.label, variant = "overline", color = "muted", truncate = true)
                        }
                    }
                }
                if (actions.isNotEmpty()) Spacer(modifier = Modifier.width(LocalAlmadarDensity.current.buttonHeightSm))
            }
            var lastGroup: String? = null
            data.forEachIndexed { index, row ->
                val id = row.string("id", index.toString())
                val rowPayload = almadarJson("id" to (row["id"] ?: JsonPrimitive(id)), "row" to row)
                if (groupBy.isNotEmpty()) {
                    val group = formatFieldValue(almadarNested(row, groupBy), "")
                    if (group != lastGroup) {
                        lastGroup = group
                        AlmadarDivider(label = group)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verificationContainer(VerificationIds.row(id))
                        .let { m -> if (itemClickEvent.isEmpty()) m else m.clickable { onEvent(itemClickEvent, eventEnvelope(props, rowPayload)) } }
                        .padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectable) {
                        AlmadarCheckbox(
                            checked = selected.contains(id),
                            event = selectEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                            onToggle = { on -> select(if (on) selected + id else selected - id) },
                            local = selectEvent.isEmpty()
                        )
                    }
                    columns.forEachIndexed { columnIndex, column ->
                        Box(modifier = Modifier.weight(1f)) {
                            val text = formatFieldValue(almadarNested(row, column.path), column.format)
                            if (column.variant == "badge" || column.format == "badge" || column.format == "boolean") {
                                AlmadarFieldCell(column, row)
                            } else {
                                AlmadarText(
                                    text, variant = "small", truncate = true,
                                    weight = if (column.variant == "h3" || column.variant == "h4") "medium" else "none",
                                    align = alignments.getOrElse(columnIndex) { "" },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    if (actions.isNotEmpty()) AlmadarRowActions(props, emptyList(), actions, row, onEvent)
                }
                AlmadarDivider()
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
        }
    }
}
