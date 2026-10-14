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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer

private const val GLYPH_PAD: Float = 6f

private class GlyphBox(val x: Float, val y: Float, val w: Float, val h: Float)

private class GlyphStroke(
    val d: String,
    val width: Float = 1.5f,
    val fill: Float = 0f,
    val dash: List<Float> = emptyList(),
    val alpha: Float = 1f,
    val solid: Boolean = false
)

private class GlyphLabel(val text: String, val x: Float, val y: Float, val size: Float, val bold: Boolean = false, val alpha: Float = 1f)

private fun gn(v: Float): String = String.format(java.util.Locale.ROOT, "%.2f", v)

private fun gCircle(cx: Float, cy: Float, r: Float): String =
    "M ${gn(cx - r)},${gn(cy)} A ${gn(r)} ${gn(r)} 0 1 0 ${gn(cx + r)},${gn(cy)} A ${gn(r)} ${gn(r)} 0 1 0 ${gn(cx - r)},${gn(cy)} Z"

private fun gEllipse(cx: Float, cy: Float, rx: Float, ry: Float): String =
    "M ${gn(cx - rx)},${gn(cy)} A ${gn(rx)} ${gn(ry)} 0 1 0 ${gn(cx + rx)},${gn(cy)} A ${gn(rx)} ${gn(ry)} 0 1 0 ${gn(cx - rx)},${gn(cy)} Z"

