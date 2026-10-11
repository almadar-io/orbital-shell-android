package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape

// tier: templates — CONTENT ONLY

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

@Composable
fun DashboardLayoutRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val density = LocalAlmadarDensity.current
    val appName = props.string("appName")
    val layoutMode = props.string("layoutMode", "sidebar")
    val navItems = props.array("navItems").mapNotNull { it as? JsonObject }
    val user = props.jsonObject("user")
    val searchEvent = props.string("searchEvent")
    val showSearch = props.bool("showSearch") || searchEvent.isNotEmpty()
    val topBarActions = props.array("topBarActions").mapNotNull { it as? JsonObject }
    val hasNotifications = (props as? JsonObject)?.get("notifications") is JsonArray
    val notificationClickEvent = props.string("notificationClickEvent")
    val currentPath = props.string("currentPath")
    val userMenuItems = props.array("userMenuItems").mapNotNull { it as? JsonObject }
    val signOutEvent = props.string("onSignOut")
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val sidebarWidth = density.buttonHeightLg * 6
    val compact = maxWidth < sidebarWidth * 2
    val offCanvas = layoutMode != "minimal" && layoutMode != "bottomnav" && (layoutMode == "drawer" || compact)
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(currentPath) { menuOpen = false }
    val expanded = remember { mutableStateListOf<String>() }
    // The web sidebar item: active (its href or a child's) is primary; children expand inline under a chevron.
    @Composable
    fun navEntry(item: JsonObject) {
        val href = item.string("href")
        val children = item.array("children").mapNotNull { it as? JsonObject }
        val childActive = children.any { it.string("href").isNotEmpty() && it.string("href") == currentPath }
        val active = (href.isNotEmpty() && href == currentPath) || childActive
        val key = href.ifEmpty { item.string("label") }
        val open = key in expanded || childActive
        val badge = almadarScalar(item["badge"]) ?: ""
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
            Row(modifier = Modifier.fillMaxWidth().semantics { selected = active }, horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1")), verticalAlignment = Alignment.CenterVertically) {
                if (children.isEmpty()) {
                    val route = almadarNavigate(item)
                    AlmadarButton(props, item.string("label"), route.event, onEvent, modifier = Modifier.weight(1f), payload = route.payload, variant = if (active) "primary" else "ghost", icon = item.string("icon"))
                } else {
                    ButtonRenderer(
                        almadarJson(
                            "label" to item.string("label"), "action" to ALMADAR_LOCAL_ACTION, "variant" to (if (active) "primary" else "ghost"),
                            "leftIcon" to item.string("icon"), "rightIcon" to (if (open) "chevron-up" else "chevron-down"), "local" to true, "sourceTrait" to almadarTrait(props)
                        ),
                        Modifier.weight(1f).semantics { stateDescription = if (open) "expanded" else "collapsed" },
                        { _, _ -> if (key in expanded) expanded.remove(key) else expanded.add(key) }
                    )
                }
                if (badge.isNotEmpty()) AlmadarBadge(badge, variant = if (active) "primary" else "default")
            }
            if (open) Column(modifier = Modifier.fillMaxWidth().padding(start = almadarSpace("space4")), verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                children.forEach { navEntry(it) }
            }
        }
    }
    val slotNode: @Composable (String, Modifier) -> Unit = { key, slotModifier ->
        when (val node = (props as? JsonObject)?.get(key)) {
            is JsonObject -> RenderChild(node, slotModifier, onEvent)
            is JsonArray -> Row(modifier = slotModifier) { node.forEach { RenderChild(it, Modifier, onEvent) } }
            else -> Unit
        }
    }
    val topBar: @Composable () -> Unit = {
        var query by remember { mutableStateOf("") }
        var searchOpen by remember { mutableStateOf(false) }
        val notificationList = (props as? JsonObject)?.get("notifications") as? JsonArray
        val unread = notificationList?.count { ((it as? JsonObject)?.get("read") as? JsonPrimitive)?.booleanOrNull != true } ?: 0
        Column(modifier = Modifier.fillMaxWidth().almadarMaterialFill(RectangleShape, palette.card)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = almadarSpace("space12") + almadarSpace("space4")).padding(horizontal = almadarSpace(if (compact) "space3" else "space4")),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(almadarSpace(if (compact) "space2" else "space4"))
            ) {
                if (offCanvas) {
                    AlmadarLocalButton(
                        props, "", { menuOpen = !menuOpen },
                        variant = "ghost", icon = if (menuOpen) "x" else "menu",
                        ariaLabel = if (menuOpen) "Close menu" else "Open menu"
                    )
                }
                slotNode("logo", Modifier)
                val searching = showSearch && (!compact || searchOpen)
                if (compact && !searchOpen || (!compact && offCanvas)) {
                    AlmadarText(appName, variant = "label", weight = "medium", truncate = true, modifier = if (compact) Modifier.weight(1f) else Modifier)
                }
                if (searching) {
                    Box(modifier = Modifier.weight(1f)) {
                        AlmadarInput(
                            name = "search", value = query, onChange = { query = it },
                            modifier = Modifier.widthIn(max = if (compact) Dp.Infinity else AlmadarSizes.maxWMd).fillMaxWidth(),
                            placeholder = "Search", leftIcon = "search", submitEvent = "search-submit",
                            onSubmit = { text -> if (searchEvent.isNotEmpty()) onEvent(searchEvent, eventEnvelope(props, almadarJson("value" to text))) }
                        )
                    }
                } else if (!compact) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (showSearch && compact) {
                    AlmadarLocalButton(
                        props, "", { searchOpen = !searchOpen },
                        variant = "ghost", icon = if (searchOpen) "x" else "search",
                        ariaLabel = "Search"
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                    slotNode("headerActions", Modifier)
                    if (props.bool("showThemeToggle", true)) ThemeToggleRenderer(almadarJson(), Modifier, onEvent)
                    topBarActions.forEach { action ->
                        AlmadarButton(
                            props, "", action.string("event", if (action.string("navigatesTo").isNotEmpty()) NAVIGATE_EVENT else ""), onEvent,
                            payload = if (action.string("navigatesTo").isNotEmpty()) almadarJson("url" to action.string("navigatesTo")) else action,
                            variant = "ghost", icon = action.string("icon"), ariaLabel = action.string("label")
                        )
                    }
                    if (hasNotifications) {
                        Box {
                            AlmadarButton(props, "", notificationClickEvent, onEvent, variant = "ghost", icon = "bell", ariaLabel = "Notifications")
                            if (unread > 0) AlmadarBadge(if (unread > 99) "99+" else unread.toString(), modifier = Modifier.align(Alignment.TopEnd), variant = "danger")
                        }
                    }
                    if (user != null && user.string("name").isNotEmpty()) {
                        val menuItems = userMenuItems.mapIndexed { index, action ->
                            val destination = action.string("navigatesTo")
                            almadarJson(
                                "id" to action.string("event", destination.ifEmpty { "entry" }) + "-" + index, "label" to action.string("label"),
                                "icon" to action.string("icon"), "event" to action.string("event"), "href" to destination,
                                "variant" to (if (action.string("variant") == "danger") "danger" else "")
                            )
                        } + almadarJson("id" to "sign-out", "label" to "Sign out", "icon" to "log-out", "variant" to "danger", "event" to SIGN_OUT_EVENT)
                        val trigger = almadarJson(
                            "type" to "hstack", "gap" to "sm", "align" to "center",
                            "children" to JsonArray(listOfNotNull(
                                almadarJson("type" to "avatar", "name" to user.string("name"), "src" to user.string("avatar"), "size" to "sm"),
                                if (compact) null else almadarJson("type" to "typography", "content" to user.string("name"), "variant" to "small", "weight" to "medium"),
                                if (compact) null else almadarJson("type" to "icon", "icon" to "chevron-down", "size" to "sm", "color" to "muted")
                            ))
                        )
                        val header = almadarJson(
                            "type" to "vstack",
                            "children" to JsonArray(listOfNotNull(
                                almadarJson("type" to "typography", "content" to user.string("name"), "variant" to "small", "weight" to "medium"),
                                if (user.string("email").isEmpty()) null else almadarJson("type" to "typography", "content" to user.string("email"), "variant" to "caption", "color" to "muted")
                            ))
                        )
                        MenuRenderer(almadarJson("trigger" to trigger, "header" to header, "items" to JsonArray(menuItems), "position" to "bottom-end", "aria-label" to "Account menu"), Modifier) { event, payload ->
                            // Sign out: the trait's declared reaction first, then the host ends the session.
                            val action = (payload as? JsonObject)?.get("actionPayload")
                            if (event == SIGN_OUT_EVENT && signOutEvent.isNotEmpty()) onEvent(signOutEvent, eventEnvelope(props, action))
                            onEvent(event, eventEnvelope(props, action))
                        }
                    }
                }
            }
            AlmadarDivider()
        }
    }
    val navPanel: @Composable BoxScope.() -> Unit = {
        Column(modifier = Modifier.align(Alignment.TopStart).width(sidebarWidth).fillMaxHeight().almadarMaterialFill(RectangleShape, palette.card)) {
            navItems.forEach { navEntry(it) }
            slotNode("sidebarContent", Modifier.fillMaxWidth())
            slotNode("sidebarFooter", Modifier.fillMaxWidth())
        }
    }
    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        Box(modifier = contentModifier) {
            Column(modifier = Modifier.fillMaxWidth().padding(almadarSpace(if (offCanvas) "space3" else "space6"))) {
                props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
            }
            if (menuOpen && offCanvas) navPanel()
        }
    }
    when (layoutMode) {
        "topnav" -> Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            if (!offCanvas) Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                navItems.forEach { item -> TopNavItemRenderer(almadarJson("item" to item, "activeHref" to currentPath, "tone" to "solid", "sourceTrait" to almadarTrait(props)), Modifier, onEvent) }
            }
            content(Modifier.weight(1f).fillMaxWidth())
        }
        "bottomnav" -> Column(modifier = Modifier.fillMaxSize()) {
            content(Modifier.weight(1f).fillMaxWidth())
            Column(modifier = Modifier.fillMaxWidth().almadarMaterialFill(RectangleShape, palette.card)) {
                AlmadarDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("sm")),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    navItems.forEach { item ->
                        val active = item.string("href").isNotEmpty() && item.string("href") == currentPath
                        val route = almadarNavigate(item)
                        StackRenderer(
                            almadarJson(
                                "direction" to "vertical", "align" to "center", "gap" to "none", "action" to route.event, "actionPayload" to route.payload, "sourceTrait" to almadarTrait(props),
                                "children" to JsonArray(listOf(
                                    almadarJson("type" to "icon", "icon" to item.string("icon"), "size" to "md", "color" to if (active) "primary" else "muted"),
                                    almadarJson("type" to "typography", "content" to item.string("label"), "variant" to "caption", "color" to if (active) "primary" else "muted")
                                ))
                            ),
                            Modifier.weight(1f).semantics { selected = active },
                            onEvent
                        )
                    }
                }
            }
        }
        "minimal" -> content(Modifier.fillMaxSize())
        else -> if (offCanvas) Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            content(Modifier.weight(1f).fillMaxWidth())
        } else Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.width(sidebarWidth).fillMaxHeight().almadarMaterialFill(RectangleShape, palette.card)) {
                AlmadarText(appName, variant = "h5", modifier = Modifier.padding(density.cardPaddingMd))
                navItems.forEach { navEntry(it) }
                if ((props as? JsonObject)?.get("sidebarContent") != null) {
                    slotNode("sidebarContent", Modifier.fillMaxWidth().weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                slotNode("sidebarFooter", Modifier.fillMaxWidth())
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                topBar()
                content(Modifier.fillMaxWidth())
            }
        }
    }
    }
}
