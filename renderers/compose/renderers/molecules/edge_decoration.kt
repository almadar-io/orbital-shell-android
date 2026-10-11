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

import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

private const val EDGE_VIEW_WIDTH: Float = 200f
private const val EDGE_VIEW_HEIGHT: Float = 600f

private fun edgeNum(value: Float): String = String.format(java.util.Locale.ROOT, "%.2f", value)

private fun edgeArch(facing: String, w: Float, h: Float): List<String> {
    val paths = mutableListOf<String>()
    val archCount = 3
    val archH = h / archCount
    val rings = 5
    val right = facing == "right"
    for (a in 0 until archCount) {
        val baseY = a * archH + archH
        val topY = a * archH + archH * 0.05f
        for (r in 0 until rings) {
            val scale = 1f - r * 0.17f
            val archW = w * 0.95f * scale
            val aTopY = topY + (1f - scale) * (baseY - topY) * 0.5f
            val aBaseY = baseY - (1f - scale) * archH * 0.05f
            val edgeX = if (right) w else 0f
            val innerX = if (right) w - archW else archW
            val midY = (aTopY + aBaseY) / 2f
            val radius = Math.sqrt((archW * archW + (aBaseY - midY) * (aBaseY - midY)).toDouble()).toFloat()
            val sweep = if (right) 0 else 1
            val mid = aTopY + (aBaseY - aTopY) * 0.5f
            paths.add("M ${edgeNum(edgeX)},${edgeNum(aBaseY)} A ${edgeNum(radius)} ${edgeNum(radius)} 0 0 $sweep ${edgeNum(innerX)},${edgeNum(mid)} A ${edgeNum(radius)} ${edgeNum(radius)} 0 0 $sweep ${edgeNum(edgeX)},${edgeNum(aTopY)}")
        }
        val edgeX = if (right) w else 0f
        val innerX = if (right) w * 0.05f else w * 0.95f
        paths.add("M ${edgeNum(edgeX)},${edgeNum(baseY)} L ${edgeNum(innerX)},${edgeNum(baseY)}")
    }
    return paths
}

private fun edgeVine(facing: String, w: Float, h: Float): List<String> {
    val paths = mutableListOf<String>()
    val right = facing == "right"
    val dir = if (right) -1f else 1f
    val edgeX = if (right) w * 0.85f else w * 0.15f
    val segments = 5
    val segH = h / segments
    for (i in 0 until segments) {
        val y0 = i * segH
        val y1 = y0 + segH
        val sway = w * 0.12f * (if (i % 2 == 0) 1f else -1f)
        paths.add("M ${edgeNum(edgeX + sway)},${edgeNum(y0)} C ${edgeNum(edgeX - sway)},${edgeNum(y0 + segH * 0.33f)} ${edgeNum(edgeX + sway)},${edgeNum(y0 + segH * 0.66f)} ${edgeNum(edgeX - sway)},${edgeNum(y1)}")
        val branchY = y0 + segH * 0.35f
        val branchX = edgeX
        val leafW = w * 0.55f
        val leafH = segH * 0.35f
        val leafTipX = branchX + dir * leafW
        val leafTipY = branchY - leafH * 0.2f
        paths.add("M ${edgeNum(branchX)},${edgeNum(branchY)} C ${edgeNum(branchX + dir * leafW * 0.3f)},${edgeNum(branchY - leafH)} ${edgeNum(leafTipX - dir * leafW * 0.1f)},${edgeNum(leafTipY - leafH * 0.3f)} ${edgeNum(leafTipX)},${edgeNum(leafTipY)}")
        paths.add("M ${edgeNum(leafTipX)},${edgeNum(leafTipY)} C ${edgeNum(leafTipX - dir * leafW * 0.2f)},${edgeNum(leafTipY + leafH * 0.6f)} ${edgeNum(branchX + dir * leafW * 0.15f)},${edgeNum(branchY + leafH * 0.4f)} ${edgeNum(branchX)},${edgeNum(branchY + leafH * 0.1f)}")
        val tendrilY = y0 + segH * 0.7f
        val tendrilW = w * 0.35f
        val tendrilTipX = edgeX + dir * tendrilW
        paths.add("M ${edgeNum(edgeX)},${edgeNum(tendrilY)} Q ${edgeNum(edgeX + dir * tendrilW * 0.6f)},${edgeNum(tendrilY - segH * 0.12f)} ${edgeNum(tendrilTipX)},${edgeNum(tendrilY + segH * 0.05f)}")
        val spR = leafW * 0.08f
        val spSweep = if (right) 0 else 1
        paths.add("M ${edgeNum(leafTipX)},${edgeNum(leafTipY)} A ${edgeNum(spR)} ${edgeNum(spR)} 0 1 $spSweep ${edgeNum(leafTipX + dir * spR * 0.5f)},${edgeNum(leafTipY + spR)}")
    }
    return paths
}

