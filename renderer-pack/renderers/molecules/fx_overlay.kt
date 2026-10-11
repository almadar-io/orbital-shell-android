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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.foundation.Canvas
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics

private val fxShakeSteps = listOf(0f, -6f, 5f, -4f, 3f, -2f, 0f)

private fun fxLifeMs(item: JsonObject, tickMs: Float): Float {
    val ttl = (item["ttl"] as? JsonPrimitive)?.floatOrNull ?: 0f
    val maxTtl = (item["maxTtl"] as? JsonPrimitive)?.floatOrNull ?: ttl
    return maxOf(maxTtl, ttl, 1f) * tickMs
}

private fun fxNumber(item: JsonObject, key: String): Float? = (item[key] as? JsonPrimitive)?.floatOrNull

@Composable
private fun FxOverlayNode(item: JsonObject, tickMs: Float, at: Float?) {
    val id = item.string("id")
    val life = fxLifeMs(item, tickMs)
    val span = life.toInt().coerceAtLeast(1)
    val clock: State<Float>? = if (at == null) rememberInfiniteTransition(label = "fx").animateFloat(
        initialValue = 0f, targetValue = span.toFloat(),
        animationSpec = infiniteRepeatable(tween(span, easing = LinearEasing)), label = "fx-clock"
    ) else null
    val origin = remember(id) { clock?.value ?: 0f }
    val bornNanos = remember(id) { System.nanoTime() }
    var finished by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (at == null) {
            delay(span.toLong())
            finished = true
        }
    }
    if (finished) return
    val elapsed = at ?: (((clock?.value ?: 0f) - origin + span) % span)
    val progress = (elapsed / life).coerceIn(0f, 1f)
    val scale = fxNumber(item, "size") ?: 1f
    val anchorX = fxNumber(item, "x") ?: 0.5f
    val anchorY = fxNumber(item, "z") ?: fxNumber(item, "y") ?: 0.4f
    val palette = LocalAlmadarTheme.current.color
    val custom = item["color"] != null
    val given = item.string("color")
    when (item.string("effect", "sparkle")) {
        "confetti" -> AlmadarConfettiBurst(
            almadarConfetti((fxNumber(item, "particleCount") ?: 30f).toInt(), id), { at ?: ((if ((clock?.value ?: 0f) >= 0f) System.nanoTime() - bornNanos else 0L) / 1_000_000f) }, life, scale,
            if (custom) almadarCssColor(given, palette.primary) else null, anchorX, anchorY, 200f, Modifier.fillMaxSize()
        )
        "flash" -> Box(Modifier.fillMaxSize().background(almadarCssColor(given.ifEmpty { "#ffffff" }, palette.foreground).copy(alpha = 0.75f * (1f - almadarFxEaseOut(progress)))))
        "streak-glow" -> {
            val amount = if (progress < 0.6f) 0.9f + (0.6f - 0.9f) * almadarFxEaseInOut(progress / 0.6f) else 0.6f * (1f - almadarFxEaseInOut((progress - 0.6f) / 0.4f))
            val color = almadarCssColor(given.ifEmpty { "var(--color-warning)" }, palette.warning).copy(alpha = amount)
            val blur = 60f * scale
            Canvas(Modifier.fillMaxSize()) {
                val reach = blur * density / 2f
                drawRect(Brush.verticalGradient(listOf(color, Color.Transparent), 0f, reach), size = Size(size.width, reach))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, color), size.height - reach, size.height), topLeft = Offset(0f, size.height - reach), size = Size(size.width, reach))
                drawRect(Brush.horizontalGradient(listOf(color, Color.Transparent), 0f, reach), size = Size(reach, size.height))
                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, color), size.width - reach, size.width), topLeft = Offset(size.width - reach, 0f), size = Size(reach, size.height))
            }
        }
        "float-text" -> {
            val message = item.string("message")
            if (message.isNotEmpty()) {
                val rise = if (progress < 0.15f) almadarFxEaseOut(progress / 0.15f) else 1f - almadarFxEaseOut((progress - 0.15f) / 0.85f)
                AlmadarBoxWithConstraints(Modifier.fillMaxSize()) {
                    val width = constraints.maxWidth
                    val height = constraints.maxHeight
                    val lift = (8f - 48f * almadarFxEaseOut(progress)) * LocalDensity.current.density
                    Box(
                        Modifier
                            .layout { measurable, limits ->
                                val placeable = measurable.measure(limits.copy(minWidth = 0, minHeight = 0, maxWidth = Int.MAX_VALUE))
                                layout(0, 0) { placeable.place((anchorX * width - placeable.width / 2f).toInt(), (anchorY * height + lift).toInt()) }
                            }
                            .graphicsLayer { alpha = rise; scaleX = scale; scaleY = scale }
                    ) {
                        CompositionLocalProvider(LocalContentColor provides almadarCssColor(given.ifEmpty { "var(--color-success)" }, palette.success)) {
                            AlmadarText(message, color = "inherit", weight = "semibold")
                        }
                    }
                }
            }
        }
        "shake" -> Unit
        else -> {
            val count = (fxNumber(item, "particleCount") ?: 8f).toInt()
            val base = if (custom) almadarCssColor(given, palette.warning) else almadarFxColor(4)
            Canvas(Modifier.fillMaxSize()) {
                repeat(maxOf(count, 0)) { index ->
                    val dx = (almadarFxHash(id, index * 4) - 0.5f) * 160f * scale
                    val dy = (almadarFxHash(id, index * 4 + 1) - 0.5f) * 120f * scale
                    val dot = (4f + almadarFxHash(id, index * 4 + 2) * 5f) * scale
                    val delay = almadarFxHash(id, index * 4 + 3) * life * 0.4f
                    if (elapsed >= delay) {
                        val t = ((elapsed - delay) / maxOf(life - delay, 200f)).coerceIn(0f, 1f)
                        val grow = if (t < 0.3f) almadarFxEaseInOut(t / 0.3f) else 1f - almadarFxEaseInOut((t - 0.3f) / 0.7f)
                        val shrink = if (t < 0.3f) grow else 0.3f + 0.7f * grow
                        drawCircle(
                            base.copy(alpha = grow),
                            radius = dot * density / 2f * shrink,
                            center = Offset(anchorX * size.width + (dx + dot / 2f) * density, anchorY * size.height + (dy + dot / 2f) * density)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FxOverlayRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val tick = (props as? JsonObject)?.get("tickMs")?.let { (it as? JsonPrimitive)?.floatOrNull } ?: 500f
    val at = (props as? JsonObject)?.get("at")?.let { (it as? JsonPrimitive)?.floatOrNull }
    val items = props.array("items").mapNotNull { it as? JsonObject }
        .filter { (it["id"] as? JsonPrimitive)?.isString == true && it.string("space") != "world" }
    val shake = items.lastOrNull { it.string("effect") == "shake" }
    val hasChildren = (props as? JsonObject)?.get("children") != null
    val layer: @Composable (Modifier) -> Unit = { layerModifier ->
        Box(layerModifier.clipToBounds().clearAndSetSemantics { }) {
            items.forEach { item -> key(item.string("id")) { FxOverlayNode(item, tick, at) } }
        }
    }
    if (hasChildren) {
        val shakeSpan = shake?.let { minOf(fxLifeMs(it, tick), 600f).toInt().coerceAtLeast(1) } ?: 1
        val shakeClock: State<Float>? = if (at == null) rememberInfiniteTransition(label = "shake").animateFloat(
            initialValue = 0f, targetValue = shakeSpan.toFloat(),
            animationSpec = infiniteRepeatable(tween(shakeSpan, easing = LinearEasing)), label = "shake-clock"
        ) else null
        val shakeOrigin = remember(shake?.string("id")) { shakeClock?.value ?: 0f }
        var shakeDone by remember(shake?.string("id")) { mutableStateOf(false) }
        LaunchedEffect(shake?.string("id")) {
            if (shake != null && at == null) {
                delay(shakeSpan.toLong())
                shakeDone = true
            }
        }
        val born = at ?: (if (shakeDone) shakeSpan.toFloat() else (((shakeClock?.value ?: 0f) - shakeOrigin + shakeSpan) % shakeSpan))
        val shakeLife = shakeSpan.toFloat()
        val shift = if (shake == null) 0f else {
            val t = (born / shakeLife).coerceIn(0f, 1f)
            val slot = minOf((t * 6f).toInt(), 5)
            fxShakeSteps[slot] + (fxShakeSteps[slot + 1] - fxShakeSteps[slot]) * almadarFxEaseInOut(t * 6f - slot)
        }
        Box(modifier = modifier.almadarLabel(props.string("aria-label"))) {
            Column(modifier = Modifier.graphicsLayer { translationX = shift * density }) {
                props.children().forEach { RenderChild(it, Modifier, onEvent) }
            }
            layer(Modifier.matchParentSize())
        }
    } else {
        Box(modifier = modifier.fillMaxWidth().heightIn(min = AlmadarSizes.w64).almadarLabel(props.string("aria-label"))) {
            layer(Modifier.matchParentSize())
        }
    }
}
