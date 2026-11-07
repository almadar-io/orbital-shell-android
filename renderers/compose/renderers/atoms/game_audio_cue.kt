package generated.patterns.core

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.math.min

/** A sound std-audio-cue resolved; the host plays it as given. */
private data class Sound(
    val key: String, val source: String, val volume: Float, val loop: Boolean,
    val poolSize: Int, val fadeMs: Long
)

private fun JsonElement?.sound(): Sound? {
    val plan = this as? JsonObject ?: return null
    fun text(name: String) = (plan[name] as? JsonPrimitive)?.contentOrNull
    val sound = Sound(
        text("key") ?: return invalidPlan(plan),
        text("source") ?: return invalidPlan(plan),
        text("volume")?.toFloatOrNull() ?: return invalidPlan(plan),
        (plan["loop"] as? JsonPrimitive)?.booleanOrNull ?: return invalidPlan(plan),
        (plan["poolSize"] as? JsonPrimitive)?.intOrNull ?: return invalidPlan(plan),
        text("fadeMs")?.toDoubleOrNull()?.toLong() ?: return invalidPlan(plan)
    )
    return sound
}

private fun invalidPlan(plan: JsonObject): Sound? {
    Log.e("OrbitalAudio", "Invalid game-audio-cue plan: $plan")
    return null
}

private class AudioDriver(private val context: Context) {
    private class Voice(val player: MediaPlayer, val sound: Sound) {
        var prepared = false
        var onReady: (() -> Unit)? = null
    }
    private val handler = Handler(Looper.getMainLooper())
    private val effects = mutableMapOf<String, MutableList<Voice>>()
    private var music: Voice? = null
    private var musicKey = ""
    private var fade: Runnable? = null
    private var fadeOutgoing: Voice? = null
    private var synced = false
    private var priorCueSeq: Double? = null
    private var priorMusic: Sound? = null
    private var unlocked = false
    private var paused = false
    private var muted = false
    private var volume = 1f

