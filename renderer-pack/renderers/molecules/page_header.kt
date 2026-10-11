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
fun PageHeaderRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val actions = props.array("actions").mapNotNull { it as? JsonObject }
    val tabs = props.array("tabs").mapNotNull { it as? JsonObject }
    val statusNode = (props as? JsonObject)?.get("status")
    val statusLabel = if (statusNode is JsonObject) statusNode.string("label") else (statusNode as? JsonPrimitive)?.contentOrNull ?: ""
    val statusVariant = if (statusNode is JsonObject) statusNode.string("variant", "default") else "default"
    val icon = props.string("icon")
    val subtitle = props.string("subtitle")
    val loading = props.bool("isLoading")
    var activeTab by remember(props.string("activeTab")) { mutableStateOf(props.string("activeTab", tabs.firstOrNull()?.string("value") ?: "")) }
    fun shown(index: Int, action: JsonObject): JsonObject = almadarJson(
        "label" to action.string("label"), "icon" to action.string("icon"), "event" to action.string("event"),
        "navigatesTo" to action.string("navigatesTo"), "disabled" to (action.bool("disabled") || loading),
        "variant" to action.string("variant", if (index == 0) "primary" else "secondary")
    )
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
        if (props.array("breadcrumbs").isNotEmpty()) {
            BreadcrumbRenderer(almadarJson("items" to props.array("breadcrumbs").let { JsonArray(it) }), Modifier, onEvent)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")), verticalAlignment = Alignment.Top) {
                if (props.bool("showBack")) {
                    AlmadarButton(props, "", props.string("backEvent", "BACK"), onEvent, variant = "ghost", icon = "arrow-left", ariaLabel = "Back")
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                        if (icon.isNotEmpty()) AlmadarGlyph(icon, color = "muted", size = "lg")
                        AlmadarText(props.string("title"), variant = "h3", modifier = Modifier.weight(1f, fill = false))
                        if (statusLabel.isNotEmpty()) AlmadarBadge(statusLabel, variant = statusVariant)
                    }
                    if (subtitle.isNotEmpty()) AlmadarText(subtitle, variant = "small", color = "muted")
                }
            }
            if (actions.isNotEmpty()) {
                AlmadarRowActions(
                    props,
                    actions.take(2).mapIndexed { index, action -> shown(index, action) },
                    actions.drop(2).mapIndexed { index, action -> shown(index + 2, action) },
                    null, onEvent
                )
            }
        }
        if (tabs.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                tabs.forEach { tab ->
                    val count = tab.string("count")
                    AlmadarLocalButton(
                        props, tab.string("label") + (if (count.isNotEmpty()) " (" + count + ")" else ""),
                        { activeTab = tab.string("value") },
                        variant = if (activeTab == tab.string("value")) "secondary" else "ghost", size = "sm"
                    )
                }
            }
        }
        if (props.jsonObject("error") != null) AlmadarErrorText(props)
        props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
    }
}
