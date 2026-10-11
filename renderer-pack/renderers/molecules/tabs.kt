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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun TabsRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val raw = (root?.get("items") as? JsonArray ?: root?.get("tabs") as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
    val tabs = raw.map { JsonObject(it + mapOf("id" to JsonPrimitive(it.string("id", it.string("value"))))) }
    val variant = props.string("variant", "default")
    val vertical = props.string("orientation", "horizontal") == "vertical"
    val tabChangeEvent = props.string("tabChangeEvent")
    var internal by remember {
        mutableStateOf(props.string("defaultActiveTab").ifEmpty { tabs.firstOrNull { it.bool("active") }?.string("id") ?: tabs.firstOrNull()?.string("id") ?: "" })
    }
    val active = if (root?.get("activeTab") != null) props.string("activeTab") else internal
    if (tabs.isEmpty()) {
        AlmadarText("No items", modifier = modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("md")), variant = "small", color = "muted")
    } else {
        val content = tabs.firstOrNull { it.string("id") == active }?.get("content")
        val strip: @Composable () -> Unit = {
            val tabItems: @Composable () -> Unit = {
                tabs.forEach { tab ->
                    val id = tab.string("id")
                    val isActive = id == active
                    val disabled = tab.bool("disabled")
                    val tabEvent = tab.string("event")
                    val primary = tabEvent.ifEmpty { tabChangeEvent }
                    val badge = almadarScalar(tab["badge"]) ?: ""
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier
                                .then(if (variant == "pills") Modifier.clip(MaterialTheme.shapes.small).background(if (isActive) palette.primary else Color.Transparent) else Modifier)
                                .clickable(enabled = !disabled, role = Role.Tab) {
                                    internal = id
                                    val payload = almadarJson("tabId" to id)
                                    if (tabChangeEvent.isNotEmpty()) onEvent(tabChangeEvent, eventEnvelope(props, payload))
                                    if (tabEvent.isNotEmpty() && tabEvent != tabChangeEvent) onEvent(tabEvent, eventEnvelope(props, payload))
                                }
                                .almadarActionTag(primary, props)
                                .almadarAlpha(if (disabled) ALMADAR_DISABLED_ALPHA else 1f)
                                .semantics { selected = isActive }
                                .padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
                            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tab.string("icon").isNotEmpty()) AlmadarGlyph(tab.string("icon"), color = if (isActive && variant == "pills") "inherit" else "muted", size = "sm")
                            AlmadarText(tab.string("label"), variant = "small", weight = "medium", color = if (isActive) (if (variant == "pills") "inherit" else "primary") else "muted")
                            if (badge.isNotEmpty()) AlmadarBadge(badge)
                        }
                        if (variant != "pills" && !vertical) {
                            Box(modifier = Modifier.fillMaxWidth().height(LocalAlmadarGeometry.current.borderStandard).background(if (isActive) palette.primary else Color.Transparent))
                        }
                    }
                }
            }
            if (vertical) {
                Column(
                    modifier = Modifier.then(if (variant == "pills") Modifier.background(palette.muted, MaterialTheme.shapes.small).padding(almadarSpaceFor("xs")) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                ) { tabItems() }
            } else {
                Column {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                            .then(if (variant == "pills") Modifier.background(palette.muted, MaterialTheme.shapes.small).padding(almadarSpaceFor("xs")) else Modifier),
                        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                    ) { tabItems() }
                    if (variant != "pills") AlmadarDivider()
                }
            }
        }
        if (vertical) {
            Row(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
                strip()
                if (content != null) AlmadarNode(content, onEvent, Modifier.weight(1f).padding(start = almadarSpaceFor("md")))
            }
        } else {
            Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
                strip()
                if (content != null) AlmadarNode(content, onEvent, Modifier.fillMaxWidth().padding(top = almadarSpaceFor("md")))
            }
        }
    }
}
