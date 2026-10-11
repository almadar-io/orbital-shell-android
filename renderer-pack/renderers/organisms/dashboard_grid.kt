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
import androidx.compose.ui.draw.clip

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

@Composable
fun DashboardGridRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val cells = props.array("cells").mapNotNull { it as? JsonObject }
    val declared = props.int("columns", 3).coerceIn(2, 4)
    val gap = almadarSpaceFor(props.string("gap", "md"))
    val density = LocalAlmadarDensity.current
    val geometry = LocalAlmadarGeometry.current
    val palette = LocalAlmadarTheme.current.color
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "grid", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier)
    } else {
        AlmadarBoxWithConstraints(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
            val columns = when {
                maxWidth < AlmadarBreakpoints.sm -> 1
                maxWidth < AlmadarBreakpoints.md -> minOf(declared, 2)
                maxWidth < AlmadarBreakpoints.lg -> minOf(declared, 3)
                else -> declared
            }
            val lines = mutableListOf<MutableList<JsonObject>>()
            var used = 0
            cells.forEach { cell ->
                val span = cell.int("colSpan", 1).coerceIn(1, columns)
                if (lines.isEmpty() || used + span > columns) {
                    lines.add(mutableListOf())
                    used = 0
                }
                lines.last().add(cell)
                used += span
            }
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                lines.forEach { line ->
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        line.forEach { cell ->
                            val span = cell.int("colSpan", 1).coerceIn(1, columns)
                            val shape = androidx.compose.foundation.shape.RoundedCornerShape(geometry.radiusContainer)
                            Box(
                                modifier = Modifier
                                    .weight(span.toFloat())
                                    .fillMaxHeight()
                                    .clip(shape)
                                    .border(geometry.borderStandard, palette.border, shape)
                                    .padding(density.cardPaddingMd)
                            ) {
                                AlmadarNode(cell["content"], onEvent, Modifier.fillMaxWidth())
                            }
                        }
                        val gapCells = columns - line.sumOf { it.int("colSpan", 1).coerceIn(1, columns) }
                        if (gapCells > 0) Spacer(modifier = Modifier.weight(gapCells.toFloat()))
                    }
                }
            }
        }
    }
}
