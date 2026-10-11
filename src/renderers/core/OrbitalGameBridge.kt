package generated.patterns.core

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * JNI bridge to the orbital-canvas native library.
 *
 * All GPU rendering lives in `liborbital_ffi.so` which is loaded
 * once via [System.loadLibrary].  The `external fun` declarations here map
 * to the `Java_generated_patterns_core_OrbitalGameBridge_native*` symbols emitted by
 * `crates/orbital-ffi/src/jni_bridge.rs`.
 *
 * Usage:
 *   val ptr = OrbitalGameBridge.nativeInit(holder.surface, w, h, null)
 *   OrbitalGameBridge.nativeSetScene(ptr, canvasNodeJson)
 *   OrbitalGameBridge.nativeRender(ptr)
 *   // on destroy:
 *   OrbitalGameBridge.nativeDestroy(ptr)
 */
object OrbitalGameBridge {
    init {
        // System.loadLibrary is safe to call once per `object` that needs the
        // library (Android no-ops a repeat load of an already-loaded native
        // library), so OrbitalKernelBridge's own `init` block loading the
        // SAME "orbital_ffi" library is not a double-load bug.
        System.loadLibrary("orbital_ffi")
    }

    /**
     * Create a renderer bound to [surface].
     *
     * [android.view.Surface] has no public ANativeWindow* accessor; the
     * native side resolves it via `ANativeWindow_fromSurface`.
     */
    external fun nativeInit(surface: Surface, width: Int, height: Int, resourceBaseUrl: String?): Long

    /** Destroy a renderer previously created with [nativeInit]. */
    external fun nativeDestroy(rendererPtr: Long)

    /** Submit a JSON-encoded GameRenderPacket to the renderer. */
    external fun nativeSubmit(rendererPtr: Long, jsonPacket: String)

    /**
     * Hand the renderer a `canvas` render-ui node (JSON). The Rust renderer maps it to a
     * display list (children → drawables, camera vocabulary, the drawable walk).
     *
     * @return 1 when accepted; 0 when refused (reason via [nativeLastError]).
     */
    external fun nativeSetScene(rendererPtr: Long, sceneJson: String): Int

    /** Render one frame to the bound surface. */
    external fun nativeRender(rendererPtr: Long)

    /** Notify the renderer that the surface dimensions changed. */
    external fun nativeResize(rendererPtr: Long, width: Int, height: Int)

    /**
     * Set the display density ([android.util.DisplayMetrics.density]). Surface sizes stay in
     * physical pixels; the scene is laid out in `physical / density` logical points.
     *
     * @return 1 when applied; 0 when rejected (reason via [nativeLastError]).
     */
    external fun nativeSetScale(rendererPtr: Long, scale: Float): Int

    /**
     * Hand the renderer the host frame clock in milliseconds (`withFrameNanos`) before
     * [nativeRender]. The first value is scene time zero.
     */
    external fun nativeSetClock(rendererPtr: Long, ms: Double)

    /** Allow (1) or forbid (0) motion; forbidden, animated scenes draw their rest frame. */
    external fun nativeSetMotion(rendererPtr: Long, enabled: Int)

    /** 1 while frames should keep coming (the scene animates, or its assets are arriving), else 0. */
    external fun nativeWantsFrames(rendererPtr: Long): Int

    /**
     * Forward a touch or key input event.
     *
     * @param eventType one of "touch-down", "touch-up", "touch-move", "touch-cancel", "key"
     * @param x         logical points (physical px / display density) for touch events, else 0.0
     * @param y         logical points (physical px / display density) for touch events, else 0.0
     * @param button    pointer index (touch) or 0 (key)
     * @param key       key name for "key" events, or null
     * @return          1 if the event was consumed, 0 otherwise
     */
    external fun nativeInput(rendererPtr: Long, eventType: String, x: Float, y: Float, button: Int, key: String?): Int

    /**
     * Take the scene events that taps queued since the last call: a JSON array of
     * `{"event": name, "payload": {...}}` (`[]` when none, null on failure).
     */
    external fun nativeTakeEvents(rendererPtr: Long): String?

    /**
     * The scene's width / height, so a host can size its surface before a renderer exists;
     * 0 when the scene cannot be mapped.
     */
    external fun nativeSceneAspect(sceneJson: String): Float

    /**
     * The scene's own width in logical units (dp), 0 when the scene cannot be mapped: the cap for a
     * fixed-pixel pattern's surface.
     */
    external fun nativeSceneWidth(sceneJson: String): Float

