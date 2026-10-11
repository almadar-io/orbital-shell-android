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
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.platform.LocalConfiguration

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun VersionDiffRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val revisions = props.array("revisions").mapNotNull { it as? JsonObject }
        .filter { it.string("id").isNotEmpty() && it["label"] != null && it["content"] != null }
    val hunks = (props as? JsonObject)?.get("hunks") as? JsonArray
    var before by remember { mutableStateOf(props.string("beforeId", revisions.firstOrNull()?.string("id") ?: "")) }
    var after by remember { mutableStateOf(props.string("afterId", revisions.getOrNull(1)?.string("id") ?: revisions.firstOrNull()?.string("id") ?: "")) }
    var inline by remember { mutableStateOf(props.string("view", "side-by-side") == "inline") }
    var revealed by remember { mutableStateOf(setOf<Int>()) }
    val activeBefore = props.string("beforeId", before)
    val activeAfter = props.string("afterId", after)
    val beforeRevision = revisions.firstOrNull { it.string("id") == activeBefore } ?: revisions.firstOrNull()
    val afterRevision = revisions.firstOrNull { it.string("id") == activeAfter } ?: (if (revisions.size > 1) revisions[1] else revisions.firstOrNull())
    class DiffRow(val kind: String, val header: String, val line: AlmadarDiffLine?, val id: Int, val count: Int)
    val rows: List<DiffRow> = if (hunks != null) {
        hunks.mapNotNull { it as? JsonObject }.flatMap { hunk ->
            listOf(DiffRow("header", hunk.string("header"), null, 0, 0)) + hunk.array("lines").map {
                DiffRow("line", "", AlmadarDiffLine(it.string("type"), (it as? JsonObject)?.get("beforeLineNumber")?.let { n -> n.jsonPrimitive.intOrNull }, (it as? JsonObject)?.get("afterLineNumber")?.let { n -> n.jsonPrimitive.intOrNull }, it.string("content")), 0, 0)
            }
        }
    } else {
        almadarLineDiff(beforeRevision?.string("content") ?: "", afterRevision?.string("content") ?: "").map { DiffRow("line", "", it, 0, 0) }
    }
    val unchanged: (DiffRow?) -> Boolean = { it?.line?.type == "unchanged" }
    val shown = mutableListOf<DiffRow>()
    var cursor = 0
    while (cursor < rows.size) {
        if (!unchanged(rows[cursor])) {
            shown.add(rows[cursor])
            cursor++
            continue
        }
        var end = cursor
        while (end < rows.size && unchanged(rows[end])) end++
        val head = if (rows.getOrNull(cursor - 1)?.line != null) 3 else 0
        val tail = if (rows.getOrNull(end)?.line != null) 3 else 0
        val hidden = end - cursor - head - tail
        val gapId = cursor + head
        if (hidden < 4 || revealed.contains(gapId)) {
            shown.addAll(rows.subList(cursor, end))
        } else {
            shown.addAll(rows.subList(cursor, cursor + head))
            shown.add(DiffRow("gap", "", null, gapId, hidden))
            shown.addAll(rows.subList(end - tail, end))
        }
        cursor = end
    }
    val added = rows.count { it.line?.type == "added" }
    val removed = rows.count { it.line?.type == "removed" }
    val compact = LocalConfiguration.current.screenWidthDp.dp < AlmadarBreakpoints.md
    val options = JsonArray(revisions.map { almadarJson("value" to it.string("id"), "label" to it.string("label")) })
    val picker: @Composable (String, String, String, String, (String) -> Unit) -> Unit = { selected, name, label, event, assign ->
        SelectRenderer(
            almadarJson("name" to name, "options" to options, "value" to selected, "aria-label" to label, "onChange" to "version-diff-select"),
            Modifier.widthIn(min = almadarSpaceFor("2xl") * 3)
        ) { _, envelope ->
            val id = envelope?.jsonObject("actionPayload")?.string("value") ?: ""
            assign(id)
            if (event.isNotEmpty()) onEvent(event, eventEnvelope(props, almadarJson("id" to id)))
        }
    }
    val describe: (JsonObject) -> String = { revision ->
        revision.string("label") + (if (revision.string("author").isEmpty()) "" else " by " + revision.string("author")) +
            (if (revision.string("timestamp").isEmpty()) "" else " (" + revision.string("timestamp") + ")")
    }
    val lineView: @Composable (AlmadarDiffLine, String) -> Unit = { line, side ->
        val changed = if (side == "inline") line.type == "added" || line.type == "removed" else if (side == "before") line.type == "removed" else line.type == "added"
        val additive = line.type == "added"
        val number = if (side == "before") line.before else if (side == "after") line.after else (line.after ?: line.before)
        val prefix = if (side == "inline") (if (additive) "+" else if (line.type == "removed") "-" else " ") else ""
        val tint = if (changed) (if (additive) palette.success else palette.error) else palette.foreground
        Row(
            modifier = Modifier.fillMaxWidth().background(if (changed) tint.copy(alpha = 0.1f) else Color.Transparent)
                .padding(horizontal = almadarSpaceFor("sm")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
        ) {
            AlmadarMonoText(number?.toString() ?: "", Modifier.widthIn(min = almadarSpaceFor("xl")), color = palette.mutedForeground)
            if (prefix.isNotEmpty()) AlmadarMonoText(prefix, color = tint)
            AlmadarMonoText(line.content.ifEmpty { " " }, color = tint)
        }
    }
    val column: @Composable (String, Modifier) -> Unit = { side, columnModifier ->
        Column(modifier = columnModifier.verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
            shown.forEach { row ->
                if (row.kind == "header") {
                    AlmadarMonoText(
                        row.header, Modifier.fillMaxWidth().background(palette.info.copy(alpha = 0.1f))
                            .padding(horizontal = almadarSpaceFor("sm"), vertical = almadarSpaceFor("xs")),
                        color = palette.info
                    )
                } else if (row.kind == "gap") {
                    Box(modifier = Modifier.fillMaxWidth().background(palette.muted.copy(alpha = 0.4f))) {
                        AlmadarLocalButton(
                            props, row.count.toString() + " unchanged lines", { revealed = revealed + row.id },
                            variant = "ghost", size = "sm", icon = "chevrons-up-down"
                        )
                    }
                } else {
                    val line = row.line
                    if (line != null && !(side == "before" && line.type == "added") && !(side == "after" && line.type == "removed")) lineView(line, side)
                }
            }
        }
    }
    AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().background(palette.muted.copy(alpha = 0.3f))
                .padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
        ) {
            AlmadarGlyph("git-commit", color = "muted", size = "sm")
            if (hunks == null) {
                AlmadarText("Compare", variant = "small", weight = "medium")
                picker(activeBefore, "version-diff-before", "Before revision", props.string("selectBeforeEvent")) { before = it }
                AlmadarText("to", variant = "caption", color = "secondary")
                picker(activeAfter, "version-diff-after", "After revision", props.string("selectAfterEvent")) { after = it }
            }
            if (props.string("language").isNotEmpty()) AlmadarBadge(props.string("language"))
            AlmadarBadge("+" + added, variant = "success")
            AlmadarBadge("-" + removed, variant = "error")
            AlmadarLocalButton(
                props, "", { inline = !inline },
                variant = "ghost", size = "sm", icon = if (inline) "columns" else "align-left",
                ariaLabel = if (inline) "Switch to side-by-side view" else "Switch to inline view"
            )
            if (hunks == null && props.string("revertEvent").isNotEmpty() && beforeRevision != null) {
                AlmadarButton(
                    props, "Revert", props.string("revertEvent"), onEvent,
                    payload = almadarJson("id" to beforeRevision.string("id")), variant = "ghost", size = "sm", icon = "rotate-ccw"
                )
            }
        }
        if (hunks == null && beforeRevision != null && afterRevision != null &&
            listOf(beforeRevision, afterRevision).any { it.string("author").isNotEmpty() || it.string("timestamp").isNotEmpty() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("xs")),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AlmadarText(describe(beforeRevision), Modifier.weight(1f), variant = "caption", color = "secondary", truncate = true)
                AlmadarText(describe(afterRevision), Modifier.weight(1f), variant = "caption", color = "secondary", truncate = true, align = "right")
            }
        }
        Column(modifier = Modifier.fillMaxWidth().background(palette.muted.copy(alpha = 0.2f)).almadarLabel("Changes")) {
            if (hunks != null && hunks.isEmpty()) {
                AlmadarText("No changes in this version.", modifier = Modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("xl")), variant = "body2", color = "muted", align = "center")
            } else if (!inline && !compact) {
                Row(modifier = Modifier.fillMaxWidth().heightIn(max = LocalAlmadarDensity.current.rowHeightSpacious * 12)) {
                    column("before", Modifier.weight(1f))
                    Box(modifier = Modifier.fillMaxHeight().width(LocalAlmadarGeometry.current.borderHairline).background(palette.border))
                    column("after", Modifier.weight(1f))
                }
            } else if (!inline) {
                column("before", Modifier.fillMaxWidth().heightIn(max = LocalAlmadarDensity.current.rowHeightSpacious * 6))
                Box(modifier = Modifier.fillMaxWidth().height(LocalAlmadarGeometry.current.borderHairline).background(palette.border))
                column("after", Modifier.fillMaxWidth().heightIn(max = LocalAlmadarDensity.current.rowHeightSpacious * 6))
            } else {
                column("inline", Modifier.fillMaxWidth().heightIn(max = LocalAlmadarDensity.current.rowHeightSpacious * 12))
            }
        }
    }
}
