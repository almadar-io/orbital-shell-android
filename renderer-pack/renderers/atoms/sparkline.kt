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
fun SparklineRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val points = props.array("data").mapNotNull { item ->
        (item as? JsonPrimitive)?.contentOrNull?.toFloatOrNull()
            ?: (item as? JsonObject)?.get("value")?.let { (it as? JsonPrimitive)?.contentOrNull?.toFloatOrNull() }
    }
    val given = props.string("color", "auto")
    val color = when {
        given == "auto" && points.size > 1 -> if (points.last() >= points.first()) palette.success else palette.error
        given == "auto" -> palette.mutedForeground
        else -> almadarTone(given).solid
    }
    val strokeWidth = props.float("strokeWidth", 2f)
    val filled = props.bool("fill")
    val box = modifier.width(props.int("width", 80).dp).height(props.int("height", 32).dp)
    if (points.size < 2) {
        Spacer(modifier = box)
    } else {
        val low = points.min()
        val range = (points.max() - low).coerceAtLeast(0.0001f)
        androidx.compose.foundation.Canvas(modifier = box.almadarLabel(props.string("aria-label", "Trend"))) {
            val step = size.width / (points.size - 1).toFloat()
            val line = androidx.compose.ui.graphics.Path()
            points.forEachIndexed { index, point ->
                val x = index * step
                val y = size.height - (point - low) / range * size.height
                if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
            }
            if (filled) {
                val area = androidx.compose.ui.graphics.Path()
                area.addPath(line)
                area.lineTo(size.width, size.height)
                area.lineTo(0f, size.height)
                area.close()
                drawPath(area, color = color.copy(alpha = 0.15f))
            }
            drawPath(line, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth * density))
        }
    }
}
