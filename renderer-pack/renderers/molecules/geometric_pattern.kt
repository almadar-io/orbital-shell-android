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

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

private const val GEOMETRIC_TILE_SIZE: Float = 60f

@Composable
private fun GeometricField(variant: String, tile: Float, stroke: Float, opacity: Float, fade: String, modifier: Modifier) {
    val ink = LocalAlmadarTheme.current.color.foreground
    val clear = ink.copy(alpha = 0f)
    val edge = tile.dp
    AlmadarBoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer(alpha = opacity, compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val mask: Brush? = when (fade) {
                    "left" -> Brush.horizontalGradient(0f to ink, 0.6f to clear, startX = 0f, endX = size.width)
                    "right" -> Brush.horizontalGradient(0.4f to clear, 1f to ink, startX = 0f, endX = size.width)
                    "dual" -> Brush.horizontalGradient(0f to ink, 0.35f to clear, 0.65f to clear, 1f to ink, startX = 0f, endX = size.width)
                    "around" -> Brush.radialGradient(0f to clear, 0.55f to clear, 0.85f to ink, 1f to ink, center = center, radius = size.maxDimension / 2f)
                    else -> null
                }
                if (mask != null) drawRect(brush = mask, blendMode = BlendMode.DstIn)
            }
    ) {
        val columns = Math.ceil((maxWidth / edge).toDouble()).toInt().coerceAtLeast(1)
        val rows = if (constraints.hasBoundedHeight) Math.ceil((maxHeight / edge).toDouble()).toInt().coerceAtLeast(1) else 1
        Column {
            repeat(rows) {
                Row {
                    repeat(columns) {
                        PatternTileRenderer(almadarJson("variant" to variant, "size" to tile.toInt(), "strokeWidth" to stroke), Modifier, AlmadarNoEvent)
                    }
                }
            }
        }
    }
}

@Composable
fun GeometricPatternRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val variant = props.string("variant", "star8")
    val mode = props.string("mode", "background")
    val opacity = props.float("opacity", 0.06f)
    val scale = props.float("scale", 1f)
    val stroke = props.float("strokeWidth", 0.5f)
    val tile = GEOMETRIC_TILE_SIZE * scale
    if (mode == "frame") {
        val strip = (tile * 2f / 3f).dp
        Column(modifier = modifier.fillMaxWidth()) {
            GeometricField(variant, tile, stroke, opacity, "", Modifier.fillMaxWidth().height(strip))
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
            GeometricField(variant, tile, stroke, opacity, "", Modifier.fillMaxWidth().height(strip))
        }
    } else {
        GeometricField(variant, tile, stroke, opacity, if (mode == "background") "" else mode, modifier.fillMaxSize())
    }
}
