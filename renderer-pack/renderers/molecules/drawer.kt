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
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.RectangleShape

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun DrawerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val titleNode = root?.get("title")
    val hasTitle = titleNode != null && titleNode !is JsonNull && !(titleNode is JsonPrimitive && titleNode.content.isEmpty())
    val closeEvent = props.string("closeEvent")
    val showClose = props.bool("showCloseButton", true) && closeEvent.isNotEmpty()
    val width = props.string("width", "md")
    val cap = width.removeSuffix("px").toFloatOrNull()?.let { Dp(it) } ?: if (width == "full" || LocalConfiguration.current.screenWidthDp.dp < AlmadarBreakpoints.sm) Dp.Infinity else when (width) {
        "sm" -> AlmadarSizes.w80
        "lg" -> AlmadarSizes.maxWLg
        "xl" -> AlmadarSizes.maxW2xl
        else -> AlmadarSizes.w96
    }
    val footer = root?.get("footer")
    val left = props.string("position", "right") == "left"
    if (!props.bool("isOpen", true)) {
        Box(modifier = modifier)
    } else {
        Box(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), contentAlignment = if (left) Alignment.TopStart else Alignment.TopEnd) {
            Surface(
                modifier = Modifier.almadarSurfaceMaterial(RectangleShape).widthIn(max = cap).fillMaxWidth(),
                color = palette.surface,
                contentColor = palette.foreground,
                border = androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderStandard, palette.border)
            ) {
                Column(modifier = Modifier.almadarSurfaceImage(LocalAlmadarSurface.current.cardImage, RectangleShape)) {
                    if (hasTitle || showClose) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("lg"), vertical = almadarSpaceFor("md")),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasTitle) AlmadarNode(titleNode, onEvent, Modifier.weight(1f), variant = "h4")
                            else Spacer(modifier = Modifier.weight(1f))
                            if (showClose) {
                                AlmadarButton(props, "", closeEvent, onEvent, variant = "ghost", size = "sm", icon = "x", ariaLabel = "Close drawer")
                            }
                        }
                        AlmadarDivider()
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp).verticalScroll(rememberScrollState()).padding(almadarSpaceFor("lg")),
                        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
                    ) {
                        props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
                    }
                    if (footer != null && footer !is JsonNull) {
                        AlmadarDivider()
                        Box(modifier = Modifier.fillMaxWidth().background(palette.muted).padding(horizontal = almadarSpaceFor("lg"), vertical = almadarSpaceFor("md"))) {
                            AlmadarNode(footer, onEvent, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
