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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// tier: molecules — a grouped container styled by tokens only

@Composable
fun SidebarRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val root = props as? JsonObject
    val collapseEvent = props.string("collapseChangeEvent")
    val closeEvent = props.string("closeEvent")
    val logoEvent = props.string("logoClickEvent")
    val brand = props.string("brandName", "KFlow")
    var internal by remember { mutableStateOf(props.bool("defaultCollapsed")) }
    val collapsed = if (root?.get("collapsed") != null) props.bool("collapsed") else internal
    val toggle = {
        if (root?.get("collapsed") == null) internal = !collapsed
        if (collapseEvent.isNotEmpty()) onEvent(collapseEvent, eventEnvelope(props, almadarJson("collapsed" to !collapsed)))
    }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "list", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")))
    } else {
        Column(
            modifier = modifier
                .width(if (collapsed) AlmadarSizes.w20 else AlmadarSizes.w64)
                .fillMaxHeight()
                .background(palette.card)
                .almadarLabel(props.string("aria-label", brand))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (root?.get("logo") as? JsonObject)?.let { RenderChild(it, Modifier, onEvent) }
                AlmadarButton(
                    props, if (collapsed) "" else brand, logoEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                    { name, _ -> if (logoEvent.isNotEmpty()) onEvent(name, eventEnvelope(props)) },
                    Modifier.weight(1f),
                    variant = "ghost", ariaLabel = brand, local = logoEvent.isEmpty()
                )
                if (!props.bool("hideCollapseButton")) {
                    AlmadarButton(
                        props, "", collapseEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                        { _, _ -> toggle() },
                        payload = almadarJson("collapsed" to !collapsed), variant = "ghost", size = "sm",
                        icon = if (collapsed) "chevron-right" else "chevron-left",
                        ariaLabel = if (collapsed) "Expand sidebar" else "Collapse sidebar", local = collapseEvent.isEmpty()
                    )
                }
                if (props.bool("showCloseButton")) {
                    AlmadarButton(props, "", closeEvent.ifEmpty { ALMADAR_LOCAL_ACTION }, onEvent, variant = "ghost", size = "sm", icon = "x", ariaLabel = "Close sidebar", local = closeEvent.isEmpty())
                }
            }
            AlmadarDivider()
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(almadarSpaceFor("sm")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
            ) {
                items.forEach { item ->
                    val route = almadarNavigate(item)
                    val active = item.bool("active", item.bool("isActive"))
                    val badge = almadarScalar(item["badge"]) ?: ""
                    Row(
                        modifier = Modifier.fillMaxWidth().verificationContainer(VerificationIds.row(item.string("id", item.string("label")))),
                        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlmadarButton(
                            props, if (collapsed) "" else item.string("label"), route.event.ifEmpty { ALMADAR_LOCAL_ACTION },
                            { name, envelope -> if (route.event.isNotEmpty()) onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload"))) },
                            Modifier.weight(1f),
                            payload = route.payload, variant = if (active) "primary" else "ghost", icon = item.string("icon"),
                            ariaLabel = item.string("label"), local = route.event.isEmpty()
                        )
                        if (!collapsed && badge.isNotEmpty()) AlmadarBadge(badge, variant = "danger")
                    }
                }
            }
            val footer = root?.get("footerContent")
            val user = root?.get("userSection")
            if (footer != null || user != null) {
                AlmadarDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("sm")),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlmadarNode(footer, onEvent)
                    AlmadarNode(user, onEvent)
                }
            }
        }
    }
}
