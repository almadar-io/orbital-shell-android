package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

@Composable
fun HealthBarRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val current = props.float("current", 3f)
    val maximum = props.float("max", 5f)
    val format = props.string("format", "hearts")
    val size = props.string("size", "md")
    val fraction = if (maximum > 0f) (current / maximum).coerceIn(0f, 1f) else 0f
    val animated = props.bool("animated", true)
    val shownFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = if (animated) (LocalAlmadarMotion.current.durationsMs["normal"] ?: 0) else 0),
        label = "health-fraction"
    )
    val summary = current.toInt().toString() + "/" + maximum.toInt().toString()
    val barHeight = almadarSpace(when (size) { "lg" -> "space4"; "sm" -> "space2"; else -> "space3" })
    val base = LocalAlmadarIconography.current.defaultSize
    val fillColor = if (fraction > 0.66f) palette.success else if (fraction > 0.33f) palette.warning else palette.error
    val label = props.string("labelSuffix").let { suffix -> current.toInt().toString() + " / " + maximum.toInt().toString() + (if (suffix.isNotEmpty()) " " + suffix else "") }
    when (format) {
        "bar" -> LinearProgressIndicator(
            progress = { shownFraction },
            modifier = modifier.width(base * 6f).height(barHeight).clip(MaterialTheme.shapes.large).almadarLabel(summary),
            color = fillColor,
            trackColor = palette.muted
        )
        "progress" -> Row(
            modifier = modifier.fillMaxWidth().almadarLabel(summary),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
        ) {
            if ((props as? JsonObject)?.get("level") != null) {
                Surface(shape = MaterialTheme.shapes.small, color = palette.accent, contentColor = palette.accentForeground) {
                    Text(
                        text = "Lv." + props.int("level"),
                        style = almadarType("xs").copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = almadarSpaceFor("sm"), vertical = almadarSpaceFor("xs"))
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                LinearProgressIndicator(
                    progress = { shownFraction },
                    modifier = Modifier.fillMaxWidth().height(barHeight).clip(MaterialTheme.shapes.large),
                    color = palette.accent,
                    trackColor = palette.muted
                )
                if (props.bool("showLabel", true)) Text(text = label, style = almadarType("xs"), color = palette.mutedForeground)
            }
        }
        "numeric" -> Text(
            text = summary,
            style = LocalAlmadarTheme.current.typeScale.numeric.style.copy(fontWeight = FontWeight.Bold),
            color = palette.foreground,
            modifier = modifier.almadarLabel(summary)
        )
        else -> Row(modifier = modifier.almadarLabel(summary), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
            for (index in 0 until maximum.toInt().coerceAtLeast(0)) {
                AlmadarIcon(
                    name = "heart",
                    tint = if (index < current.toInt()) palette.error else palette.mutedForeground,
                    modifier = Modifier.size(base * (when (size) { "lg" -> 2f; "sm" -> 1f; else -> 1.5f }))
                )
            }
        }
    }
}
