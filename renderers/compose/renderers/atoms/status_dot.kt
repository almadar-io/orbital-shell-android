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
import androidx.compose.foundation.background

// tier: atoms — a single Material 3 control

@Composable
fun StatusDotRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val status = props.string("status", "offline")
    val color = if (status == "offline") palette.mutedForeground else almadarTone(status).solid
    val extent = almadarSpace(when (props.string("size", "md")) { "sm" -> "space2"; "lg" -> "space3"; else -> "space2" }) * (if (props.string("size", "md") == "md") 1.25f else 1f)
    val pulse = rememberInfiniteTransition(label = "status-pulse")
    val level by pulse.animateFloat(
        initialValue = if (props.bool("pulse")) 0.4f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = (LocalAlmadarMotion.current.durationsMs["slow"] ?: 0) * 2), repeatMode = RepeatMode.Reverse),
        label = "status-pulse-level"
    )
    Box(
        modifier = modifier
            .size(extent)
            .almadarAlpha(level)
            .background(color, MaterialTheme.shapes.large)
            .almadarStatus(props.string("label", props.string("aria-label", status)))
    )
}
