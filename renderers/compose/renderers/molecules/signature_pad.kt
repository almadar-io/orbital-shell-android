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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

private fun signatureDataUrl(strokes: List<List<Offset>>, size: IntSize, color: Int, width: Float): String? {
    if (size.width <= 0 || size.height <= 0) return null
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        this.color = color
        strokeWidth = width
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    strokes.filter { it.isNotEmpty() }.forEach { stroke ->
        val path = android.graphics.Path()
        path.moveTo(stroke[0].x, stroke[0].y)
        if (stroke.size == 1) path.lineTo(stroke[0].x, stroke[0].y)
        stroke.drop(1).forEach { path.lineTo(it.x, it.y) }
        canvas.drawPath(path, paint)
    }
    val bytes = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
    return "data:image/png;base64," + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
}

@Composable
fun SignaturePadRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val readOnly = props.bool("readOnly")
    val value = props.string("value")
    val label = props.string("label", "Signature")
    val helper = props.string("helperText", "Draw your signature above")
    val signEvent = props.string("signEvent")
    val changeEvent = props.string("onChange")
    val clearEvent = props.string("clearEvent")
    val ink = props.string("strokeColor").takeIf { it.isNotEmpty() }?.let { almadarCssColor(it, palette.foreground) } ?: palette.foreground
    val inkWidth = (props as? JsonObject)?.get("strokeWidth")?.let { (it as? JsonPrimitive)?.floatOrNull } ?: 2f
    val padHeight = ((props as? JsonObject)?.get("height")?.let { (it as? JsonPrimitive)?.floatOrNull } ?: 200f).dp
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    val signed = strokes.isNotEmpty() || current.isNotEmpty() || value.isNotEmpty()
    val density = LocalDensity.current.density
    fun snapshot(): JsonElement = signatureDataUrl(strokes.toList(), padSize, ink.toArgb(), inkWidth * density)?.let { JsonPrimitive(it) } ?: JsonNull
    if (props.bool("isLoading")) {
        LoadingStateRenderer(almadarJson("message" to "Loading...", "sourceTrait" to almadarTrait(props)), modifier, onEvent)
    } else if (props.jsonObject("error") != null) {
        ErrorStateRenderer(almadarJson("title" to "Signature pad error", "message" to (props.jsonObject("error")?.string("message") ?: ""), "sourceTrait" to almadarTrait(props)), modifier, onEvent)
    } else {
        val shape = almadarShape(AlmadarCorner.Container)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(palette.card, shape)
                .then(almadarBorder(AlmadarCorner.Container, geometry.borderStandard, palette.border))
                .padding(almadarSpace("space4"))
                .almadarLabel(props.string("aria-label")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
        ) {
            if (label.isNotEmpty()) AlmadarText(label, variant = "label", weight = "medium")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(padHeight)
                    .alpha(if (readOnly) 0.6f else 1f)
                    .clip(shape)
                    .background(palette.background)
                    .then(almadarBorder(AlmadarCorner.Container, geometry.borderStandard, palette.border))
                    .onSizeChanged { padSize = it }
                    .almadarLabel(label)
                    .pointerInput(readOnly) {
                        if (!readOnly) {
                            detectDragGestures(
                                onDragStart = { start -> current = listOf(start) },
                                onDrag = { change, _ -> current = current + change.position },
                                onDragEnd = {
                                    strokes.add(current)
                                    current = emptyList()
                                    if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, almadarJson("value" to snapshot())))
                                },
                                onDragCancel = { current = emptyList() }
                            )
                        }
                    }
            ) {
                if (value.isNotEmpty() && strokes.isEmpty() && current.isEmpty()) {
                    ImageRenderer(almadarJson("src" to value, "alt" to label, "fit" to "contain", "sourceTrait" to almadarTrait(props)), Modifier.fillMaxSize(), onEvent)
                }
                Canvas(Modifier.fillMaxSize()) {
                    (strokes.toList() + listOf(current)).filter { it.isNotEmpty() }.forEach { stroke ->
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(stroke[0].x, stroke[0].y)
                            if (stroke.size == 1) lineTo(stroke[0].x, stroke[0].y)
                            stroke.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, ink, style = Stroke(width = inkWidth * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
            if (helper.isNotEmpty()) AlmadarText(helper, variant = "caption", color = "secondary")
            if (!readOnly) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"), Alignment.End)) {
                    AlmadarLocalButton(
                        props, "Clear",
                        {
                            strokes.clear()
                            current = emptyList()
                            if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, almadarJson("value" to JsonNull)))
                            if (clearEvent.isNotEmpty()) onEvent(clearEvent, eventEnvelope(props))
                        },
                        variant = "ghost", icon = "eraser", size = "sm", disabled = !signed
                    )
                    if (signEvent.isNotEmpty()) {
                        AlmadarButton(
                            props, "Confirm", signEvent,
                            { _, _ -> onEvent(signEvent, eventEnvelope(props, almadarJson("signature" to snapshot()))) },
                            icon = "check", size = "sm", disabled = !signed
                        )
                    }
                }
            }
        }
    }
}
