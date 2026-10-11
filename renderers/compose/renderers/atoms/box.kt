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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

// tier: atoms — a single Material 3 control

import androidx.compose.ui.draw.shadow

@Composable
fun BoxRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val action = props.string("action")
    val hoverEvent = props.string("hoverEvent")
    val padding = props.string("padding", "none")
    val padX = almadarSpaceFor(props.string("paddingX", padding))
    val padY = almadarSpaceFor(props.string("paddingY", padding))
    val margin = props.string("margin", "none")
    val marginX = almadarSpaceFor(props.string("marginX", margin))
    val marginY = almadarSpaceFor(props.string("marginY", margin))
    val bgToken = props.string("bg", "transparent")
    val background = when (bgToken) {
        "primary" -> palette.primary
        "secondary" -> palette.secondary
        "muted" -> palette.muted
        "accent" -> palette.accent
        "surface" -> palette.card
        "overlay" -> palette.card.copy(alpha = 0.8f)
        "inverse" -> palette.foreground
        else -> Color.Transparent
    }
    val foreground = when (bgToken) {
        "primary" -> palette.primaryForeground
        "secondary" -> palette.secondaryForeground
        "muted" -> palette.foreground
        "accent" -> palette.accentForeground
        "inverse" -> palette.background
        else -> LocalContentColor.current
    }
    val shape = when (props.string("rounded", "none")) {
        "none" -> almadarShape(AlmadarCorner.None)
        "sm" -> almadarShape(AlmadarCorner.Interactive)
        "full" -> almadarShape(AlmadarCorner.Pill)
        else -> almadarShape(AlmadarCorner.Container)
    }
    val borderCorner = when (props.string("rounded", "none")) {
        "none" -> AlmadarCorner.None
        "sm" -> AlmadarCorner.Interactive
        "full" -> AlmadarCorner.Pill
        else -> AlmadarCorner.Container
    }
    val elevation = LocalAlmadarElevation.current
    val shadowDepth = when (props.string("shadow", "none")) {
        "sm" -> elevation.cardElevation.radius
        "md" -> elevation.popoverElevation.radius
        "lg", "xl" -> elevation.dialogElevation.radius
        else -> Dp.Hairline
    }
    var styled: Modifier = Modifier
    if (props.bool("fullWidth")) styled = styled.fillMaxWidth()
    if (props.bool("fullHeight")) styled = styled.fillMaxHeight()
    styled = styled.padding(horizontal = marginX, vertical = marginY)
    if (shadowDepth > Dp.Hairline) styled = styled.shadow(shadowDepth, shape)
    styled = styled.clip(shape).background(background, shape)
    if (props.bool("border")) styled = styled.then(almadarBorder(borderCorner, LocalAlmadarGeometry.current.borderStandard, palette.border))
    if (action.isNotEmpty()) styled = styled.clickable { onEvent(action, eventEnvelope(props)) }.almadarTag(almadarActionId(action, props))
    if (hoverEvent.isNotEmpty()) {
        styled = styled.almadarHover(hoverEvent) { hovered ->
            onEvent(hoverEvent, eventEnvelope(props, JsonObject(mapOf("hovered" to JsonPrimitive(hovered)))))
        }
    }
    Box(modifier = modifier.almadarLabel(props.string("aria-label")), propagateMinConstraints = true) {
        CompositionLocalProvider(LocalContentColor provides foreground) {
            Column(modifier = styled.padding(horizontal = padX, vertical = padY)) {
                props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
            }
        }
    }
}
