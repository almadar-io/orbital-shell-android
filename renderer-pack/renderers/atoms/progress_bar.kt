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
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics

@Composable
fun ProgressBarRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val maximum = props.float("max", 100f).coerceAtLeast(1f)
    val fraction = (props.float("value") / maximum).coerceIn(0f, 1f)
    val percentText = (fraction * 100f).toInt().toString() + "%"
    val label = props.string("label")
    val showPercent = props.bool("showPercentage") || props.bool("showLabel")
    val tone = almadarTone(props.string("color", props.string("variant", "primary")).let { if (it == "default") "primary" else it })
    val size = props.string("size", "md")
    val a11y = props.string("aria-label", if (label.isNotEmpty()) label else percentText)
    val barHeight = almadarSpaceFor(when (size) { "lg" -> "md"; "sm" -> "xs"; else -> "sm" })
    val track = palette.muted
    val bar = LocalAlmadarGeometry.current.borderStandard
    when (props.string("progressType", "linear")) {
        "circular" -> {
            val diameter = LocalAlmadarIconography.current.defaultSize * (when (size) { "lg" -> 8f; "sm" -> 4f; else -> 6f })
            Box(modifier = modifier.almadarLabel(a11y), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.size(diameter),
                    color = tone.solid,
                    trackColor = track,
                    strokeWidth = barHeight
                )
                if (showPercent) Text(text = percentText, style = almadarType("sm").copy(fontWeight = FontWeight.Medium), color = palette.foreground)
            }
        }
        "stepped" -> {
            val steps = props.int("steps", 5).coerceAtLeast(1)
            val filled = Math.round(fraction * steps)
            Column(modifier = modifier.fillMaxWidth().almadarLabel(a11y)) {
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), modifier = Modifier.fillMaxWidth()) {
                    for (index in 0 until steps) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barHeight)
                                .background(if (index < filled) tone.solid else track, almadarShape(AlmadarCorner.None))
                                .then(almadarBorder(AlmadarCorner.None, bar, palette.border))
                        )
                    }
                }
                if (label.isNotEmpty() || showPercent) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = label, style = almadarType("sm").copy(fontWeight = FontWeight.Bold), color = palette.foreground)
                        if (showPercent) Text(text = percentText, style = almadarType("sm"), color = palette.foreground)
                    }
                }
            }
        }
        else -> {
            Column(modifier = modifier.fillMaxWidth().almadarLabel(a11y), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                if (label.isNotEmpty() || showPercent) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = label, style = almadarType("sm").copy(fontWeight = FontWeight.Bold), color = palette.foreground)
                        if (showPercent) Text(text = percentText, style = almadarType("sm").copy(fontWeight = FontWeight.Medium), color = palette.foreground)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }
                        .background(track, almadarShape(AlmadarCorner.None))
                        .then(almadarBorder(AlmadarCorner.None, bar, palette.border))
                ) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(tone.solid))
                }
            }
        }
    }
}