    fun sync(props: JsonElement) {
        muted = props.bool("muted")
        volume = props.float("volume", 1f)
        val obj = props as? JsonObject
        val cueSeq = (obj?.get("cueSeq") as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
        val music = obj?.get("musicPlay").sound()
        if ((!synced || music != priorMusic) && music != null) changeMusic(music)
        val before = priorCueSeq
        if (synced && before != null && cueSeq != null && before != cueSeq) {
            obj?.get("cuePlay").sound()?.takeIf { it.key.isNotEmpty() }?.let(::play)
        }
        synced = true
        priorCueSeq = cueSeq
        priorMusic = music
        if (!unlocked && !muted) {
            (obj?.get("autostart") as? JsonArray)?.forEach { item -> item.sound()?.let(::play) }
            unlocked = true
        }
        applyVolumes()
    }

    private fun level(sound: Sound): Float = if (muted || paused) 0f else min(1f, sound.volume * volume)

    private fun voice(key: String, sound: Sound, path: String): Voice? {
        val player = MediaPlayer()
        return try {
            val source = path
            if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("file://")) {
                player.setDataSource(context, Uri.parse(source))
            } else {
                context.assets.openFd(source.removePrefix("/")).use {
                    player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
            }
            val voice = Voice(player, sound)
            player.isLooping = sound.loop
            player.setOnPreparedListener {
                voice.prepared = true
                if (!paused) it.start()
                voice.onReady?.invoke()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e("OrbitalAudio", "Playback failed for $key: $what/$extra")
                true
            }
            player.prepareAsync()
            voice
        } catch (error: Exception) {
            Log.e("OrbitalAudio", "Source failed for $key", error)
            player.release()
            null
        }
    }

    private fun play(sound: Sound) {
        val key = sound.key
        if (muted) return
        val pool = effects.getOrPut(key) { mutableListOf() }
        if (pool.size >= sound.poolSize) {
            if (sound.loop) return
            pool.removeAt(0).player.release()
        }
        val voice = voice(key, sound, sound.source) ?: return
        voice.player.setVolume(level(sound), level(sound))
        voice.player.setOnCompletionListener {
            pool.remove(voice)
            it.release()
        }
        pool.add(voice)
    }

    private fun changeMusic(sound: Sound) {
        val key = sound.key
        fade?.let(handler::removeCallbacks)
        fade = null
        fadeOutgoing?.player?.release()
        fadeOutgoing = null
        val outgoing = music
        music = null
        musicKey = key
        if (key.isEmpty()) {
            fadeOut(outgoing, sound.fadeMs)
            return
        }
        val incoming = voice(key, sound, sound.source) ?: run {
            outgoing?.player?.release()
            return
        }
        incoming.player.isLooping = true
        music = incoming
        if (outgoing == null || sound.fadeMs == 0L) {
            outgoing?.player?.release()
            incoming.player.setVolume(level(sound), level(sound))
            return
        }
        incoming.player.setVolume(0f, 0f)
        fadeOutgoing = outgoing
        var began = 0L
        val oldLevel = level(outgoing.sound)
        val task = object : Runnable {
            override fun run() {
                val progress = ((SystemClock.uptimeMillis() - began).toFloat() / sound.fadeMs).coerceIn(0f, 1f)
                incoming.player.setVolume(level(sound) * progress, level(sound) * progress)
                outgoing.player.setVolume(oldLevel * (1f - progress), oldLevel * (1f - progress))
                if (progress < 1f && !paused) handler.postDelayed(this, 50)
                else { outgoing.player.release(); fadeOutgoing = null; fade = null }
            }
        }
        fade = task
        incoming.onReady = {
            began = SystemClock.uptimeMillis()
            handler.post(task)
        }
    }

    private fun fadeOut(outgoing: Voice?, fadeMs: Long) {
        if (outgoing == null) return
        if (fadeMs == 0L || paused) { outgoing.player.release(); return }
        fadeOutgoing = outgoing
        val began = SystemClock.uptimeMillis()
        val oldLevel = level(outgoing.sound)
        val task = object : Runnable {
            override fun run() {
                val progress = ((SystemClock.uptimeMillis() - began).toFloat() / fadeMs).coerceIn(0f, 1f)
                outgoing.player.setVolume(oldLevel * (1f - progress), oldLevel * (1f - progress))
                if (progress < 1f && !paused) handler.postDelayed(this, 50)
                else { outgoing.player.release(); fadeOutgoing = null; fade = null }
            }
        }
        fade = task
        handler.post(task)
    }

    private fun applyVolumes() {
        music?.let { it.player.setVolume(level(it.sound), level(it.sound)) }
        effects.values.flatten().forEach { it.player.setVolume(level(it.sound), level(it.sound)) }
    }

    fun suspend() {
        paused = true
        fade?.let(handler::removeCallbacks)
        fadeOutgoing?.player?.release()
        fadeOutgoing = null
        fade = null
        music?.takeIf { it.prepared }?.player?.pause()
        effects.values.flatten().filter { it.prepared }.forEach { it.player.pause() }
    }

    fun resume() {
        paused = false
        music?.takeIf { it.prepared }?.player?.start()
        effects.values.flatten().filter { it.prepared }.forEach { it.player.start() }
        applyVolumes()
    }

    fun close() {
        fade?.let(handler::removeCallbacks)
        fadeOutgoing?.player?.release()
        effects.values.flatten().forEach { it.player.release() }
        effects.clear()
        music?.player?.release()
        music = null
    }
}

@Composable
fun GameAudioCueRenderer(props: JsonElement, modifier: Modifier, onEvent: (String, JsonElement?) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val driver = remember(context) { AudioDriver(context.applicationContext) }
    SideEffect { driver.sync(props) }
    DisposableEffect(driver, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> driver.suspend()
                Lifecycle.Event.ON_START -> driver.resume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); driver.close() }
    }
}
