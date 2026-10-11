package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/** `ScaledDiagram.tsx` MIN_DIAGRAM_WIDTH: a narrower diagram is not one worth scaling. */
private const val MIN_DIAGRAM_WIDTH = 200

@Composable
fun ScaledDiagramRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    // `ScaledDiagram.tsx`: the children lay out at their natural size and scale down to the column (never up)
    // once that width reaches MIN_DIAGRAM_WIDTH, anchored at the leading top; the box is as tall as the scaled content.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val minDiagramWidth = with(LocalDensity.current) { MIN_DIAGRAM_WIDTH.dp.roundToPx() }
    CompositionLocalProvider(LocalAlmadarCanvasNaturalSize provides true) {
        Layout(
            content = { Column { props.children().forEach { RenderChild(it, Modifier, onEvent) } } },
            modifier = modifier.fillMaxWidth().clipToBounds(),
        ) { measurables, constraints ->
            val diagram = measurables.first().measure(Constraints(maxHeight = constraints.maxHeight))
            val scale = if (diagram.width >= minDiagramWidth && constraints.hasBoundedWidth) minOf(1f, constraints.maxWidth.toFloat() / diagram.width) else 1f
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else diagram.width
            layout(width, constraints.constrainHeight((diagram.height * scale).roundToInt())) {
                diagram.placeRelativeWithLayer(0, 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(if (rtl) 1f else 0f, 0f)
                }
            }
        }
    }
}