private fun edgeLattice(w: Float, h: Float): List<String> {
    val paths = mutableListOf<String>()
    val cols = 5
    val cellW = w / cols
    val rows = Math.ceil((h / cellW).toDouble()).toInt()
    val bulge = cellW * 0.3f
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val cx = col * cellW + cellW / 2f
            val cy = row * cellW + cellW / 2f
            val hw = cellW * 0.5f
            val hh = cellW * 0.5f
            paths.add("M ${edgeNum(cx)},${edgeNum(cy - hh)} Q ${edgeNum(cx + bulge)},${edgeNum(cy)} ${edgeNum(cx)},${edgeNum(cy + hh)}")
            paths.add("M ${edgeNum(cx)},${edgeNum(cy - hh)} Q ${edgeNum(cx - bulge)},${edgeNum(cy)} ${edgeNum(cx)},${edgeNum(cy + hh)}")
            paths.add("M ${edgeNum(cx - hw)},${edgeNum(cy)} Q ${edgeNum(cx)},${edgeNum(cy - bulge * 0.5f)} ${edgeNum(cx + hw)},${edgeNum(cy)}")
            paths.add("M ${edgeNum(cx - hw)},${edgeNum(cy)} Q ${edgeNum(cx)},${edgeNum(cy + bulge * 0.5f)} ${edgeNum(cx + hw)},${edgeNum(cy)}")
        }
    }
    return paths
}

@Composable
fun EdgeDecorationRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val variant = props.string("variant", "arch")
    val side = props.string("side", "both")
    val stroke = props.float("strokeWidth", 0.5f)
    val share = props.float("width", 15f) / 100f
    val colorName = props.string("color", "primary")
    val ink = when (colorName) {
        "primary" -> palette.primary
        "secondary" -> palette.secondary
        "success" -> palette.success
        "warning" -> palette.warning
        "error" -> palette.error
        "muted" -> palette.muted
        else -> parseCssColor(colorName, palette.primary)
    }
    val sides = if (side == "both") listOf("left", "right") else listOf(side)
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxSize()) {
        sides.forEach { facing ->
            val paths = remember(variant, facing) {
                val commands = when (variant) {
                    "vine" -> edgeVine(facing, EDGE_VIEW_WIDTH, EDGE_VIEW_HEIGHT)
                    "lattice" -> edgeLattice(EDGE_VIEW_WIDTH, EDGE_VIEW_HEIGHT)
                    else -> edgeArch(facing, EDGE_VIEW_WIDTH, EDGE_VIEW_HEIGHT)
                }
                commands.map { PathParser().parsePathString(it).toPath() }
            }
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .align(if (facing == "left") Alignment.CenterStart else Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(maxWidth * share)
                    .almadarAlpha(props.float("opacity", 0.15f))
            ) {
                withTransform({ scale(size.width / EDGE_VIEW_WIDTH, size.height / EDGE_VIEW_HEIGHT, androidx.compose.ui.geometry.Offset.Zero) }) {
                    paths.forEach { drawPath(it, color = ink, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)) }
                }
            }
        }
    }
}
