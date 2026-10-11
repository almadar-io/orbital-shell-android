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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BreadcrumbRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val itemEvent = props.string("itemEvent")
    val separator = props.string("separator", "chevron-right")
    val maxItems = props.int("maxItems", 0)
    val collapsed = maxItems > 0 && items.size > maxItems
    val shown: List<Pair<Int, JsonObject>> = items.withIndex().map { it.index to it.value }.let { all ->
        if (collapsed) listOf(all.first()) + all.takeLast(maxItems - 1) else all
    }
    FlowRow(
        modifier = modifier.almadarLabel(props.string("aria-label", "Breadcrumb")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
    ) {
        shown.forEachIndexed { position, (index, item) ->
            val isLast = position == shown.lastIndex
            val label = item.string("label")
            val icon = item.string("icon")
            val event = item.string("event", itemEvent)
            if (collapsed && position == 1) {
                AlmadarText("…", variant = "small", color = "muted")
                AlmadarGlyph(separator, color = "muted", size = "sm")
            }
            if (isLast || event.isEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                    if (icon.isNotEmpty()) AlmadarGlyph(icon, size = "sm")
                    AlmadarText(label, variant = "small", color = if (isLast) "primary" else "muted", weight = "medium", truncate = true)
                }
            } else {
                val href = item.string("path", item.string("href"))
                AlmadarButton(
                    props, label, event, onEvent,
                    payload = almadarJson("label" to label, "href" to href, "index" to index),
                    variant = "link", icon = icon, size = "sm"
                )
            }
            if (!isLast) AlmadarGlyph(separator, color = "muted", size = "sm")
        }
    }
}
