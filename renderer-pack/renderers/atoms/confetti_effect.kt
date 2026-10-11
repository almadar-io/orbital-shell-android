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

// tier: atoms — a single Material 3 control

import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.delay

@Composable
fun ConfettiEffectRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val trigger = props.bool("trigger")
    val duration = props.float("duration", 2000f)
    val count = props.int("particleCount", 30)
    val context = LocalContext.current
    val reducedMotion = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    var armed by remember { mutableStateOf(false) }
    var burst by remember { mutableStateOf(0) }
    var live by remember { mutableStateOf(false) }
    val span = duration.toInt().coerceAtLeast(1)
    val at = (props as? JsonObject)?.get("at")?.let { (it as? JsonPrimitive)?.floatOrNull }
    val clock: State<Float>? = if (at == null) rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue = 0f, targetValue = span.toFloat(),
        animationSpec = infiniteRepeatable(tween(span, easing = LinearEasing)), label = "confetti-clock"
    ) else null
    var origin by remember { mutableStateOf(0L) }
    LaunchedEffect(trigger) {
        val rising = trigger && !armed
        armed = trigger
        if (rising && !reducedMotion && at == null) {
            burst += 1
            origin = System.nanoTime()
            live = true
            delay(span.toLong())
            live = false
        }
    }
    val elapsedAt: () -> Float = { at ?: (if ((clock?.value ?: 0f) >= 0f) (System.nanoTime() - origin) / 1_000_000f else 0f) }
    val particles = remember(burst, count) { almadarConfetti(count, "confetti-$burst") }
    Box(modifier = modifier.fillMaxWidth().heightIn(min = AlmadarSizes.w64).clearAndSetSemantics { }) {
        if (live || (at != null && trigger && !reducedMotion)) {
            AlmadarConfettiBurst(particles, elapsedAt, duration, 1f, null, null, 0f, 1f, Modifier.matchParentSize())
        }
    }
}
