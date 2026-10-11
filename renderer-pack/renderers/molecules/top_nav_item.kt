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

import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun TopNavItemRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val item = props.jsonObject("item") ?: JsonObject(emptyMap())
    val children = item.array("children").mapNotNull { it as? JsonObject }
    val activeHref = props.string("activeHref")
    val soft = props.string("tone", "solid") == "soft"
    val isActive: (JsonObject) -> Boolean = { it.bool("active") || (it.string("href").isNotEmpty() && it.string("href") == activeHref) }
    val active = isActive(item) || children.any(isActive)
    var open by remember { mutableStateOf(false) }
    val entry: @Composable (JsonObject, Boolean) -> Unit = { target, on ->
        val route = almadarNavigate(target)
        val badge = almadarScalar(target["badge"]) ?: ""
        Row(
            modifier = Modifier.semantics { selected = on },
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlmadarButton(
                props, target.string("label"), route.event, onEvent,
                payload = route.payload,
                variant = if (on) (if (soft) "secondary" else "primary") else "ghost",
                icon = target.string("icon")
            )
            if (badge.isNotEmpty()) AlmadarBadge(badge, variant = if (on) "primary" else "default")
        }
    }
    Box(modifier = modifier.almadarLabel(props.string("aria-label"))) {
        if (children.isEmpty()) {
            entry(item, active)
        } else {
            ButtonRenderer(
                almadarJson(
                    "label" to item.string("label"), "rightIcon" to "chevron-down", "leftIcon" to item.string("icon"),
                    "variant" to if (active) (if (soft) "secondary" else "primary") else "ghost",
                    "action" to ALMADAR_LOCAL_ACTION, "local" to true
                ),
                Modifier.almadarTag(VerificationIds.ACTION_OVERFLOW).semantics { selected = active },
                { _, _ -> open = true }
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.verificationWindowRoot()) {
                Column(modifier = Modifier.padding(horizontal = almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    children.forEach { child ->
                        val route = almadarNavigate(child)
                        AlmadarButton(
                            props, child.string("label"), route.event,
                            { name, envelope -> open = false; onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload"))) },
                            payload = route.payload,
                            variant = if (isActive(child)) "secondary" else "ghost",
                            icon = child.string("icon")
                        )
                    }
                }
            }
        }
    }
}
