package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun GridRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val width = LocalConfiguration.current.screenWidthDp
    val colsNode = root?.get("cols")
    val parseCols: (JsonElement?) -> Int? = { node ->
        val text = (node as? JsonPrimitive)?.contentOrNull
        if (text == "none") 0 else text?.toIntOrNull()
    }
    val columns = if (colsNode is JsonObject) {
        val tiers = listOf("base" to Dp.Hairline, "sm" to AlmadarBreakpoints.sm, "md" to AlmadarBreakpoints.md, "lg" to AlmadarBreakpoints.lg, "xl" to AlmadarBreakpoints.xl)
        tiers.filter { width.dp >= it.second }.mapNotNull { parseCols(colsNode[it.first]) }.lastOrNull() ?: 1
    } else parseCols(colsNode) ?: 1
    val count = columns.coerceAtLeast(1)
    val gap = props.string("gap", "md")
    val rowGap = almadarSpaceFor(props.string("rowGap", gap))
    val colGap = almadarSpaceFor(props.string("colGap", gap))
    val vertical = when (props.string("alignItems")) {
        "center" -> Alignment.CenterVertically
        "end" -> Alignment.Bottom
        else -> Alignment.Top
    }
    val horizontal = when (props.string("justifyItems")) {
        "center" -> Alignment.TopCenter
        "end" -> Alignment.TopEnd
        else -> Alignment.TopStart
    }
    val stretch = props.string("justifyItems", "stretch") == "stretch"
    val children = props.children()
    val rowCount = props.int("rows", 0).takeIf { it > 0 } ?: ((children.size + count - 1) / count)
    val byColumn = props.string("flow").startsWith("col")
    val cells: List<JsonElement?> = if (byColumn) {
        (0 until rowCount).flatMap { row -> (0 until count).map { col -> children.getOrNull(col * rowCount + row) } }
    } else {
        children.chunked(count).flatMap { chunk -> chunk + List(count - chunk.size) { null } }
    }
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(rowGap)) {
        cells.chunked(count).forEach { rowCells ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(colGap), verticalAlignment = vertical) {
                rowCells.forEach { cell ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = horizontal) {
                        if (cell != null) RenderChild(cell, if (stretch) Modifier.fillMaxWidth() else Modifier, onEvent)
                    }
                }
            }
        }
    }
}
