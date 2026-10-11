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
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun AccordionRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val multiple = props.bool("multiple")
    val toggleEvent = props.string("toggleEvent")
    val root = props as? JsonObject
    val ids = items.mapIndexed { index, item ->
        val header = item["header"] ?: item["title"]
        val label = (header as? JsonPrimitive)?.contentOrNull
        item.string("id").ifEmpty {
            if (label != null) "accordion-" + label.lowercase().replace(Regex("\\s+"), "-") + "-" + index else "accordion-item-" + index
        }
    }
    val initial: Set<String> = almadarStrings(root?.get("defaultOpenItems")).toSet().ifEmpty {
        props.array("defaultOpen").mapNotNull { (it as? JsonPrimitive)?.intOrNull }.filter { it >= 0 && it < ids.size }.map { ids[it] }.toSet()
            .ifEmpty { items.indices.filter { items[it].bool("defaultOpen") }.map { ids[it] }.toSet() }
    }
    var internal by remember { mutableStateOf(initial) }
    val controlled = root?.get("openItems") as? JsonArray
    val open = if (controlled != null) almadarStrings(controlled).toSet() else internal
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        items.forEachIndexed { index, item ->
            val id = ids[index]
            val isOpen = open.contains(id)
            val disabled = item.bool("disabled")
            if (index > 0) AlmadarDivider()
            Column(modifier = Modifier.verificationContainer(VerificationIds.row(id))) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isOpen) palette.muted else Color.Transparent)
                        .clickable(enabled = !disabled, role = Role.Button) {
                            val next = if (isOpen) open - id else if (multiple) open + id else setOf(id)
                            if (controlled == null) internal = next
                            if (toggleEvent.isNotEmpty()) onEvent(toggleEvent, eventEnvelope(props, almadarJson("itemId" to id, "isOpen" to !isOpen)))
                        }
                        .almadarActionTag(toggleEvent, props)
                        .almadarAlpha(if (disabled) ALMADAR_DISABLED_ALPHA else 1f)
                        .semantics { selected = isOpen }
                        .padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlmadarNode(item["header"] ?: item["title"], onEvent, Modifier.weight(1f), weight = if (isOpen) "bold" else "medium")
                    AlmadarGlyph(if (isOpen) "chevron-up" else "chevron-down", size = "sm")
                }
                if (isOpen) {
                    AlmadarNode(item["content"], onEvent, Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")))
                }
            }
        }
    }
}
