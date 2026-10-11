package generated.patterns.core

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Jetpack Compose wrapper around [OrbitalGameSurfaceView].
 *
 * Hosts the native SurfaceView inside a Compose hierarchy so that
 * game-canvas/canvas/game-canvas-2d patterns are routed to the
 * orbital-canvas GPU path instead of the placeholder Box composable.
 *
 * @param sceneJson  The `canvas` render-ui node as JSON; the Rust renderer maps it to a
 *                   display list (`mappers::game_canvas_2d::from_canvas_node`).
 * @param modifier   Standard Compose modifier.
 */
@Composable
fun OrbitalGameCanvas(
    sceneJson: String,
    modifier: Modifier = Modifier,
    onSceneEvent: ((String, JsonElement?) -> Unit)? = null,
) {
    val context = LocalContext.current
    val surfaceView = remember { OrbitalGameSurfaceView(context) }
    var animating by remember { mutableStateOf(false) }

    // The frame loop runs only while the scene animates: the host frame clock (withFrameNanos, the
    // clock the kernel loop uses) is read per frame and a static scene never starts it.
    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        while (true) {
            val nowMs = withFrameNanos { it / 1_000_000L }
            if (!surfaceView.frame(nowMs)) {
                animating = false
                break
            }
        }
    }

    AndroidView(
        factory = { surfaceView },
        modifier = modifier,
        update = { view ->
            view.onSceneEvent = onSceneEvent
            view.onWantsFrames = { animating = it }
            view.setScene(sceneJson)
        },
    )
}

/**
 * Registered in `CorePatternRegistry` for `canvas` / `game-canvas` / `canvas-2d` /
 * `game-canvas-2d` — hands the node's props (JSON) to the Rust renderer through
 * `OrbitalGameBridge.nativeSetScene`, which owns the children → drawables routing,
 * camera vocabulary and drawable walk (shared with the desktop shells). The draw-*
 * children are never composed as Compose nodes.
 */
@Composable
fun GameCanvas2dRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit,
) {
    OrbitalGameCanvas(sceneJson = props.toString(), modifier = modifier.fillMaxSize())
}

/**
 * Registered in `CorePatternRegistry` for the learning canvases (`math-canvas`,
 * `physics-canvas`, ...). The Rust renderer draws them (`mappers::learning`) on the same
 * surface the game boards use; this host sizes the surface to the scene's own aspect ratio,
 * shows the pattern's `title` above it, and forwards taps as the pattern's authored events
 * (`onShapeClick`, ...) through [onEvent]. No drawing happens here. A [fixedSize] pattern (the web
 * draws it in fixed CSS pixels) is capped at the scene's own width instead of filling the column.
 */
/**
 * Whether the Rust renderer draws [pattern] with [props]: a host composing a scene (a mermaid fence
 * in markdown) shows its own fallback when this is false.
 */
fun orbitalSceneDrawable(pattern: String, props: JsonElement): Boolean {
    val fields = (props as? JsonObject)?.toMutableMap() ?: mutableMapOf()
    fields["type"] = JsonPrimitive(pattern)
    return OrbitalGameBridge.nativeSceneAspect(JsonObject(fields).toString()) > 0f
}

