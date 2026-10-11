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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// tier: templates — CONTENT ONLY

@Composable
fun AuthLayoutRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val density = LocalAlmadarDensity.current
    val root = props as? JsonObject
    val appName = props.string("appName", "My App")
    val logo = root?.get("logo")?.takeIf { it !is JsonNull }
    val branding = root?.get("brandingContent")?.takeIf { it !is JsonNull }
    val backdrop = props.string("backgroundImage")
    val home = { onEvent(NAVIGATE_EVENT, eventEnvelope(props, almadarJson("href" to "/", "url" to "/"))) }
    val mark: @Composable (Color, Color, Dp) -> Unit = { fill, ink, extent ->
        Row(
            modifier = Modifier
                .clickable(role = androidx.compose.ui.semantics.Role.Button) { home() }
                .almadarActionTag(NAVIGATE_EVENT, props),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (logo != null) {
                AlmadarNode(logo, onEvent)
            } else {
                Box(
                    modifier = Modifier.size(extent).clip(androidx.compose.foundation.shape.RoundedCornerShape(geometry.radiusContainer)).background(fill),
                    contentAlignment = Alignment.Center
                ) { AlmadarColoredText(appName.take(1).uppercase(), ink, sizeKey = "lg", weight = FontWeight.Bold) }
            }
            AlmadarColoredText(appName, if (logo != null || fill == palette.primary) palette.foreground else palette.primaryForeground, sizeKey = "2xl", weight = FontWeight.Bold)
        }
    }
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxSize().almadarLabel(props.string("aria-label"))) {
        val wide = maxWidth >= AlmadarBreakpoints.lg
        Row(modifier = Modifier.fillMaxSize()) {
            if (props.bool("showBranding", true) && wide) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(palette.primary)) {
                    if (backdrop.isNotEmpty()) {
                        ImageRenderer(almadarJson("src" to backdrop, "rounded" to "none"), Modifier.fillMaxSize(), AlmadarNoEvent)
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            androidx.compose.ui.graphics.Brush.linearGradient(listOf(palette.primary.copy(alpha = 0.9f), palette.primaryHover.copy(alpha = 0.9f)))
                        )
                    )
                    Column(
                        modifier = Modifier.fillMaxSize().padding(almadarSpaceFor("2xl")),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        mark(palette.primaryForeground.copy(alpha = 0.2f), palette.primaryForeground, density.buttonHeightMd)
                        if (branding != null) {
                            AlmadarNode(branding, onEvent)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("lg"))) {
                                AlmadarColoredText("Welcome to " + appName, palette.primaryForeground, sizeKey = "4xl", heading = true)
                                AlmadarColoredText(
                                    "Sign in to access your dashboard and manage your account.",
                                    palette.primaryForeground.copy(alpha = 0.8f), sizeKey = "lg"
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().background(palette.background).verticalScroll(rememberScrollState()).padding(almadarSpaceFor(if (wide) "2xl" else "lg")),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.widthIn(max = AlmadarSizes.maxWMd).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!wide) {
                        Box(modifier = Modifier.padding(bottom = almadarSpaceFor("xl"))) { mark(palette.primary, palette.primaryForeground, density.buttonHeightLg) }
                    }
                    props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
                }
            }
        }
    }
}
