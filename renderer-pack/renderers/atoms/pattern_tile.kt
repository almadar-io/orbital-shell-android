package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: atoms — a single Material 3 control

@Composable
fun PatternTileRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val variant = props.string("variant", "star8")
    val extent = props.int("size", 60).dp
    val stroke = props.float("strokeWidth", 0.5f)
    val color = palette.primary
    val points = when (variant) {
        "star6" -> 6
        "star10" -> 10
        "star12" -> 12
        else -> 8
    }
    androidx.compose.foundation.Canvas(modifier = modifier.size(extent).almadarLabel(props.string("aria-label"))) {
        val radius = size.minDimension / 2f * 0.9f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke * density)
        if (variant.startsWith("rosette")) {
            for (ring in 1..3) {
                drawCircle(color = color, radius = radius * ring / 3f, center = androidx.compose.ui.geometry.Offset(centerX, centerY), style = style)
            }
        } else {
            val path = androidx.compose.ui.graphics.Path()
            val skip = if (points % 2 == 0) points / 2 - 1 else points / 2
            for (index in 0..points) {
                val angle = 2.0 * Math.PI * ((index * skip) % points) / points - Math.PI / 2.0
                val x = centerX + radius * Math.cos(angle).toFloat()
                val y = centerY + radius * Math.sin(angle).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = color, style = style)
            drawCircle(color = color, radius = radius, center = androidx.compose.ui.geometry.Offset(centerX, centerY), style = style)
        }
    }
}
