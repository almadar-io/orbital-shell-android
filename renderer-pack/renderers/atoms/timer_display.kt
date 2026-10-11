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
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

@Composable
fun TimerDisplayRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val seconds = props.int("seconds", 90).coerceAtLeast(0)
    val running = props.bool("running", true)
    val lowThreshold = props.int("lowThreshold", -1)
    val low = lowThreshold >= 0 && seconds <= lowThreshold && seconds > 0
    val minutes = seconds / 60
    val rest = seconds % 60
    val padded = if (rest < 10) "0" + rest else rest.toString()
    val text = when (props.string("format", "mm:ss")) {
        "ss" -> seconds.toString() + "s"
        "countdown" -> if (minutes > 0) minutes.toString() + ":" + padded else rest.toString()
        else -> (if (minutes < 10) "0" + minutes else minutes.toString()) + ":" + padded
    }
    val pulse = rememberInfiniteTransition(label = "timer-pulse")
    val level by pulse.animateFloat(
        initialValue = if (low) 0.5f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = LocalAlmadarMotion.current.durationsMs["slow"] ?: 0), repeatMode = RepeatMode.Reverse),
        label = "timer-pulse-level"
    )
    val borderColor = if (low) palette.error.copy(alpha = 0.5f) else if (running) palette.success.copy(alpha = 0.5f) else palette.muted
    val style = LocalAlmadarTheme.current.typeScale.numeric.style.copy(
        fontWeight = FontWeight.Bold,
        fontSize = almadarType(when (props.string("size", "md")) { "sm" -> "sm"; "lg" -> "2xl"; else -> "lg" }).fontSize
    )
    Surface(
        modifier = modifier.almadarAlpha(level).almadarLabel(props.string("aria-label", text)).then(almadarBorder(AlmadarCorner.Container, LocalAlmadarGeometry.current.borderStandard, borderColor)),
        shape = almadarShape(AlmadarCorner.Container),
        color = palette.card.copy(alpha = 0.8f),
        contentColor = if (low) palette.error else palette.foreground
    ) {
        Text(
            text = text,
            style = style,
            modifier = Modifier.padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("xs"))
        )
    }
}
