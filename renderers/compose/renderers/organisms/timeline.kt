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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TimelineRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val titleField = props.string("titleField")
    val descriptionField = props.string("descriptionField")
    val dateField = props.string("dateField")
    val statusField = props.string("statusField")
    val actions = props.array("itemActions").mapNotNull { it as? JsonObject }.filter { almadarActionEvent(it).isNotEmpty() }
    val look = props.string("look", "vertical-spacious")
    val items: List<JsonObject> = props.array("items").mapNotNull { it as? JsonObject }.ifEmpty {
        almadarRows(root?.get("entity")).mapIndexed { index, record ->
            almadarJson(
                "id" to (almadarScalar(record["id"]) ?: index.toString()),
                "title" to (if (titleField.isEmpty()) "" else formatFieldValue(almadarNested(record, titleField), "")),
                "description" to (if (descriptionField.isEmpty()) "" else formatFieldValue(almadarNested(record, descriptionField), "")),
                "date" to (if (dateField.isEmpty()) "" else formatFieldValue(almadarNested(record, dateField), "")),
                "status" to (if (statusField.isEmpty()) "pending" else formatFieldValue(almadarNested(record, statusField), "").ifEmpty { "pending" })
            )
        }
    }
    val statusTone: (String) -> String = { status -> when (status) { "complete" -> "success"; "active" -> "primary"; "error" -> "error"; else -> "muted" } }
    val statusIcon: (String) -> String = { status -> when (status) { "complete" -> "check-circle"; "active" -> "clock"; "error" -> "alert-circle"; else -> "circle" } }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "list", modifier)
    } else if (props.jsonObject("error") != null) {
        ErrorStateRenderer(almadarJson("title" to "Timeline error", "message" to (props.jsonObject("error")?.string("message") ?: "")), modifier, AlmadarNoEvent)
    } else if (items.isEmpty()) {
        EmptyStateRenderer(almadarJson("title" to "No events", "description" to "No timeline events to display."), modifier, AlmadarNoEvent)
    } else {
        val horizontal = look == "horizontal" || look == "swimlane"
        val gap = almadarSpaceFor(if (look == "vertical-compact") "xs" else if (look == "swimlane") "lg" else "md")
        val entry: @Composable (JsonObject, Int) -> Unit = { item, index ->
            val status = item.string("status", "pending")
            val isLast = index == items.lastIndex
            val tags = almadarStrings(item["tags"])
            val row = JsonObject(item - "icon")
            val lineColor = if (status == "pending") palette.border else almadarTone(statusTone(status)).solid
            Row(
                modifier = Modifier.then(if (horizontal) Modifier.widthIn(max = LocalAlmadarDensity.current.rowHeightSpacious * 6) else Modifier.fillMaxWidth())
                    .height(IntrinsicSize.Min)
                    .verificationContainer(VerificationIds.row(item.string("id", index.toString()))),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
            ) {
                Column(modifier = Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AlmadarGlyph(item.string("icon", statusIcon(status)), color = statusTone(status), size = "sm", label = status)
                    if (!isLast && !horizontal) {
                        Box(modifier = Modifier.weight(1f).width(LocalAlmadarGeometry.current.borderHeavy).background(lineColor.copy(alpha = 0.4f)))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f, fill = !horizontal).padding(bottom = almadarSpaceFor(if (isLast || look == "vertical-compact") "xs" else "lg")),
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                ) {
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AlmadarText(item.string("title"), variant = "body1", weight = "semibold")
                        if (item.string("date").isNotEmpty()) AlmadarText(formatFieldValue(JsonPrimitive(item.string("date")), "date"), variant = "caption", color = "secondary")
                    }
                    if (item.string("description").isNotEmpty()) AlmadarText(item.string("description"), variant = "small", color = "secondary")
                    if (tags.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) { tags.forEach { AlmadarBadge(it) } }
                    }
                    if (actions.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                            actions.forEach { action ->
                                AlmadarButton(
                                    props, action.string("label"), almadarActionEvent(action), onEvent,
                                    payload = almadarActionPayload(action, row), variant = "ghost", size = "sm"
                                )
                            }
                        }
                    }
                }
            }
        }
        Column(
            modifier = modifier.fillMaxWidth().padding(almadarSpaceFor("lg")).almadarLabel(props.string("aria-label")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
        ) {
            if (props.string("title").isNotEmpty()) AlmadarText(props.string("title"), variant = "h5")
            if (horizontal) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    items.forEachIndexed { index, item -> entry(item, index) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(if (look == "vertical-compact") gap else Dp.Hairline)) {
                    items.forEachIndexed { index, item -> entry(item, index) }
                }
            }
        }
    }
}
