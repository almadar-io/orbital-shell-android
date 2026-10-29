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
fun DocSidebarRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val hasActive: (JsonObject) -> Boolean = { start ->
        val pending = ArrayDeque(listOf(start))
        var found = false
        while (pending.isNotEmpty() && !found) {
            val node = pending.removeFirst()
            if (node.bool("active")) found = true
            pending.addAll(node.array("items").mapNotNull { it as? JsonObject })
        }
        found
    }
    val initial = mutableSetOf<String>()
    val seed: (JsonObject, String) -> Unit = { start, startPath ->
        val pending = ArrayDeque(listOf(start to startPath))
        while (pending.isNotEmpty()) {
            val (node, path) = pending.removeFirst()
            val children = node.array("items").mapNotNull { it as? JsonObject }
            if (children.isNotEmpty() && children.any { hasActive(it) }) initial.add(path)
            children.forEachIndexed { index, child -> pending.add(child to path + "." + index) }
        }
    }
    items.forEachIndexed { index, item -> seed(item, index.toString()) }
    var open by remember { mutableStateOf(initial.toSet()) }
    @Composable
    fun Entry(item: JsonObject, depth: Int, path: String) {
        val children = item.array("items").mapNotNull { it as? JsonObject }
        val label = item.string("label")
        val href = item.string("href")
        if (children.isNotEmpty()) {
            val expanded = open.contains(path)
            Column {
                AlmadarLocalButton(
                    props, label.uppercase(), { open = if (expanded) open - path else open + path },
                    modifier = Modifier.padding(start = if (depth > 0) almadarSpaceFor("md") else Dp.Hairline),
                    variant = "ghost", size = "sm", icon = if (expanded) "chevron-down" else "chevron-right"
                )
                if (expanded) {
                    Column(modifier = Modifier.padding(start = almadarSpaceFor("md"))) {
                        children.forEachIndexed { index, child -> Entry(child, depth + 1, path + "." + index) }
                    }
                }
            }
        } else if (href.isEmpty()) {
            AlmadarText(label, modifier = Modifier.padding(start = almadarSpaceFor("sm"), top = almadarSpaceFor("xs"), bottom = almadarSpaceFor("xs")), variant = "body2", color = "muted")
        } else {
            AlmadarButton(
                props, label, NAVIGATE_EVENT, onEvent,
                modifier = Modifier.fillMaxWidth().padding(start = if (depth > 0) almadarSpaceFor("sm") else Dp.Hairline),
                payload = almadarJson("href" to href, "url" to href),
                variant = if (item.bool("active")) "secondary" else "ghost", size = "sm"
            )
        }
    }
    Column(modifier = modifier.fillMaxWidth().almadarLandmark(props.string("aria-label", "Documentation navigation")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        items.forEachIndexed { index, item -> Entry(item, 0, index.toString()) }
    }
}
