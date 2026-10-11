package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LineChartRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val points = props.array("data").mapNotNull { it as? JsonObject }
    val sorted = if (points.any { it["date"] != null }) points.sortedBy { it.string("date") }
        else if (points.any { it["x"] != null }) points.sortedBy { it.float("x") }
        else points
    val values = sorted.map { it.float("value") }
    val labels = sorted.map { it.string("label", it.string("date", it.string("x"))) }
    if (points.isEmpty()) {
        AlmadarText("No data available", modifier = modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")), color = "muted", align = "center")
    } else {
        val low = values.minOrNull() ?: 0f
        val high = maxOf(values.maxOrNull() ?: 1f, low + 1f)
        val series = listOf(AlmadarChartSeries("", labels, values, palette.primary, false))
        Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", "Line chart")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
            AlmadarLineCanvas(
                series, low, high, props.float("height", 200f).dp, props.bool("showArea", true),
                if (props.bool("showGrid", true)) palette.border else Color.Transparent,
                LocalAlmadarGeometry.current.borderStandard,
                { _, _ -> }
            )
            if (props.bool("showValues") || values.size == 1) {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    values.forEachIndexed { index, value ->
                        AlmadarText(
                            labels[index].ifEmpty { formatFieldValue(JsonPrimitive(value), "number") } + if (labels[index].isNotEmpty()) ": " + formatFieldValue(JsonPrimitive(value), "number") else "",
                            variant = "caption", color = "secondary"
                        )
                    }
                }
            }
        }
    }
}
