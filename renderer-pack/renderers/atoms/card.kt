package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.border

// tier: atoms — a single Material 3 control

@Composable
fun CardRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val density = LocalAlmadarDensity.current
    val elevation = LocalAlmadarElevation.current
    val title = props.string("title")
    val subtitle = props.string("subtitle")
    val variant = props.string("variant", "bordered")
    val look = props.string("look", "elevated")
    val action = props.string("action")
    val loading = props.bool("loading")
    val padding = when (if (look == "tile-image-first") "none" else props.string("padding", "md")) {
        "none" -> Dp.Hairline
        "sm" -> density.cardPaddingSm
        "lg" -> density.cardPaddingLg
        else -> density.cardPaddingMd
    }
    val corner = when (look) {
        "chip" -> AlmadarCorner.Pill
        "borderless-divider" -> AlmadarCorner.None
        "invoice" -> AlmadarCorner.Interactive
        else -> AlmadarCorner.Container
    }
    val shape = almadarShape(corner)
    val depth = when (props.string("shadow", if (variant == "elevated" || variant == "interactive") "md" else "none")) {
        "sm" -> elevation.cardElevation.radius
        "md" -> elevation.popoverElevation.radius
        "lg" -> elevation.dialogElevation.radius
        else -> Dp.Hairline
    }
    val flat = look != "elevated"
    val borderWidth = if (look == "invoice") LocalAlmadarGeometry.current.borderHeavy else LocalAlmadarGeometry.current.borderStandard
    val border = when {
        look == "borderless-divider" -> Modifier
        flat || variant != "default" -> almadarBorder(corner, borderWidth, palette.border)
        else -> Modifier
    }
    val colors = CardDefaults.cardColors(containerColor = palette.card, contentColor = palette.cardForeground)
    val cardElevation = CardDefaults.cardElevation(defaultElevation = if (flat) Dp.Hairline else depth)
    val body: @Composable ColumnScope.() -> Unit = {
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.primary, trackColor = palette.muted)
        Column(modifier = Modifier.almadarSurfaceImage(LocalAlmadarSurface.current.cardImage, shape).padding(padding), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
            if (title.isNotEmpty()) Text(text = almadarHeadingText(title), style = almadarHeadingStyle(almadarType("lg")), color = palette.cardForeground)
            if (subtitle.isNotEmpty()) Text(text = subtitle, style = almadarType("sm"), color = palette.mutedForeground)
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
        if (look == "borderless-divider") HorizontalDivider(color = palette.border, thickness = LocalAlmadarGeometry.current.borderHairline)
    }
    Box(modifier = modifier.almadarLabel(props.string("aria-label")), propagateMinConstraints = true) {
        if (action.isNotEmpty()) {
            Card(
                onClick = { onEvent(action, eventEnvelope(props)) },
                modifier = Modifier.almadarSurfaceMaterial(shape).almadarTag(almadarActionId(action, props)).then(border),
                shape = shape, colors = colors, elevation = cardElevation, content = body
            )
        } else {
            Card(modifier = Modifier.almadarSurfaceMaterial(shape).then(border), shape = shape, colors = colors, elevation = cardElevation, content = body)
        }
    }
}
