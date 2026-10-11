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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import coil.compose.AsyncImage
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

@Composable
fun ControlButtonRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val pressEvent = props.string("pressEvent")
    val releaseEvent = props.string("releaseEvent")
    val disabled = props.bool("disabled")
    val label = props.string("label")
    val iconName = props.string("icon")
    val asset = props.assetUrl("assetUrl")
    val spoken = props.string("aria-label", label.ifEmpty { iconName })
    var held by remember { mutableStateOf(false) }
    val pressed = props.bool("pressed", held)
    val base = LocalAlmadarIconography.current.defaultSize
    val extent = base * (when (props.string("size", "md")) { "sm" -> 2.5f; "lg" -> 4.5f; "xl" -> 6f; else -> 3.5f })
    val corner = if (props.string("shape", "circle") == "circle") AlmadarCorner.Pill else AlmadarCorner.Interactive
    val shape = almadarShape(corner)
    val variant = props.string("variant", "secondary")
    val container = when (variant) {
        "primary" -> if (pressed) palette.primaryHover else palette.primary
        "ghost" -> if (pressed) palette.muted else Color.Transparent
        else -> if (pressed) palette.secondaryHover else palette.secondary
    }
    val content = when (variant) {
        "primary" -> palette.primaryForeground
        "ghost" -> palette.foreground
        else -> palette.secondaryForeground
    }
    val press = {
        if (pressEvent.isNotEmpty()) onEvent(pressEvent, eventEnvelope(props))
    }
    val release = {
        if (releaseEvent.isNotEmpty()) onEvent(releaseEvent, eventEnvelope(props))
    }
    Surface(
        modifier = modifier
            .size(extent)
            .almadarAlpha(if (disabled) ALMADAR_DISABLED_ALPHA else 1f)
            .almadarTag(almadarActionId(pressEvent, props))
            .then(almadarBorder(corner, LocalAlmadarGeometry.current.borderHeavy, if (pressed) palette.foreground else if (variant == "primary") palette.primary else palette.border))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (spoken.isNotEmpty()) contentDescription = spoken
                onClick(label = spoken) { if (!disabled) { press(); release() }; true }
            }
            .pointerInput(disabled, pressEvent, releaseEvent) {
                detectTapGestures(onPress = {
                    if (!disabled) {
                        held = true
                        press()
                        tryAwaitRelease()
                        held = false
                        release()
                    }
                })
            },
        shape = shape,
        color = container,
        contentColor = content
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (asset.isNotEmpty()) AsyncImage(model = asset, imageLoader = rememberAlmadarImageLoader(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(base * 1.5f))
            else if (iconName.isNotEmpty()) AlmadarIcon(name = iconName, modifier = Modifier.size(base * 1.5f))
            else if (label.isNotEmpty()) Text(text = label, style = almadarType("base").copy(fontWeight = FontWeight.Bold))
        }
    }
}