    /**
     * Map and render a scene once, headless, into RGBA bytes (`width` x `height` physical pixels at
     * density `scale`); null when it cannot be drawn. For JVM snapshot tests, which have no GPU surface.
     */
    external fun nativeRenderSceneToRgba(sceneJson: String, width: Int, height: Int, scale: Float): ByteArray?

    /**
     * Scripts painted text needed and no face covers, each once: a JSON array of
     * `{"script": ISO 15924 short name, "sample": a char of it}` (`[]` when none).
     */
    external fun nativeTakeMissingScripts(): String?

    /** Register face [index] of a system font file for [script]; 1 when accepted, 0 when refused. */
    external fun nativeAddFont(script: String, bytes: ByteArray, index: Int): Int

    /** A pinch zooming a pannable scene (a graph) by [factor] about the focus, in dp. */
    external fun nativeZoom(rendererPtr: Long, factor: Float, x: Float, y: Float)

    /** A view button on a pannable scene: `zoom-in`, `zoom-out` or `reset`; 1 when applied. */
    external fun nativeViewCommand(rendererPtr: Long, command: String): Int

    /** The asset cache counter: moves when a sprite sheet, atlas or image finishes loading. */
    external fun nativeAssetGeneration(): Long

    /** Return the last error message from the renderer, or `""` when nothing has failed. */
    external fun nativeLastError(rendererPtr: Long): String

    /**
     * Headless render: write the current packet to an RGBA byte array.
     *
     * Returns null when no GPU adapter is available or no packet has been
     * submitted.  Used by the Orbital verifier / CI screenshot path.
     */
    external fun nativeRenderToRgba(rendererPtr: Long): ByteArray?
}

/**
 * Answers the renderer's missing scripts from the platform's own fonts: the system fonts whose
 * declared locale names that script (`fonts.xml` `lang="und-Hebr"`), tried in the system's order
 * until the renderer accepts one. Before API 29 there is no font list, so the text stays `.notdef`.
 */
object AssetGeneration {
    /** Read by canvas hosts that size from the scene: a loaded sprite can change its aspect. */
    val state = androidx.compose.runtime.mutableLongStateOf(0L)

    /** Publish the counter after a frame; a change recomposes the hosts that read [state]. */
    fun poll() {
        val now = OrbitalGameBridge.nativeAssetGeneration()
        if (now != state.longValue) state.longValue = now
    }
}

object SystemFontSource {
    /** True when a face was added, so the caller draws again. */
    fun supplyMissing(): Boolean {
        val json = OrbitalGameBridge.nativeTakeMissingScripts() ?: return false
        val missing = Json.parseToJsonElement(json) as? JsonArray ?: return false
        var added = false
        for (item in missing) {
            val script = ((item as? JsonObject)?.get("script") as? JsonPrimitive)?.content ?: continue
            if (supply(script)) added = true else Log.w("OrbitalGame", "no system font for script $script")
        }
        return added
    }

    private fun supply(script: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return false
        for (font in android.graphics.fonts.SystemFonts.getAvailableFonts()) {
            val locales = font.localeList
            val declares = (0 until locales.size()).any { locales.get(it).script == script }
            val file = font.file ?: continue
            if (declares && OrbitalGameBridge.nativeAddFont(script, file.readBytes(), font.ttcIndex) == 1) return true
        }
        return false
    }
}

/**
 * A [SurfaceView] that owns an orbital-canvas instance.
 *
 * The Compose wrapper [OrbitalGameCanvas] below wraps this view.
 * The native renderer is initialized when the surface is created
 * (ANativeWindow becomes valid) and destroyed when the surface is destroyed.
 */
class OrbitalGameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var rendererPtr: Long = 0L
    private var pendingScene: String? = null

    /** Receives each event a tap on the scene produced (name, payload). */
    var onSceneEvent: ((String, JsonElement?) -> Unit)? = null

    /** Told whether the renderer wants a frame loop: true while the scene animates, false once it is static. */
    var onWantsFrames: ((Boolean) -> Unit)? = null

    init {
        holder.addCallback(this)
    }

    /** Set the canvas node (JSON) as the scene; if the renderer is not ready yet it is queued. */
    fun setScene(sceneJson: String) {
        if (sceneJson == pendingScene) return
        pendingScene = sceneJson
        applyScene()
    }

    private fun applyScene() {
        val scene = pendingScene ?: return
        if (rendererPtr == 0L) return
        if (OrbitalGameBridge.nativeSetScene(rendererPtr, scene) == 0) {
            Log.w("OrbitalGame", "set_scene refused: " + OrbitalGameBridge.nativeLastError(rendererPtr))
            return
        }
        applyMotion()
        OrbitalGameBridge.nativeRender(rendererPtr)
        if (SystemFontSource.supplyMissing()) OrbitalGameBridge.nativeRender(rendererPtr)
        AssetGeneration.poll()
        onWantsFrames?.invoke(OrbitalGameBridge.nativeWantsFrames(rendererPtr) == 1)
    }

    /** Draw one frame at the host frame clock; false once the scene is static and the loop can stop. */
    fun frame(nowMs: Long): Boolean {
        if (rendererPtr == 0L) return false
        applyMotion()
        OrbitalGameBridge.nativeSetClock(rendererPtr, nowMs.toDouble())
        OrbitalGameBridge.nativeRender(rendererPtr)
        if (SystemFontSource.supplyMissing()) OrbitalGameBridge.nativeRender(rendererPtr)
        AssetGeneration.poll()
        return OrbitalGameBridge.nativeWantsFrames(rendererPtr) == 1
    }

    /** "Remove animations" (animator duration scale 0) is Android's reduced-motion setting. */
    private fun applyMotion() {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        OrbitalGameBridge.nativeSetMotion(rendererPtr, if (scale == 0f) 0 else 1)
    }

    private fun applyScale() {
        if (rendererPtr == 0L) return
        if (OrbitalGameBridge.nativeSetScale(rendererPtr, resources.displayMetrics.density) == 0) {
            Log.w("OrbitalGame", "set_scale refused: " + OrbitalGameBridge.nativeLastError(rendererPtr))
        }
    }

    /** Pinch zooms a pannable scene; the renderer ignores it for every other scene. */
    private val pinch = android.view.ScaleGestureDetector(
        context,
        object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                val density = resources.displayMetrics.density
                OrbitalGameBridge.nativeZoom(rendererPtr, detector.scaleFactor, detector.focusX / density, detector.focusY / density)
                OrbitalGameBridge.nativeRender(rendererPtr)
                return true
            }
        },
    )

    /** Double tap fires a region's double-click event (`onNodeDoubleClick`); a long press is hover. */
    private val doubleTap = android.view.GestureDetector(
        context,
        object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                val density = resources.displayMetrics.density
                OrbitalGameBridge.nativeInput(rendererPtr, "long-press", e.x / density, e.y / density, 0, null)
                OrbitalGameBridge.nativeRender(rendererPtr)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val density = resources.displayMetrics.density
                OrbitalGameBridge.nativeInput(rendererPtr, "double-tap", e.x / density, e.y / density, 0, null)
                drainSceneEvents()
                return true
            }
        },
    )

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (rendererPtr == 0L) return super.onTouchEvent(event)
        pinch.onTouchEvent(event)
        doubleTap.onTouchEvent(event)
        val density = resources.displayMetrics.density
        if (event.pointerCount > 1 || pinch.isInProgress) {
            // A second finger turns the gesture into a pinch: the one-finger pan or drag stops.
            if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                OrbitalGameBridge.nativeInput(rendererPtr, "touch-cancel", event.x / density, event.y / density, 0, null)
            }
            return true
        }
        val type = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> "touch-down"
            MotionEvent.ACTION_UP -> "touch-up"
            MotionEvent.ACTION_MOVE -> "touch-move"
            MotionEvent.ACTION_CANCEL -> "touch-cancel"
            else -> return super.onTouchEvent(event)
        }
        OrbitalGameBridge.nativeInput(rendererPtr, type, event.x / density, event.y / density, 0, null)
        if (type == "touch-move") OrbitalGameBridge.nativeRender(rendererPtr)
        if (type == "touch-up" || type == "touch-move") drainSceneEvents()
        return true
    }

    private fun drainSceneEvents() {
        val json = OrbitalGameBridge.nativeTakeEvents(rendererPtr) ?: return
        val events = Json.parseToJsonElement(json) as? JsonArray ?: return
        for (item in events) {
            val obj = item as? JsonObject ?: continue
            val name = (obj["event"] as? JsonPrimitive)?.content ?: continue
            onSceneEvent?.invoke(name, obj["payload"])
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val surface = holder.surface
        val w = width.takeIf { it > 0 } ?: 320
        val h = height.takeIf { it > 0 } ?: 240
        rendererPtr = OrbitalGameBridge.nativeInit(surface, w, h, null)
        applyScale()
        applyScene()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (rendererPtr != 0L) {
            OrbitalGameBridge.nativeResize(rendererPtr, width, height)
            applyScene()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (rendererPtr != 0L) {
            OrbitalGameBridge.nativeDestroy(rendererPtr)
            rendererPtr = 0L
        }
    }
}
