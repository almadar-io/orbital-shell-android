package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: molecules — a grouped container styled by tokens only

@Composable
fun ImportPreviewTreeRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val units = props.array("units").mapNotNull { it as? JsonObject }
    val skipped = props.array("skipped").mapNotNull { it as? JsonObject }
    val display = (props as? JsonObject)?.get("entityDisplay") as? JsonObject
    val indent = (props as? JsonObject)?.get("indent")?.let { (it as? JsonPrimitive)?.floatOrNull } ?: 16f
    val confirm = props.string("onConfirm")
    val cancel = props.string("onCancel")
    fun formatted(value: JsonElement?): String = when (value) {
        null, is JsonNull -> "—"
        is JsonArray -> value.joinToString(", ") { formatted(it) }
        is JsonObject -> value.toString()
        is JsonPrimitive -> value.content
    }
    fun summary(unit: JsonObject): String =
        (unit["fields"] as? JsonObject)?.entries?.joinToString(" · ") { (key, value) -> "$key: ${formatted(value)}" } ?: ""
    val groups = units.groupBy { it.string("targetEntity") }
    val tile: @Composable (@Composable ColumnScope.() -> Unit) -> Unit = { content ->
        if (props.string("surface", "auto") == "none") {
            Column(modifier = Modifier.fillMaxWidth().padding(almadarSpace("space3")), content = content)
        } else {
            AlmadarPanel(modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(almadarSpace("space3")), content = content) }
        }
    }
    val heading: @Composable (String, Int) -> Unit = { label, count ->
        Row(
            modifier = Modifier.padding(bottom = almadarSpace("space2")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlmadarText(label, variant = "label")
            AlmadarBadge(count.toString())
        }
    }
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
        if (units.isEmpty()) AlmadarText("No units staged.", variant = "body2", color = "muted")
        groups.forEach { (entity, group) ->
            val refs = group.map { it.string("ref") }.toSet()
            val children = group.filter { it.string("parentRef").isNotEmpty() && it.string("parentRef") in refs }.groupBy { it.string("parentRef") }
            val roots = group.filter { it.string("parentRef").isEmpty() || it.string("parentRef") !in refs }
            val flat = mutableListOf<Pair<JsonObject, Int>>()
            fun walk(unit: JsonObject, depth: Int) {
                flat.add(unit to depth)
                children[unit.string("ref")]?.forEach { walk(it, depth + 1) }
            }
            roots.forEach { walk(it, 0) }
            tile {
                heading(display?.jsonObject(entity)?.string("plural", entity) ?: entity, group.size)
                flat.forEach { (unit, depth) ->
                    val line = summary(unit)
                    Column(modifier = Modifier.fillMaxWidth().padding(start = indent.dp * depth, top = almadarSpace("space1"), bottom = almadarSpace("space1"))) {
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                            AlmadarGlyph("file-text", color = "muted", size = "xs")
                            AlmadarText(unit.string("title").ifEmpty { unit.string("ref") }, variant = "body2")
                        }
                        if (line.isNotEmpty()) AlmadarText(line, variant = "caption", color = "muted")
                    }
                }
            }
        }
        if (skipped.isNotEmpty()) {
            tile {
                heading("Skipped", skipped.size)
                skipped.forEach { element ->
                    Row(
                        modifier = Modifier.padding(vertical = almadarSpace("space1")),
                        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AlmadarText(element.string("ref"), variant = "body2")
                        AlmadarText(element.string("reason"), variant = "caption", color = "muted")
                    }
                }
            }
        }
        if (confirm.isNotEmpty() || cancel.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"), Alignment.End)) {
                if (cancel.isNotEmpty()) AlmadarButton(props, props.string("cancelLabel", "Cancel"), cancel, onEvent, variant = "default")
                if (confirm.isNotEmpty()) AlmadarButton(props, props.string("confirmLabel", "Confirm import"), confirm, onEvent)
            }
        }
    }
}
