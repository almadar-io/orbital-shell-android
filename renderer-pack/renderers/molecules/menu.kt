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

// tier: molecules — a grouped container styled by tokens only

@Composable
fun MenuRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val trigger = root?.get("trigger")
    var open by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<String?>(null) }
    val headerNode = root?.get("header")
    val footerNode = root?.get("footer")
    val triggerNode = (trigger as? JsonObject)?.merging(mapOf("action" to JsonPrimitive(ALMADAR_LOCAL_ACTION), "local" to JsonPrimitive(true))) ?: trigger
    @Composable
    fun Entry(item: JsonObject, index: Int, depth: Int) {
        if (item.string("type") == "divider") {
            AlmadarDivider()
        } else {
            val label = item.string("label")
            val itemId = item.string("id", "item-" + label.lowercase().replace(Regex("\\s+"), "-") + "-" + index)
            val children = item.array("subMenu").mapNotNull { it as? JsonObject }
            val event = item.string("event")
            val href = item.string("href")
            val route = if (event.isNotEmpty()) event else if (href.isNotEmpty()) NAVIGATE_EVENT else ""
            val payload = if (event.isNotEmpty()) almadarJson("itemId" to itemId, "label" to label) else almadarJson("href" to href, "url" to href)
            val badge = almadarScalar(item["badge"]) ?: ""
            Row(
                modifier = Modifier.padding(start = almadarSpaceFor("md") * depth),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (children.isNotEmpty()) {
                    AlmadarLocalButton(
                        props, label, { expanded = if (expanded == itemId) null else itemId },
                        variant = "ghost", icon = if (expanded == itemId) "chevron-down" else "chevron-right",
                        disabled = item.bool("disabled")
                    )
                } else {
                    AlmadarButton(
                        props, label, route,
                        { name, envelope ->
                            open = false
                            onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload")))
                            if (event.isNotEmpty() && href.isNotEmpty()) onEvent(NAVIGATE_EVENT, eventEnvelope(props, almadarJson("href" to href, "url" to href)))
                        },
                        payload = payload,
                        variant = if (item.string("variant") == "danger") "danger" else "ghost",
                        icon = item.string("icon"), disabled = item.bool("disabled"), ariaLabel = item.string("title")
                    )
                }
                if (badge.isNotEmpty()) AlmadarBadge(badge)
            }
            if (children.isNotEmpty() && expanded == itemId) children.forEachIndexed { childIndex, child -> Entry(child, childIndex, depth + 1) }
        }
    }
    Box(modifier = modifier.almadarLabel(props.string("aria-label"))) {
        Box(modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { open = true }) {
            AlmadarNode(triggerNode, { _, _ -> open = true })
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.verificationWindowRoot()) {
            Column(modifier = Modifier.padding(horizontal = almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                if (headerNode != null && headerNode !is JsonNull) AlmadarNode(headerNode, onEvent)
                items.forEachIndexed { index, item -> Entry(item, index, 0) }
                if (footerNode != null && footerNode !is JsonNull) AlmadarNode(footerNode, onEvent)
            }
        }
    }
}