private fun gRound(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M ${gn(x + r)},${gn(y)} H ${gn(x + w - r)} A ${gn(r)} ${gn(r)} 0 0 1 ${gn(x + w)},${gn(y + r)} V ${gn(y + h - r)} A ${gn(r)} ${gn(r)} 0 0 1 ${gn(x + w - r)},${gn(y + h)} H ${gn(x + r)} A ${gn(r)} ${gn(r)} 0 0 1 ${gn(x)},${gn(y + h - r)} V ${gn(y + r)} A ${gn(r)} ${gn(r)} 0 0 1 ${gn(x + r)},${gn(y)} Z"

private fun gPoly(close: Boolean, vararg p: Float): String {
    val sb = StringBuilder()
    var i = 0
    while (i < p.size) {
        sb.append(if (i == 0) "M " else " L ").append(gn(p[i])).append(',').append(gn(p[i + 1]))
        i += 2
    }
    if (close) sb.append(" Z")
    return sb.toString()
}

private fun gLine(x1: Float, y1: Float, x2: Float, y2: Float): String = gPoly(false, x1, y1, x2, y2)

private fun glyphBox(kind: String, label: String): GlyphBox = when (kind) {
    "application" -> GlyphBox(0f, 0f, 120f, 80f)
    "orbital" -> GlyphBox(-36f, -36f, 72f, 72f)
    "entity" -> GlyphBox(-28f, -28f, 56f, 56f)
    "trait" -> GlyphBox(-44f, -22f, 88f, 44f)
    "page" -> GlyphBox(-10f, -10f, 20f, 20f)
    "state" -> GlyphBox(0f, 0f, 96f, 34f)
    "event" -> GlyphBox(-5f, -10f, 11.25f, 20f)
    "guard", "literal", "effect" -> GlyphBox(-14f, -14f, 28f, 28f)
    "field" -> GlyphBox(0f, -2.5f, 42.5f, 5f)
    "fieldType" -> GlyphBox(-9f, -9f, 18f, 18f)
    "binding" -> GlyphBox(-40f, -13f, 80f, 13f)
    "bindingRef" -> {
        val r = maxOf(14f, ("@" + label.ifEmpty { "entity.id" }).length * 3.5f + 4f)
        GlyphBox(-r, -r, r * 2f, r * 2f)
    }
    "operator" -> GlyphBox(-20f, -14f, 40f, 28f)
    "sexpr" -> GlyphBox(0f, 0f, 110f, 60f)
    else -> GlyphBox(-50f, -50f, 100f, 100f)
}

private fun glyphEffect(op: String, x: Float, y: Float, s: Float): List<GlyphStroke> = when (op) {
    "render-ui" -> listOf(
        GlyphStroke(gRound(x - s, y - s, s * 2f, s * 2f, 1f)),
        GlyphStroke(gLine(x, y - s, x, y + s), 1f),
        GlyphStroke(gLine(x - s, y, x + s, y), 1f)
    )
    "set" -> listOf(GlyphStroke(gPoly(true, x - s * 0.6f, y + s, x - s, y + s * 0.4f, x + s * 0.4f, y - s, x + s, y - s * 0.4f)))
    "persist" -> listOf(
        GlyphStroke(gEllipse(x, y - s * 0.6f, s, s * 0.4f)),
        GlyphStroke(gLine(x - s, y - s * 0.6f, x - s, y + s * 0.4f)),
        GlyphStroke(gLine(x + s, y - s * 0.6f, x + s, y + s * 0.4f)),
        GlyphStroke(gEllipse(x, y + s * 0.4f, s, s * 0.4f))
    )
    "fetch" -> listOf(
        GlyphStroke(gLine(x, y - s, x, y + s * 0.6f)),
        GlyphStroke(gPoly(false, x - s * 0.5f, y + s * 0.1f, x, y + s, x + s * 0.5f, y + s * 0.1f))
    )
    "emit" -> listOf(
        GlyphStroke(gCircle(x, y, s * 0.3f), 0f, 1f),
        GlyphStroke("M ${gn(x - s * 0.7f)},${gn(y - s * 0.7f)} A ${gn(s)} ${gn(s)} 0 0 1 ${gn(x + s * 0.7f)},${gn(y - s * 0.7f)}"),
        GlyphStroke("M ${gn(x - s)},${gn(y - s)} A ${gn(s * 1.4f)} ${gn(s * 1.4f)} 0 0 1 ${gn(x + s)},${gn(y - s)}", 1f, alpha = 0.6f)
    )
    "navigate" -> listOf(
        GlyphStroke(gLine(x - s, y, x + s * 0.6f, y)),
        GlyphStroke(gPoly(false, x + s * 0.1f, y - s * 0.5f, x + s, y, x + s * 0.1f, y + s * 0.5f))
    )
    "call-service" -> listOf(
        GlyphStroke(gLine(x - s, y - s * 0.3f, x + s, y - s * 0.3f)),
        GlyphStroke(gPoly(false, x + s * 0.5f, y - s * 0.7f, x + s, y - s * 0.3f, x + s * 0.5f, y + s * 0.1f)),
        GlyphStroke(gLine(x + s, y + s * 0.3f, x - s, y + s * 0.3f)),
        GlyphStroke(gPoly(false, x - s * 0.5f, y - s * 0.1f, x - s, y + s * 0.3f, x - s * 0.5f, y + s * 0.7f))
    )
    "spawn" -> listOf(
        GlyphStroke(gCircle(x, y, s)),
        GlyphStroke(gLine(x - s * 0.5f, y, x + s * 0.5f, y)),
        GlyphStroke(gLine(x, y - s * 0.5f, x, y + s * 0.5f))
    )
    "despawn" -> listOf(GlyphStroke(gCircle(x, y, s)), GlyphStroke(gLine(x - s * 0.5f, y, x + s * 0.5f, y)))
    "do" -> listOf(
        GlyphStroke(gCircle(x, y, s)),
        GlyphStroke(gPoly(true, x - s * 0.3f, y - s * 0.5f, x + s * 0.5f, y, x - s * 0.3f, y + s * 0.5f), 0f, 1f)
    )
    "if" -> listOf(GlyphStroke(gPoly(true, x, y - s, x + s * 0.7f, y, x, y + s, x - s * 0.7f, y)))
    "clock" -> listOf(
        GlyphStroke(gCircle(x, y, s)),
        GlyphStroke(gPoly(false, x, y - s * 0.6f, x, y, x + s * 0.5f, y + s * 0.3f))
    )
    "neuron" -> listOf(
        GlyphStroke(gLine(x - s, y - s * 0.6f, x + s * 0.8f, y), 1f),
        GlyphStroke(gLine(x - s, y + s * 0.6f, x + s * 0.8f, y), 1f),
        GlyphStroke(gCircle(x - s, y - s * 0.6f, s * 0.3f), 0f, 1f),
        GlyphStroke(gCircle(x - s, y + s * 0.6f, s * 0.3f), 0f, 1f),
        GlyphStroke(gCircle(x + s * 0.8f, y, s * 0.35f), 0f, 1f)
    )
    "gear" -> listOf(
        GlyphStroke(gCircle(x, y, s * 0.75f), dash = listOf(s * 0.5f, s * 0.3f)),
        GlyphStroke(gCircle(x, y, s * 0.3f), 0f, 1f)
    )
    else -> listOf(GlyphStroke(gCircle(x, y, s), 1.2f, dash = listOf(2f, 2f)))
}

private fun glyphEffectStrokes(effectType: String, x: Float, y: Float, s: Float): List<GlyphStroke> {
    val own = setOf("render-ui", "set", "persist", "fetch", "emit", "navigate", "call-service", "spawn", "despawn", "do", "if")
    if (effectType in own) return glyphEffect(effectType, x, y, s)
    val category = AVL_EFFECT_CATEGORY[effectType] ?: AVL_EFFECT_FAMILY[effectType.substringBefore('/', "")]
    return when (category) {
        "ui" -> glyphEffect("render-ui", x, y, s)
        "data" -> glyphEffect("set", x, y, s)
        "communication" -> glyphEffect("emit", x, y, s)
        "lifecycle" -> glyphEffect("spawn", x, y, s)
        "control" -> glyphEffect("if", x, y, s)
        "async" -> glyphEffect("clock", x, y, s)
        "compute" -> glyphEffect("neuron", x, y, s)
        "system" -> glyphEffect("gear", x, y, s)
        else -> glyphEffect("unknown", x, y, s)
    }
}

private fun glyphFieldType(fieldType: String, x: Float, y: Float, s: Float): List<GlyphStroke> {
    val shape = when (fieldType) {
        "string", "email", "url", "phone", "uuid" -> "circle"
        "number", "money" -> "triangle"
        "boolean" -> "square"
        "date", "timestamp", "datetime" -> "diamond"
        "enum", "union" -> "ring"
        "array", "tuple" -> "bars"
        "relation" -> "link"
        else -> "hexagon"
    }
    return when (shape) {
        "circle" -> listOf(GlyphStroke(gCircle(x, y, s), 0f, 1f))
        "triangle" -> listOf(GlyphStroke(gPoly(true, x, y - s, x + s, y + s * 0.7f, x - s, y + s * 0.7f), 0f, 1f))
        "square" -> listOf(GlyphStroke(gPoly(true, x - s * 0.8f, y - s * 0.8f, x + s * 0.8f, y - s * 0.8f, x + s * 0.8f, y + s * 0.8f, x - s * 0.8f, y + s * 0.8f), 0f, 1f))
        "diamond" -> listOf(GlyphStroke(gPoly(true, x, y - s, x + s, y, x, y + s, x - s, y), 0f, 1f))
        "ring" -> listOf(GlyphStroke(gCircle(x, y, s)))
        "bars" -> listOf(
            GlyphStroke(gLine(x - s, y - s * 0.6f, x + s, y - s * 0.6f), 2f),
            GlyphStroke(gLine(x - s, y, x + s, y), 2f),
            GlyphStroke(gLine(x - s, y + s * 0.6f, x + s, y + s * 0.6f), 2f)
        )
        "link" -> listOf(GlyphStroke(gCircle(x - s * 0.45f, y, s * 0.55f)), GlyphStroke(gCircle(x + s * 0.45f, y, s * 0.55f)))
        else -> {
            val pts = FloatArray(12)
            for (i in 0 until 6) {
                val a = (Math.PI * 2.0 * i / 6.0 - Math.PI / 6.0)
                pts[i * 2] = x + s * Math.cos(a).toFloat()
                pts[i * 2 + 1] = y + s * Math.sin(a).toFloat()
            }
            listOf(GlyphStroke(gPoly(true, *pts), 0f, 1f, alpha = 0.8f))
        }
    }
}

private fun glyphStrokes(kind: String, label: String, effectType: String, fieldType: String, persistence: String): List<GlyphStroke> = when (kind) {
    "application" -> listOf(GlyphStroke(gRound(0f, 0f, 120f, 80f, 12f), 3f, 0.02f))
    "orbital" -> listOf(GlyphStroke(gCircle(0f, 0f, 36f), 2f, 0.03f))
    "entity" -> {
        val runtime = persistence == "runtime"
        val out = mutableListOf(
            GlyphStroke(gCircle(0f, 0f, 18f), 0f, 0.15f),
            GlyphStroke(gCircle(0f, 0f, 18f), if (runtime) 2f else 2.5f, dash = if (runtime) listOf(6f, 3f) else emptyList())
        )
        for (i in 0 until 4) {
            val a = Math.PI * 2.0 * i / 4.0 - Math.PI / 2.0
            out.add(GlyphStroke(gLine(20f * Math.cos(a).toFloat(), 20f * Math.sin(a).toFloat(), 28f * Math.cos(a).toFloat(), 28f * Math.sin(a).toFloat()), 1f, alpha = 0.6f))
        }
        out
    }
    "trait" -> listOf(GlyphStroke(gEllipse(0f, 0f, 44f, 22f), 1.5f, dash = listOf(4f, 2f)))
    "page" -> listOf(GlyphStroke(gPoly(true, -10f, -10f, 10f, -10f, 10f, 10f, -10f, 10f), 1.5f, 0.3f))
    "state" -> listOf(GlyphStroke(gRound(0f, 0f, 96f, 34f, 17f), 2f))
    "transition" -> listOf(
        GlyphStroke(gLine(-40f, 0f, 36f, 0f)),
        GlyphStroke(gPoly(true, 40f, 0f, 32f, -3f, 32f, 3f), 0f, 1f)
    )
    "event" -> listOf(GlyphStroke(gPoly(true, -2.5f, -10f, 5f, -10f, 1.25f, -1.25f, 6.25f, -1.25f, -3.75f, 10f, 0f, 1.25f, -5f, 1.25f), 0f, 1f))
    "guard" -> listOf(GlyphStroke(gPoly(true, 0f, -14f, 14f, 0f, 0f, 14f, -14f, 0f), 2f))
    "effect" -> glyphEffectStrokes(effectType, 0f, 0f, 14f)
    "field" -> listOf(GlyphStroke(gLine(0f, 0f, 40f, 0f)), GlyphStroke(gCircle(40f, 0f, 2.5f), 0f, 1f, alpha = 0.6f))
    "fieldType" -> glyphFieldType(fieldType, 0f, 0f, 9f)
    "binding" -> listOf(GlyphStroke(gLine(-40f, 0f, 40f, 0f), 1f, dash = listOf(2f, 3f), alpha = 0.6f))
    "bindingRef" -> listOf(GlyphStroke(gCircle(0f, 0f, maxOf(14f, ("@" + label.ifEmpty { "entity.id" }).length * 3.5f + 4f)), 1.5f, dash = listOf(3f, 2f)))
    "persistence" -> listOf(
        GlyphStroke(
            gLine(-12f, 0f, 12f, 0f), if (persistence == "runtime") 2f else 2.5f,
            dash = if (persistence == "runtime") listOf(6f, 3f) else emptyList()
        )
    )
    "operator" -> listOf(GlyphStroke(gRound(-20f, -14f, 40f, 28f, 4f), 1.5f, 0.15f))
    "sexpr" -> listOf(
        GlyphStroke(gRound(0f, 0f, 110f, 60f, 6f), 1f, 0.05f, dash = listOf(6f, 3f)),
        GlyphStroke(gCircle(12f, 12f, 3f), 0f, 1f, alpha = 0.4f),
        GlyphStroke(gLine(12f, 15f, 8f, 22f), 0.8f, alpha = 0.3f),
        GlyphStroke(gLine(12f, 15f, 16f, 22f), 0.8f, alpha = 0.3f)
    )
    "literal" -> listOf(GlyphStroke(gCircle(0f, 0f, 12f), 1f, alpha = 0.5f))
    else -> emptyList()
}

private fun glyphLabels(kind: String, label: String, namespace: String): List<GlyphLabel> = when (kind) {
    "binding" -> listOf(GlyphLabel("@", 0f, -4f, 10f, true))
    "bindingRef" -> listOf(GlyphLabel("@" + label.ifEmpty { "entity.id" }, 0f, 1f, 10f))
    "operator" -> listOf(GlyphLabel(label.ifEmpty { "+" }, 0f, 1f, 10f, true))
    "literal" -> listOf(GlyphLabel(label.ifEmpty { "42" }, 0f, 1f, 9f))
    else -> emptyList()
}

@Composable
private fun AvlGlyphCanvas(kind: String, label: String, effectType: String, fieldType: String, persistence: String, namespace: String, height: Dp, modifier: Modifier) {
    val palette = LocalAlmadarTheme.current.color
    fun tone(name: String): Color = when (name) {
        "primary" -> palette.primary
        "warning" -> palette.warning
        "accent" -> palette.accent
        "error" -> palette.error
        "info" -> palette.info
        "success" -> palette.success
        "foreground" -> palette.foreground
        "background" -> palette.background
        "muted-foreground" -> palette.mutedForeground
        else -> palette.primary
    }
    val spec = AVL_OPERATOR_TONE[namespace.ifEmpty { "arithmetic" }]
    val ink = if (kind == "operator" && spec != null) {
        androidx.compose.ui.graphics.lerp(tone(spec[2]), tone(spec[0]), spec[1].toFloat() / 100f)
    } else palette.primary
    val box = glyphBox(kind, label)
    val strokes = remember(kind, label, effectType, fieldType, persistence) { glyphStrokes(kind, label, effectType, fieldType, persistence) }
    val paths = remember(strokes) { strokes.map { PathParser().parsePathString(it.d).toPath() } }
    val labels = remember(kind, label, namespace) { glyphLabels(kind, label, namespace) }
    val measurer = rememberTextMeasurer()
    val span = box.h + 2f * GLYPH_PAD
    val width = height * ((box.w + 2f * GLYPH_PAD) / span)
    androidx.compose.foundation.Canvas(modifier = modifier.size(width, height)) {
        val scale = size.height / span
        withTransform({
            scale(scale, scale, androidx.compose.ui.geometry.Offset.Zero)
            translate(-(box.x - GLYPH_PAD), -(box.y - GLYPH_PAD))
        }) {
            strokes.forEachIndexed { index, item ->
                if (item.fill > 0f) drawPath(paths[index], color = ink, alpha = item.fill, style = Fill)
                if (item.width > 0f) {
                    drawPath(
                        paths[index], color = ink, alpha = item.alpha,
                        style = Stroke(width = item.width, pathEffect = if (item.dash.isEmpty()) null else PathEffect.dashPathEffect(item.dash.toFloatArray()))
                    )
                }
            }
            labels.forEach { entry ->
                val layout = measurer.measure(entry.text, androidx.compose.ui.text.TextStyle(fontSize = entry.size.toSp(), fontWeight = if (entry.bold) FontWeight.Bold else null))
                drawText(layout, color = ink, topLeft = androidx.compose.ui.geometry.Offset(entry.x - layout.size.width / 2f, entry.y - layout.size.height / 2f))
            }
        }
    }
}

private fun glyphCaption(kind: String): String = when (kind) {
    "application" -> "Application"
    "orbital" -> "Orbital"
    "entity" -> "Entity"
    "trait" -> "Trait"
    "page" -> "Page"
    "state" -> "State"
    "transition" -> "Transition"
    "event" -> "Event"
    "guard" -> "Guard"
    "effect" -> "Effect"
    "field" -> "Field"
    "fieldType" -> "Field type"
    "binding" -> "Binding"
    "bindingRef" -> "Binding reference"
    "persistence" -> "Persistence"
    "operator" -> "Operator"
    "sexpr" -> "Expression"
    "literal" -> "Literal"
    else -> kind
}

private val AVL_EFFECT_CATEGORY: Map<String, String> = mapOf(
    "render-ui" to "ui",
    "navigate" to "ui",
    "navigate-back" to "ui",
    "set" to "data",
    "persist" to "data",
    "fetch" to "data",
    "fetch-stream" to "data",
    "ref" to "data",
    "deref" to "data",
    "swap" to "data",
    "watch" to "data",
    "emit" to "communication",
    "notify" to "communication",
    "send-server" to "communication",
    "call-service" to "communication",
    "cancel-call" to "communication",
    "integration/http" to "communication",
    "integration/github-get-repo" to "communication",
    "integration/github-create-issue" to "communication",
    "spawn" to "lifecycle",
    "despawn" to "lifecycle",
    "do" to "control",
    "if" to "control",
    "when" to "control",
    "let" to "control",
    "log" to "control",
    "atomic" to "control",
    "wait" to "async",
    "async/delay" to "async",
    "async/debounce" to "async",
    "async/throttle" to "async",
    "async/interval" to "async",
    "async/race" to "async",
    "async/all" to "async",
    "async/sequence" to "async",
    "async/timeout" to "async",
    "async/retry" to "async",
    "forward" to "compute",
    "train" to "compute",
    "evaluate" to "compute",
    "checkpoint/save" to "compute",
    "checkpoint/load" to "compute",
    "nn/setWeights" to "compute",
    "train/loop" to "compute",
    "train/step" to "compute",
    "train/clipGradients" to "compute",
    "train/clipWeights" to "compute",
    "train/sgd" to "compute",
    "train/adam" to "compute",
    "prob/seed" to "compute",
    "prob/condition" to "compute",
    "workspace/write-orbital" to "system",
    "workspace/write-file" to "system",
    "workspace/write-schema" to "system",
    "workspace/write-plan" to "system",
    "workspace/archive-orbital" to "system",
    "behavior/catalog" to "system",
    "behavior/describe" to "system",
    "behavior/source" to "system",
    "program/read" to "system",
    "program/print" to "system",
    "program/eval" to "system",
    "program/compose" to "system"
)

private val AVL_EFFECT_FAMILY: Map<String, String> = mapOf(
    "agent" to "system",
    "os" to "system",
    "browser" to "system",
    "llm" to "system",
    "session" to "system",
    "trace" to "control",
    "memory" to "data",
    "application" to "system"
)

private val AVL_OPERATOR_TONE: Map<String, List<String>> = mapOf(
    "std-behavior-value" to listOf("accent", "40", "success"),
    "std-program" to listOf("accent", "60", "info"),
    "arithmetic" to listOf("primary", "100", "primary"),
    "comparison" to listOf("warning", "100", "warning"),
    "logic" to listOf("accent", "100", "accent"),
    "control" to listOf("error", "100", "error"),
    "effect" to listOf("warning", "60", "error"),
    "collection" to listOf("info", "100", "info"),
    "std-math" to listOf("primary", "100", "primary"),
    "std-str" to listOf("success", "100", "success"),
    "std-array" to listOf("info", "100", "info"),
    "std-object" to listOf("info", "60", "foreground"),
    "std-validate" to listOf("warning", "100", "warning"),
    "std-json" to listOf("success", "60", "foreground"),
    "std-time" to listOf("info", "50", "success"),
    "std-format" to listOf("success", "60", "background"),
    "std-i18n" to listOf("info", "60", "background"),
    "std-async" to listOf("accent", "50", "error"),
    "std-nn" to listOf("primary", "60", "accent"),
    "std-tensor" to listOf("primary", "60", "info"),
    "std-train" to listOf("primary", "60", "warning"),
    "std-prob" to listOf("accent", "60", "warning"),
    "std-os" to listOf("muted-foreground", "60", "primary"),
    "std-browser" to listOf("muted-foreground", "60", "info"),
    "std-composition" to listOf("accent", "60", "foreground"),
    "std-vec" to listOf("primary", "60", "background"),
    "std-geo" to listOf("success", "50", "info"),
    "std-grid" to listOf("info", "60", "background"),
    "std-anim" to listOf("accent", "60", "background"),
    "std-ease" to listOf("accent", "50", "info"),
    "std-noise" to listOf("muted-foreground", "60", "accent"),
    "std-path" to listOf("success", "60", "primary"),
    "ml-arch" to listOf("primary", "70", "foreground"),
    "ml-effect" to listOf("primary", "50", "error"),
    "ml-tensor" to listOf("primary", "50", "info"),
    "ml-graph" to listOf("primary", "50", "success"),
    "ml-contract" to listOf("primary", "50", "warning"),
    "ml-data" to listOf("primary", "50", "background"),
    "std-llm" to listOf("accent", "70", "primary"),
    "std-workspace" to listOf("muted-foreground", "60", "warning"),
    "std-session" to listOf("muted-foreground", "60", "success"),
    "std-memory" to listOf("info", "60", "accent"),
    "std-trace" to listOf("muted-foreground", "100", "muted-foreground"),
    "std-behavior" to listOf("accent", "60", "success"),
    "std-integration" to listOf("warning", "60", "info")
)

@Composable
fun AvlGlyphRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val kind = props.string("kind", "state")
    val label = props.string("label")
    val height = when (props.string("size", "md")) {
        "sm" -> almadarSpaceFor("xl")
        "lg" -> AlmadarSizes.w24
        else -> AlmadarSizes.w14
    }
    Column(
        modifier = modifier.almadarLabel(label.ifEmpty { glyphCaption(kind) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
    ) {
        AvlGlyphCanvas(
            kind, label, props.string("effectType", "set"), props.string("fieldType", "string"),
            props.string("persistence", "persistent"), props.string("namespace", "arithmetic"), height, Modifier
        )
        if (props.bool("showCaption", true)) AlmadarText(label.ifEmpty { glyphCaption(kind) }, variant = "caption", color = "muted")
    }
}
