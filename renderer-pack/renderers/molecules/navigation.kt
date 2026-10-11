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
fun NavigationRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val vertical = props.string("orientation", "horizontal") == "vertical"
    val entry: @Composable (JsonObject) -> Unit = { item ->
        val route = almadarNavigate(item)
        val subs = item.array("subMenu").mapNotNull { it as? JsonObject }
        val badge = almadarScalar(item["badge"]) ?: ""
        val variant = if (item.bool("isActive")) "primary" else "ghost"
        var open by remember { mutableStateOf(false) }
        Box {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                if (subs.isNotEmpty()) {
                    AlmadarLocalButton(props, item.string("label"), { open = true }, variant = variant, icon = item.string("icon"), size = "sm", disabled = item.bool("disabled"))
                } else if (route.event.isNotEmpty()) {
                    AlmadarButton(
                        props, item.string("label"), route.event, onEvent,
                        payload = route.payload, variant = variant, icon = item.string("icon"), size = "sm", disabled = item.bool("disabled")
                    )
                } else {
                    AlmadarLocalButton(props, item.string("label"), {}, variant = variant, icon = item.string("icon"), size = "sm", disabled = item.bool("disabled"))
                }
                if (badge.isNotEmpty()) AlmadarBadge(badge, variant = "danger")
            }
            if (subs.isNotEmpty()) {
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.verificationWindowRoot()) {
                    Column(modifier = Modifier.padding(horizontal = almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        subs.forEach { sub ->
                            val subRoute = almadarNavigate(sub)
                            if (subRoute.event.isNotEmpty()) {
                                AlmadarButton(
                                    props, sub.string("label"), subRoute.event,
                                    { name, envelope ->
                                        open = false
                                        onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload")))
                                    },
                                    payload = subRoute.payload, variant = "ghost", icon = sub.string("icon"), size = "sm", disabled = sub.bool("disabled")
                                )
                            } else {
                                AlmadarLocalButton(props, sub.string("label"), { open = false }, variant = "ghost", icon = sub.string("icon"), size = "sm", disabled = sub.bool("disabled"))
                            }
                        }
                    }
                }
            }
        }
    }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "list", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")))
    } else if (vertical) {
        Column(modifier = modifier.almadarLabel(props.string("aria-label", "Navigation")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
            items.forEach { entry(it) }
        }
    } else {
        FlowRow(
            modifier = modifier.almadarLabel(props.string("aria-label", "Navigation")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
        ) {
            items.forEach { entry(it) }
        }
    }
}
