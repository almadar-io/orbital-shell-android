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
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.platform.LocalDensity

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

import androidx.compose.foundation.gestures.Orientation

@Composable
fun SplitPaneRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val horizontal = props.string("direction", "horizontal") == "horizontal"
    val initial = props.float("ratio", 50f)
    var ratio by remember(initial) { mutableStateOf(initial) }
    val resizable = props.bool("resizable", true)
    val density = LocalDensity.current
    val thickness = LocalAlmadarGeometry.current.borderHeavy
    val leftNode = (props as? JsonObject)?.get("left")
    val rightNode = (props as? JsonObject)?.get("right")
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        val total = if (horizontal) maxWidth else maxHeight
        val weighted = horizontal || constraints.hasBoundedHeight
        val totalPx = with(density) { total.toPx() }
        val minRatio = if (total.value > 0f) props.float("minSize", 100f) / total.value * 100f else 0f
        val drag = rememberDraggableState { delta ->
            if (totalPx > 0f) ratio = (ratio + delta / totalPx * 100f).coerceIn(minRatio, maxOf(minRatio, 100f - minRatio))
        }
        val handle: @Composable () -> Unit = {
            if (resizable) {
                Box(
                    modifier = (if (horizontal) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
                        .draggable(drag, if (horizontal) Orientation.Horizontal else Orientation.Vertical)
                        .padding(almadarSpaceFor("xs")),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = (if (horizontal) Modifier.fillMaxHeight().width(thickness) else Modifier.fillMaxWidth().height(thickness)).background(palette.border))
                }
            }
        }
        val first: @Composable (Modifier) -> Unit = { paneModifier -> Box(modifier = paneModifier) { AlmadarNode(leftNode, onEvent, Modifier.fillMaxWidth()) } }
        val second: @Composable (Modifier) -> Unit = { paneModifier -> Box(modifier = paneModifier) { AlmadarNode(rightNode, onEvent, Modifier.fillMaxWidth()) } }
        val share = ratio.coerceIn(0.01f, 99.99f)
        if (!weighted) {
            Column(modifier = Modifier.fillMaxWidth()) {
                first(Modifier.fillMaxWidth())
                handle()
                second(Modifier.fillMaxWidth())
            }
        } else if (horizontal) {
            Row(modifier = Modifier.fillMaxWidth()) {
                first(Modifier.weight(share))
                handle()
                second(Modifier.weight(100f - share))
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                first(Modifier.weight(share))
                handle()
                second(Modifier.weight(100f - share))
            }
        }
    }
}
