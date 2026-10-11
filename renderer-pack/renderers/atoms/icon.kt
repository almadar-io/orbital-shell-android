package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.animation.core.*

// tier: atoms — a single Material 3 control

import androidx.compose.ui.draw.rotate

@Composable
fun IconRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val rawName = props.string("icon", props.string("name"))
    val tint = when (props.string("color")) {
        "primary" -> palette.primary
        "secondary" -> palette.secondary
        "muted" -> palette.mutedForeground
        "error" -> palette.error
        "success" -> palette.success
        "warning" -> palette.warning
        "info" -> palette.info
        "inherit" -> LocalContentColor.current
        else -> palette.foreground
    }
    val base = LocalAlmadarIconography.current.defaultSize
    val extent = when (props.string("size", "md")) {
        "xs" -> base * 0.75f
        "sm" -> base
        "lg" -> base * 1.5f
        "xl" -> base * 2f
        else -> base * 1.25f
    }
    val animation = props.string("animation", "none")
    val duration = LocalAlmadarMotion.current.durationsMs["slow"] ?: 0
    val transition = rememberInfiniteTransition(label = "icon-anim")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animation == "spin") 360f else 0f,
        animationSpec = infiniteRepeatable(tween(durationMillis = duration * 2, easing = LinearEasing)),
        label = "rotation"
    )
    val pulse by transition.animateFloat(
        initialValue = if (animation == "pulse") 0.4f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = duration * 2, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    AlmadarIcon(
        name = rawName,
        tint = tint,
        description = props.string("aria-label").ifEmpty { null },
        modifier = modifier
            .size(extent)
            .rotate(if (animation == "spin") rotation else 0f)
            .almadarAlpha(if (animation == "pulse") pulse else 1f)
    )
}
