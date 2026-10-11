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

@Composable
fun PresenceRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val nodes = props.children()
    val show = props.bool("show", true)
    val animate = props.bool("animate", true)
    val motion = LocalAlmadarMotion.current
    val duration = if (animate) (motion.durationsMs["normal"] ?: 0) else 0
    val spec = tween<Float>(durationMillis = duration)
    val offsetSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = duration)
    val slides = props.string("animation") == "slide-up" || props.string("animation") == "drawer" || props.string("animation") == "toast"
    val enter = if (slides) androidx.compose.animation.fadeIn(spec) + androidx.compose.animation.slideInVertically(offsetSpec) { it / 4 } else androidx.compose.animation.fadeIn(spec)
    val exit = if (slides) androidx.compose.animation.fadeOut(spec) + androidx.compose.animation.slideOutVertically(offsetSpec) { it / 4 } else androidx.compose.animation.fadeOut(spec)
    androidx.compose.animation.AnimatedVisibility(visible = show, enter = enter, exit = exit, modifier = modifier) {
        Column { nodes.forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) } }
    }
}