@Composable
fun LearningCanvasRenderer(
    pattern: String,
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit,
    fixedSize: Boolean = false,
) {
    val isDark = LocalAlmadarThemeMode.current.isDark
    val canvasTheme = LocalAlmadarCanvasTheme.current
    val sceneJson = remember(pattern, props, isDark, canvasTheme) {
        val fields = (props as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        fields["type"] = JsonPrimitive(pattern)
        fields["colorScheme"] = JsonPrimitive(if (isDark) "dark" else "light")
        canvasTheme?.let { fields["canvasTheme"] = it }
        JsonObject(fields).toString()
    }
    val assets = AssetGeneration.state.longValue
    val aspect = remember(sceneJson, assets) { OrbitalGameBridge.nativeSceneAspect(sceneJson) }
    if (aspect <= 0f) {
        Log.w("OrbitalGame", "$pattern scene refused by the native painter")
        return
    }
    val title = props.string("title")
    val naturalSize = LocalAlmadarCanvasNaturalSize.current
    Column(modifier = if (naturalSize) modifier else modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title.isNotEmpty()) {
            TypographyRenderer(
                JsonObject(mapOf("variant" to JsonPrimitive("h4"), "content" to JsonPrimitive(title))),
                Modifier,
                onEvent,
            )
        }
        val naturalWidth = remember(sceneJson, assets) {
            if (fixedSize || naturalSize) OrbitalGameBridge.nativeSceneWidth(sceneJson) else 0f
        }
        val canvasModifier = when {
            naturalSize && naturalWidth > 0f -> Modifier.size(naturalWidth.dp, (naturalWidth / aspect).dp)
            naturalWidth > 0f -> Modifier.widthIn(max = naturalWidth.dp).fillMaxWidth().aspectRatio(aspect)
            else -> Modifier.fillMaxWidth().aspectRatio(aspect)
        }
        when (LocalOrbitalCanvasTarget.current) {
            OrbitalCanvasTarget.Surface -> OrbitalGameCanvas(
                sceneJson = sceneJson,
                modifier = canvasModifier,
                onSceneEvent = { name, payload -> onEvent(name, eventEnvelope(props, payload)) },
            )
            OrbitalCanvasTarget.Raster -> OrbitalGameRaster(sceneJson, canvasModifier)
        }
    }
}

/** How a canvas scene reaches pixels: the GPU surface of an app, or a one-shot Rust raster for JVM snapshot tests. */
enum class OrbitalCanvasTarget { Surface, Raster }

val LocalOrbitalCanvasTarget = compositionLocalOf { OrbitalCanvasTarget.Surface }

/** The app theme's resolved `canvasTheme` from the kernel snapshot; null outside an app (snapshot tests). */
val LocalAlmadarCanvasTheme = compositionLocalOf<JsonElement?> { null }

/**
 * The app theme's resolved motion (orbital-core `resolve_motion`) from the kernel snapshot: what an
 * element entry and a surface's enter shape animate. Null outside an app (snapshot tests): nothing animates.
 */
val LocalAlmadarResolvedMotion = compositionLocalOf<JsonElement?> { null }

/**
 * Provided by a host that lays its children out at their natural size (`scaled-diagram`): a canvas
 * takes its scene's own size instead of filling the column.
 */
val LocalAlmadarCanvasNaturalSize = compositionLocalOf { false }

/**
 * Shows a scene the Rust renderer drew once, headless, at this composable's pixel size and the
 * display density (`OrbitalGameBridge.nativeRenderSceneToRgba`) — the same mapper and renderer the
 * surface path runs, with no GPU surface. Used by snapshot tests only; apps use [OrbitalGameCanvas].
 */
@Composable
fun OrbitalGameRaster(sceneJson: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }
        val bitmap = remember(sceneJson, widthPx, heightPx, density.density) {
            OrbitalGameBridge.nativeRenderSceneToRgba(sceneJson, widthPx, heightPx, density.density)
                ?.let { rgbaToBitmap(it, widthPx, heightPx) }
        }
        if (bitmap == null) {
            Log.w("OrbitalGame", "raster render refused (no GPU adapter or a scene the native painter refuses)")
        } else {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun rgbaToBitmap(rgba: ByteArray, width: Int, height: Int): Bitmap {
    val pixels = IntArray(width * height) { i ->
        val o = i * 4
        ((rgba[o + 3].toInt() and 0xFF) shl 24) or ((rgba[o].toInt() and 0xFF) shl 16) or
            ((rgba[o + 1].toInt() and 0xFF) shl 8) or (rgba[o + 2].toInt() and 0xFF)
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}
