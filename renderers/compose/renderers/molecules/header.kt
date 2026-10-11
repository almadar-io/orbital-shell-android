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

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role

@Composable
fun HeaderRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val width = LocalConfiguration.current.screenWidthDp
    val variant = props.string("variant", "mobile")
    val look = props.string("look", "compact-bar")
    val brandName = props.string("brandName", "KFlow")
    val brandHref = props.string("brandHref")
    val logoNode = root?.get("logo")
    val logoSource = props.string("logoSrc")
    val items = props.array("navigationItems").mapNotNull { it as? JsonObject }
    val actionsNode = root?.get("actions")
    val avatar = props.jsonObject("userAvatar")
    val userName = props.string("userName")
    val vertical = when (look) {
        "hero" -> almadarSpaceFor("2xl")
        "editorial-banner" -> almadarSpaceFor("xl")
        "breadcrumb" -> almadarSpaceFor("xs")
        "contextual" -> almadarSpaceFor("sm")
        else -> almadarSpaceFor("sm")
    }
    val minHeight = when (look) {
        "hero" -> LocalAlmadarDensity.current.rowHeightSpacious * 4
        "editorial-banner" -> LocalAlmadarDensity.current.rowHeightSpacious * 2
        else -> LocalAlmadarDensity.current.buttonHeightLg
    }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "card", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier)
    } else if (variant == "mobile" && width.dp >= AlmadarBreakpoints.lg) {
        Box(modifier = modifier)
    } else {
        Surface(
            modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")),
            color = palette.card,
            contentColor = palette.foreground
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).padding(horizontal = almadarSpaceFor("md"), vertical = vertical),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .then(if (brandHref.isEmpty()) Modifier else Modifier.clickable(role = Role.Button) {
                                onEvent(NAVIGATE_EVENT, eventEnvelope(props, almadarJson("href" to brandHref, "url" to brandHref)))
                            }.almadarActionTag(NAVIGATE_EVENT, props))
                            .almadarLabel(if (brandHref.isEmpty()) "" else brandName),
                        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (logoNode != null && logoNode !is JsonNull && !(logoNode is JsonPrimitive && logoNode.content.isNotEmpty())) {
                            AlmadarNode(logoNode, onEvent)
                        } else {
                            val source = (logoNode as? JsonPrimitive)?.contentOrNull ?: logoSource
                            if (source.isNotEmpty()) {
                                ImageRenderer(almadarJson("src" to source, "alt" to brandName, "fit" to "contain", "rounded" to "none"), Modifier.width(LocalAlmadarDensity.current.rowHeightSpacious).height(LocalAlmadarDensity.current.buttonHeightMd), AlmadarNoEvent)
                            }
                        }
                        if (brandName.isNotEmpty()) AlmadarText(brandName, variant = "h5", truncate = true)
                    }
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"), Alignment.CenterHorizontally)) {
                        if (variant == "desktop" && width.dp >= AlmadarBreakpoints.md) {
                            items.forEach { item ->
                                TopNavItemRenderer(almadarJson("item" to item, "tone" to "soft", "sourceTrait" to almadarTrait(props)), Modifier, onEvent)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")), verticalAlignment = Alignment.CenterVertically) {
                        if (actionsNode != null && actionsNode !is JsonNull) AlmadarNode(actionsNode, onEvent)
                        if (avatar != null || userName.isNotEmpty()) {
                            AvatarRenderer(
                                almadarJson(
                                    "src" to (avatar?.string("src") ?: ""), "alt" to (avatar?.string("alt") ?: userName), "name" to userName,
                                    "initials" to (avatar?.string("initials") ?: ""), "size" to "sm"
                                ),
                                Modifier, AlmadarNoEvent
                            )
                        }
                    }
                }
                AlmadarDivider()
            }
        }
    }
}
