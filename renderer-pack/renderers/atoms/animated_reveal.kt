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
fun AnimatedRevealRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val nodes = props.children()
    val revealEvent = props.string("revealEvent")
    val enterEvent = props.string("enterEvent")
    val leaveEvent = props.string("leaveEvent")
    DisposableEffect(enterEvent, leaveEvent) {
        if (enterEvent.isNotEmpty()) onEvent(enterEvent, eventEnvelope(props))
        onDispose { if (leaveEvent.isNotEmpty()) onEvent(leaveEvent, eventEnvelope(props)) }
    }
    val duration = props.int("duration", 600)
    val delay = props.int("delay", 0)
    val animation = props.string("animation", "fade-up")
    val animate = props.bool("animate", true)
    var visible by remember { mutableStateOf(!animate) }
    LaunchedEffect(animate) {
        if (animate) {
            kotlinx.coroutines.delay(delay.toLong())
            visible = true
        }
        if (revealEvent.isNotEmpty()) onEvent(revealEvent, eventEnvelope(props))
    }
    val spec = tween<Float>(durationMillis = duration)
    val offsetSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = duration)
    val enter = when (animation) {
        "fade" -> androidx.compose.animation.fadeIn(spec)
        "scale" -> androidx.compose.animation.fadeIn(spec) + androidx.compose.animation.scaleIn(spec, initialScale = 0.9f)
        "slide-left" -> androidx.compose.animation.fadeIn(spec) + androidx.compose.animation.slideInHorizontally(offsetSpec) { it / 4 }
        "slide-right" -> androidx.compose.animation.fadeIn(spec) + androidx.compose.animation.slideInHorizontally(offsetSpec) { -it / 4 }
        else -> androidx.compose.animation.fadeIn(spec) + androidx.compose.animation.slideInVertically(offsetSpec) { it / 4 }
    }
    androidx.compose.animation.AnimatedVisibility(visible = visible, enter = enter, modifier = modifier) {
        Column(modifier = Modifier.almadarTag(almadarActionId(revealEvent, props))) {
            nodes.forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
    }
}
