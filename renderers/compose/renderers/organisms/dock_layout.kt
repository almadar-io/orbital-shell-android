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

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

private fun dockCollapsed(root: JsonObject?, key: String, local: Boolean): Boolean =
    (root?.get(key) as? JsonPrimitive)?.booleanOrNull ?: local

@Composable
fun DockLayoutRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val node: (String) -> JsonElement? = { key -> root?.get(key)?.takeIf { it !is JsonNull } }
    val rail = node("rail")
    val sidebar = node("sidebar")
    val main = node("main")
    val bottomPanel = node("bottomPanel")
    val statusBar = node("statusBar")
    val secondary = node("secondarySidebar")
    val topBarActions = node("topBarActions")
    var sidebarLocal by remember { mutableStateOf(false) }
    var bottomLocal by remember { mutableStateOf(false) }
    var secondaryLocal by remember { mutableStateOf(false) }
    var drawer by remember { mutableStateOf("") }
    var leftView by remember { mutableStateOf("menu") }
    val sidebarCollapsed = dockCollapsed(root, "sidebarCollapsed", sidebarLocal)
    val bottomCollapsed = dockCollapsed(root, "bottomPanelCollapsed", bottomLocal)
    val secondaryCollapsed = dockCollapsed(root, "secondarySidebarCollapsed", secondaryLocal)
    val sidebarEvent = props.string("onSidebarCollapsedChange")
    val bottomEvent = props.string("onBottomPanelCollapsedChange")
    val secondaryEvent = props.string("onSecondarySidebarCollapsedChange")
    val toggle: @Composable (String, String, String, Boolean, (Boolean) -> Unit) -> Unit = { event, openIcon, closedIcon, collapsed, assign ->
        AlmadarButton(
            props, "", event.ifEmpty { ALMADAR_LOCAL_ACTION },
            { _, _ ->
                assign(!collapsed)
                if (event.isNotEmpty()) onEvent(event, eventEnvelope(props, almadarJson("collapsed" to !collapsed)))
            },
            variant = "ghost", icon = if (collapsed) closedIcon else openIcon, size = "sm",
            ariaLabel = if (collapsed) "Expand panel" else "Collapse panel", local = event.isEmpty()
        )
    }
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxSize().almadarLabel(props.string("aria-label"))) {
        val compact = if (root?.containsKey("compact") == true) props.bool("compact") else maxWidth < AlmadarBreakpoints.lg
        if (main == null) {
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        } else if (compact) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (rail != null || sidebar != null || secondary != null || bottomPanel != null || topBarActions != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(palette.surface).padding(horizontal = almadarSpaceFor("sm"), vertical = almadarSpaceFor("xs")),
                            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (rail != null || sidebar != null) {
                                AlmadarLocalButton(props, "", { leftView = if (rail != null) "menu" else "panel"; drawer = "left" }, variant = "ghost", icon = "menu", ariaLabel = "Menu")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (bottomPanel != null) {
                                AlmadarLocalButton(props, "", { drawer = "bottom" }, variant = "ghost", icon = props.string("bottomPanelIcon", "panel-bottom-open"), ariaLabel = props.string("bottomPanelLabel", "Expand bottom panel"))
                            }
                            if (secondary != null) {
                                AlmadarLocalButton(props, "", { drawer = "secondary" }, variant = "ghost", icon = props.string("secondarySidebarIcon", "panel-right-open"), ariaLabel = props.string("secondarySidebarLabel", "Expand right panel"))
                            }
                            AlmadarNode(topBarActions, onEvent)
                        }
                        AlmadarDivider()
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) { AlmadarNode(main, onEvent, Modifier.fillMaxSize()) }
                    if (statusBar != null) {
                        AlmadarDivider()
                        AlmadarNode(statusBar, onEvent, Modifier.fillMaxWidth())
                    }
                }
                if (drawer.isNotEmpty()) {
                    val left = drawer == "left"
                    val heading = when (drawer) {
                        "left" -> "Menu"
                        "secondary" -> props.string("secondarySidebarLabel", "Expand right panel")
                        else -> props.string("bottomPanelLabel", "Expand bottom panel")
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().background(palette.scrim),
                        contentAlignment = if (left) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Column(modifier = Modifier.fillMaxHeight().widthIn(max = AlmadarSizes.w96).fillMaxWidth(0.85f).background(palette.surface)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (left && leftView == "panel" && rail != null) {
                                        AlmadarLocalButton(props, "", { leftView = "menu" }, variant = "ghost", icon = "arrow-left", size = "sm", ariaLabel = "Back to the menu")
                                    }
                                    AlmadarText(heading, variant = "h5")
                                }
                                AlmadarLocalButton(props, "", { drawer = "" }, variant = "ghost", icon = "x", size = "sm", ariaLabel = "Close")
                            }
                            AlmadarDivider()
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                                when (drawer) {
                                    "left" -> if (leftView == "menu" && rail != null) AlmadarNode(rail, onEvent) else AlmadarNode(sidebar, onEvent)
                                    "secondary" -> AlmadarNode(secondary, onEvent)
                                    else -> AlmadarNode(bottomPanel, onEvent)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val showSidebar = sidebar != null && !sidebarCollapsed
            val showBottom = bottomPanel != null && !bottomCollapsed
            val showSecondary = secondary != null && !secondaryCollapsed
            val sidebarShare = props.int("sidebarWidth", 20).coerceIn(1, 99)
            val bottomShare = props.int("bottomPanelHeight", 30).coerceIn(1, 99)
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().weight(if (showBottom) (100 - bottomShare).toFloat() else 1f)) {
                    if (rail != null) {
                        val railWidth = props.int("railWidth", 0)
                        Box(
                            modifier = (if (railWidth > 0) Modifier.width(railWidth.dp) else Modifier).fillMaxHeight().verticalScroll(rememberScrollState())
                        ) { AlmadarNode(rail, onEvent) }
                        AlmadarDivider(vertical = true)
                    }
                    if (sidebar != null) {
                        if (showSidebar) {
                            Box(modifier = Modifier.weight(sidebarShare.toFloat()).widthIn(min = props.int("sidebarMinSize", 0).dp).fillMaxHeight()) {
                                AlmadarNode(sidebar, onEvent, Modifier.fillMaxSize())
                            }
                        }
                        Column(modifier = Modifier.fillMaxHeight().background(palette.surface).padding(almadarSpaceFor("xs"))) {
                            toggle(sidebarEvent, "panel-left-close", "panel-left-open", sidebarCollapsed) { sidebarLocal = it }
                        }
                    }
                    Row(modifier = Modifier.weight(if (showSidebar) (100 - sidebarShare).toFloat() else 1f).fillMaxHeight()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { AlmadarNode(main, onEvent, Modifier.fillMaxSize()) }
                        if (secondary != null) {
                            Column(modifier = Modifier.fillMaxHeight().background(palette.surface).padding(almadarSpaceFor("xs"))) {
                                toggle(secondaryEvent, "panel-right-close", "panel-right-open", secondaryCollapsed) { secondaryLocal = it }
                            }
                            if (showSecondary) {
                                Box(modifier = Modifier.width(props.int("secondarySidebarWidth", 280).dp).fillMaxHeight()) {
                                    AlmadarNode(secondary, onEvent, Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
                if (bottomPanel != null) {
                    AlmadarDivider()
                    Row(modifier = Modifier.fillMaxWidth().background(palette.surface), horizontalArrangement = Arrangement.End) {
                        toggle(bottomEvent, "panel-bottom-close", "panel-bottom-open", bottomCollapsed) { bottomLocal = it }
                    }
                    if (showBottom) {
                        Box(modifier = Modifier.fillMaxWidth().weight(bottomShare.toFloat())) { AlmadarNode(bottomPanel, onEvent, Modifier.fillMaxSize()) }
                    }
                }
                if (statusBar != null) {
                    AlmadarDivider()
                    AlmadarNode(statusBar, onEvent, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
