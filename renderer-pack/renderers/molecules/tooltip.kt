package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.ui.input.pointer.pointerInput

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass

@Composable
fun TooltipRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    var visible by remember { mutableStateOf(false) }
    val tip = (props as? JsonObject)?.get("content")
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    waitForUpOrCancellation(PointerEventPass.Initial)
                }
                if (released == null) visible = true
            }
        }
    ) {
        props.children().forEach { RenderChild(it, Modifier, onEvent) }
        if (visible) {
            androidx.compose.ui.window.Popup(
                popupPositionProvider = AlmadarAnchoredPosition(props.string("position", "top")),
                onDismissRequest = { visible = false }
            ) {
                Box(modifier = Modifier.verificationWindowRoot().padding(almadarSpaceFor("sm"))) {
                    Surface(color = palette.primary, contentColor = palette.primaryForeground, shape = MaterialTheme.shapes.medium) {
                        Box(modifier = Modifier.widthIn(max = AlmadarSizes.maxWXs).padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm"))) {
                            if (tip is JsonPrimitive) AlmadarText(tip.contentOrNull ?: "", variant = "small", color = "inherit")
                            else AlmadarNode(tip, onEvent)
                        }
                    }
                }
            }
        }
    }
}
