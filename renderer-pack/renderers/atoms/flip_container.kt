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
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity

// tier: atoms — a single Material 3 control

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

@Composable
fun FlipContainerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val nodes = props.children()
    val flipped = props.bool("flipped")
    val clickEvent = props.string("onClick")
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = LocalAlmadarMotion.current.durationsMs["slow"] ?: 0),
        label = "flip-rotation"
    )
    val back = rotation > 90f
    val reach = 12f * LocalDensity.current.density
    var surface: Modifier = Modifier
        .fillMaxWidth()
        .graphicsLayer {
            rotationY = if (back) rotation - 180f else rotation
            cameraDistance = reach
        }
    if (clickEvent.isNotEmpty()) {
        surface = surface
            .clickable(role = Role.Button) { onEvent(clickEvent, eventEnvelope(props)) }
            .almadarTag(almadarActionId(clickEvent, props))
    }
    Box(modifier = modifier.almadarLabel(props.string("aria-label"))) {
        Column(modifier = surface) {
            val face = if (back && nodes.size > 1) nodes[1] else nodes.firstOrNull()
            if (face != null) RenderChild(face, Modifier.fillMaxWidth(), onEvent)
        }
    }
}
