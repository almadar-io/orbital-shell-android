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

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DataGridRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val density = LocalAlmadarDensity.current
    val items = almadarRows((props as? JsonObject)?.get("entity"))
    val fields = almadarFields(props.array("fields").ifEmpty { props.array("columns") })
    val actions = props.array("itemActions").mapNotNull { it as? JsonObject }
    val itemClickEvent = props.string("itemClickEvent")
    val selectionEvent = props.string("selectionEvent")
    val loadMoreEvent = props.string("loadMoreEvent")
    val selectable = props.bool("selectable")
    val imageField = props.string("imageField")
    val pageSize = props.int("pageSize", 0)
    val cols = props.int("cols", 0)
    val gap = almadarSpaceFor(props.string("gap", "md"))
    val inlineCap = minOf(props.int("maxInlineActions", 1), 1)
    var visibleCount by remember { mutableStateOf(if (pageSize > 0) pageSize else Int.MAX_VALUE) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val data = items.take(visibleCount)
    val remaining = items.size - data.size
    val allIds = data.mapIndexed { index, row -> row.string("id", index.toString()) }
    val select = { next: Set<String> ->
        selected = next
        if (selectionEvent.isNotEmpty()) onEvent(selectionEvent, eventEnvelope(props, almadarSelectedPayload(next)))
    }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "grid", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")))
    } else if (data.isEmpty()) {
        AlmadarEmptySlot(props, "No items", onEvent, modifier)
    } else {
        AlmadarBoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = when {
            cols <= 0 -> 0
            maxWidth < AlmadarBreakpoints.sm -> 1
            maxWidth < AlmadarBreakpoints.lg -> minOf(cols, 2)
            maxWidth < AlmadarBreakpoints.xl -> minOf(cols, 3)
            else -> cols
        }
        LazyVerticalGrid(
            columns = if (columnCount > 0) GridCells.Fixed(columnCount) else GridCells.Adaptive(props.int("minCardWidth", 280).dp),
            modifier = Modifier.fillMaxWidth().almadarFlexFill(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            if (selectable && selected.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AlmadarCheckbox(
                        checked = allIds.isNotEmpty() && allIds.all { selected.contains(it) },
                        event = selectionEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                        onToggle = { on -> select(if (on) allIds.toSet() else emptySet()) },
                        label = selected.size.toString() + " selected",
                        local = selectionEvent.isEmpty()
                    )
                }
            }
            items(data.size) { index ->
                val row = data[index]
                val id = row.string("id", index.toString())
                val rowPayload = almadarJson("id" to (row["id"] ?: JsonPrimitive(id)), "row" to row)
                val title = fields.firstOrNull { it.variant == "h3" || it.variant == "h4" }?.let { formatFieldValue(almadarNested(row, it.path), it.format) } ?: ""
                val visible = actions
                val inline = visible.filter { it.string("variant") == "primary" }.take(inlineCap)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verificationContainer(VerificationIds.row(id))
                        .let { m -> if (itemClickEvent.isEmpty()) m else m.clickable { onEvent(itemClickEvent, eventEnvelope(props, rowPayload)) } },
                    color = palette.card,
                    contentColor = palette.cardForeground,
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(
                        LocalAlmadarGeometry.current.borderStandard,
                        if (selected.contains(id)) palette.primary else palette.border
                    )
                ) {
                    Column(modifier = Modifier.padding(density.cardPaddingMd), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                        if (imageField.isNotEmpty()) {
                            ImageRenderer(
                                almadarJson("src" to formatFieldValue(almadarNested(row, imageField), ""), "alt" to title, "aspect" to "16/9"),
                                Modifier,
                                onEvent
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.Top) {
                            if (selectable) {
                                AlmadarCheckbox(
                                    checked = selected.contains(id),
                                    event = selectionEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                                    onToggle = { on -> select(if (on) selected + id else selected - id) },
                                    local = selectionEvent.isEmpty()
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                val headerField = fields.firstOrNull { it.variant == "h3" || it.variant == "h4" }
                                if (headerField != null || visible.isNotEmpty()) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                                    ) {
                                        if (headerField != null) AlmadarFieldCell(headerField, row)
                                        if (visible.isNotEmpty()) {
                                            AlmadarRowActions(props, inline, visible.filter { !inline.contains(it) }, row, onEvent, iconOnly = true)
                                        }
                                    }
                                }
                                AlmadarRecordFields(fields, row, skipTitle = true)
                            }
                        }
                    }
                }
            }
            if (remaining > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AlmadarLocalButton(
                            props, "Show more (" + remaining + " remaining)",
                            { visibleCount += if (pageSize > 0) pageSize else 5 },
                            variant = "ghost", icon = "chevron-down", size = "sm"
                        )
                    }
                }
            }
            if (props.bool("infiniteScroll") && props.bool("hasMore") && loadMoreEvent.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LaunchedEffect(data.size) { onEvent(loadMoreEvent, eventEnvelope(props)) }
                }
            }
        }
        }
    }
}
