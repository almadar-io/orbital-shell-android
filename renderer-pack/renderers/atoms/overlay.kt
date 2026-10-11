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
import androidx.compose.ui.graphics.RectangleShape

// tier: atoms — a single Material 3 control

@Composable
fun OverlayRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val action = props.string("action")
    if (props.bool("isVisible", true)) {
        var scrim: Modifier = Modifier.fillMaxSize()
        if (props.bool("blur")) scrim = scrim.almadarBackdrop(RectangleShape, ALMADAR_OVERLAY_BLUR)
        scrim = scrim.background(LocalAlmadarTheme.current.color.scrim)
        if (action.isNotEmpty()) {
            scrim = scrim
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                    onEvent(action, eventEnvelope(props))
                }
                .almadarTag(almadarActionId(action, props))
        }
        Box(modifier = modifier.fillMaxSize()) {
            Box(modifier = scrim)
            props.children().forEach { RenderChild(it, Modifier, onEvent) }
        }
    }
}
