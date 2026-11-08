package generated.patterns.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * JNI bridge to the in-app `orbital_core::runtime::RuntimeKernel` (B5/B7,
 * ledger §159). The `external fun` declarations here map to the
 * `Java_generated_patterns_core_OrbitalKernelBridge_native*` symbols emitted by
 * `crates/orbital-ffi/src/jni_kernel_bridge.rs`, which forward to
 * `kernel_c_api.rs`'s `orbital_kernel_*` C ABI.
 *
 * `orbital_ffi` is the SAME native library [OrbitalGameBridge] loads —
 * `System.loadLibrary` is safe to call once per `object` that needs it
 * (Android no-ops a repeat load of an already-loaded native library), so
 * this `init` block loading it again is not a double-load bug.
 *
 * String args/returns are declared as real Kotlin `String`/`String?` — the
 * JSON envelopes this bridge exchanges are meant to be decoded, so a caller
 * needs actual string content. The native side marshals every `JString` for
 * real (`jni::JNIEnv::get_string`/`new_string`, `crates/orbital-ffi/src/jni_kernel_bridge.rs`
 * via `jni_util.rs`) — a `jstring` is an opaque JNI reference, not a
 * `*const c_char`.
 */
object OrbitalKernelBridge {
    init {
        System.loadLibrary("orbital_ffi")
    }

    /** Create a kernel handle from an ALREADY-RESOLVED schema JSON; 0 on failure. */
    external fun nativeInit(resolvedSchemaJson: String, clientId: String): Long
    external fun nativeInitFromFile(path: String, clientId: String): Long

    /** Destroy a handle previously created with [nativeInit]. */
    external fun nativeDestroy(handle: Long)

    /** Dispatch one event; returns the `KernelDispatchResult` JSON envelope, or null. */
    external fun nativeDispatch(
        handle: Long,
        traitName: String,
        entityId: String?,
        event: String,
        payloadJson: String?
    ): String?

    /** Fold a server response into the kernel; returns the `{"applied": [...]}` JSON, or null. */
    external fun nativeApplyServerResponse(
        handle: Long,
        responseJson: String,
        alreadyDeliveredJson: String?
    ): String?

    /** Roll a trait instance back to a prior snapshot (`RuntimeOptimistic` rollback). */
    /** Roll a trait back; returns the repaint (`KernelEventResponse` JSON) the rollback owes bound traits. */
    external fun nativeRestore(handle: Long, traitName: String, entityId: String?, snapshotJson: String): String?


    /** The `KernelFrameDelta` JSON a painter at version [since] of the kernel's frames needs. */
    external fun nativeFramesSince(handle: Long, since: Long): String?

    /** Run one scheduled tick; returns the resulting `KernelEventResponse` JSON, or null. */
    external fun nativeTick(handle: Long, traitName: String, tickName: String, entityId: String?): String?

    /** Run one host frame of the native tick clock; null when no tick fired, else the `KernelFrameWire` JSON. */
    external fun nativeFrame(handle: Long, nowMs: Long): String?

    /** Stop the native tick clock (app backgrounded); ticks and their phase are kept. */
    external fun nativeFramePause(handle: Long)

    /** Restart it after [nativeFramePause]; the paused span is never counted. */
    external fun nativeFrameResume(handle: Long)

    /** How many `(trait, tick)` pairs the schema declares. */
    external fun nativeTickCount(handle: Long): Int

    /** Current state name for a trait instance, or null when unset. */
    external fun nativeGetState(handle: Long, traitName: String, entityId: String?): String?

    /** `{"states": {...}, "theme": KernelResolvedTheme}` for the handle's bound orbital. */
    external fun nativeSnapshot(handle: Long): String?

    /** The rows a `RowSetRef` JSON resolves to now for the current viewer: `{"rows": [...], "total": N}`, or an `{"error": ...}` JSON. */
    external fun nativeResolveRows(handle: Long, refJson: String): String?

    /** Set the kernel's tracked light/dark toggle (`"light"|"dark"`). */
    external fun nativeSetThemeMode(handle: Long, mode: String)

    /** Report whether a trait's rendered output is on screen, so its `[visible]` ticks run only then. */
    external fun nativeSetTraitOnScreen(handle: Long, traitName: String, onScreen: Boolean)

    /** Seed `entityType` rows (a JSON array) into the handle's in-memory entity cache. */
    external fun nativeSeed(handle: Long, entityType: String, rowsJson: String)

    /** Declare a new mount (a JSON array of trait names) before its INITs fire. */
    external fun nativeRemount(handle: Long, traitsJson: String)
    external fun nativeMount(handle: Long, traitsJson: String)

    /** Strings the library returned that were not freed yet (the JNI forwarders free every result, so this reads 0). */
    external fun nativeOutstandingStrings(): Long

    /** The handle's last recorded error message. */
    external fun nativeLastError(handle: Long): String
}

// MARK: - Wire models
// The kernel's wire types are generated from orbital-core (`generated/KernelWire.kt`); this file names the
// ones the host uses and keeps the host-side view of the frames it holds.

typealias KernelEventSource = KernelBusEventSource
typealias KernelServerLeg = KernelEventRequest
typealias KernelSeed = KernelSeedWire
typealias KernelDispatchResult = KernelDispatchWire
typealias KernelFrameResult = KernelFrameWire
typealias KernelAppliedResult = KernelAppliedWire
typealias KernelTraitEventPair = KernelTupleStringString

val KernelTraitEventPair.trait: String get() = first
val KernelTraitEventPair.event: String get() = second

/** A snapshot starts from nothing: `from` 0 and a leading `clear`. */
val KernelFrameDelta.isSnapshot: Boolean
    get() = from == 0L && ((ops.firstOrNull() as? JsonArray)?.firstOrNull() as? JsonPrimitive)?.contentOrNull == "clear"

enum class KernelFrameApply { APPLIED, STALE, GAP }

/** The frames a view holds and the version of the kernel's record they are at; the kernel owns the reconciliation. */
class KernelFrameView {
    var session: String? = null
        private set
    var version: Long = 0
        private set
    var frames: Map<String, JsonObject> = emptyMap()
        private set

    fun apply(delta: KernelFrameDelta): KernelFrameApply {
        val deltaSession = delta.session
        if (deltaSession != null) {
            if (!delta.isSnapshot) {
                val current = deltaSession == session && delta.from == version
                if (deltaSession == session && delta.to <= version && !current) return KernelFrameApply.STALE
                if (!current) return KernelFrameApply.GAP
            }
            session = deltaSession
            version = delta.to
        }
        for (op in delta.ops.mapNotNull { it as? JsonArray }) {
            when (op.firstOrNull()?.jsonPrimitive?.contentOrNull) {
                "clear" -> frames = emptyMap()
                "del" -> op.getOrNull(1)?.jsonPrimitive?.contentOrNull?.let { frames = frames - it }
                "put" -> {
                    val frame = op.getOrNull(1)?.jsonPrimitive?.contentOrNull
                    val fields = op.getOrNull(2) as? JsonObject
                    if (frame != null && fields != null) frames = frames + (frame to fields)
                }
                "set" -> (op.getOrNull(1) as? JsonObject)?.forEach { (frame, written) ->
                    val fields = written as? JsonObject ?: return@forEach
                    frames = frames + (frame to JsonObject((frames[frame] ?: JsonObject(emptyMap())) + fields))
                }
            }
        }
        return KernelFrameApply.APPLIED
    }
}

/** Mirror of `orbital_core::runtime::ResolvedTheme` (`key`/`base`/`mode`/`tokens`);
 * `mode` stays a raw `"light"|"dark"` string here (not [ColorMode]) so decoding
 * never fails on a value the mapping hasn't been extended for. */
@Serializable
data class KernelResolvedTheme(
    val key: String,
    val base: String,
    val mode: String,
    val tokens: ThemeTokensJson
)

/** Why a call into the kernel gave no usable answer: the kernel said so, or its answer is not the wire shape. */
sealed class KernelWireException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Native(message: String) : KernelWireException(message)
    class Decode(val type: String, cause: Throwable) : KernelWireException("the kernel's answer is not a $type: ${cause.message}", cause)
}

@Serializable
private data class KernelErrorWire(val error: String)

/** What one [OrbitalKernel.frame] call produced. */
sealed class KernelFrameOutcome {
    /** No tick was due (or none passed its guard): nothing to fold. */
    object Idle : KernelFrameOutcome()
    data class Fired(val frame: KernelFrameResult) : KernelFrameOutcome()
    data class Failed(val message: String) : KernelFrameOutcome()
}

/** [OrbitalKernel.snapshot] result: every trait's current state plus the resolved `@currentTheme`. */
data class KernelSnapshot(val states: Map<String, String>, val theme: KernelResolvedTheme?, val canvasTheme: JsonElement? = null, val motion: JsonElement? = null)

private val kernelJson = Json { ignoreUnknownKeys = true }

/**
 * Owns one native kernel handle for the lifetime of the app's kernel —
 * `RuntimeKernel` in a `Client` environment, embedded via `orbital-ffi`.
 * Generated view models (B7) load this off the main thread and drive
 * `dispatch`/`applyServerResponse` from it; render-ui content still flows
 * through the existing splice/`CorePatternRegistry` path.
 */
class OrbitalKernel private constructor(private val handle: Long) {
    companion object {
        /**
         * `resolvedSchemaJson` is the ALREADY-RESOLVED schema
         * (`resolved_schema.json`, embedded into `assets/` at compile time,
         * B6) — no compiler runs on device. Returns `null` when the native
         * side fails to parse/register it.
         */
        fun create(resolvedSchemaJson: String, clientId: String): OrbitalKernel? {
            val handle = OrbitalKernelBridge.nativeInit(resolvedSchemaJson, clientId)
            return if (handle == 0L) null else OrbitalKernel(handle)
        }

        /** [create] with the resolved schema read natively from [path], so the program never becomes a JVM string. */
        fun createFromFile(path: String, clientId: String): OrbitalKernel? {
            val handle = OrbitalKernelBridge.nativeInitFromFile(path, clientId)
            return if (handle == 0L) null else OrbitalKernel(handle)
        }
    }

    /** The native side's own last error, read right after a `null`/decode-failure return. */
    val lastError: String
        get() = OrbitalKernelBridge.nativeLastError(handle)

    fun destroy() {
        OrbitalKernelBridge.nativeDestroy(handle)
    }

    private fun answer(raw: String?): String =
        raw ?: throw KernelWireException.Native(lastError.ifEmpty { "the kernel gave no answer" })

    private inline fun <reified T> decode(raw: String): T =
        try {
            kernelJson.decodeFromString<T>(raw)
        } catch (failure: SerializationException) {
            val native = runCatching { kernelJson.decodeFromString(KernelErrorWire.serializer(), raw).error }.getOrNull()
            throw native?.let { KernelWireException.Native(it) } ?: KernelWireException.Decode(T::class.simpleName ?: "wire type", failure)
        }

    fun dispatch(trait: String, entityId: String?, event: String, payloadJson: String?): KernelDispatchResult =
        decode(answer(OrbitalKernelBridge.nativeDispatch(handle, trait, entityId, event, payloadJson)))

    fun applyServerResponse(responseJson: String, alreadyDelivered: List<KernelTraitEventPair>): KernelAppliedResult {
        val alreadyDeliveredJson: String = kernelJson.encodeToString(alreadyDelivered)
        return decode(answer(OrbitalKernelBridge.nativeApplyServerResponse(handle, responseJson, alreadyDeliveredJson)))
    }

    /** The delta a painter at version [since] of this kernel's frames needs: empty when current, else a snapshot. */
    fun framesSince(since: Long): KernelFrameDelta = decode(answer(OrbitalKernelBridge.nativeFramesSince(handle, since)))

    fun restore(trait: String, entityId: String?, snapshot: KernelTraitSnapshot): KernelEventResponse {
        val snapshotJson = kernelJson.encodeToString(KernelTraitSnapshot.serializer(), snapshot)
        return decode(answer(OrbitalKernelBridge.nativeRestore(handle, trait, entityId, snapshotJson)))
    }

    fun tick(trait: String, tickName: String, entityId: String?): KernelEventResponse =
        decode(answer(OrbitalKernelBridge.nativeTick(handle, trait, tickName, entityId)))

    /** Runs one host frame of the native tick clock (`nowMs` = the host's monotonic frame time). */
    fun frame(nowMs: Long): KernelFrameOutcome {
        val raw = OrbitalKernelBridge.nativeFrame(handle, nowMs) ?: return KernelFrameOutcome.Idle
        return try {
            KernelFrameOutcome.Fired(decode<KernelFrameResult>(raw))
        } catch (failure: KernelWireException) {
            KernelFrameOutcome.Failed(failure.message ?: "frame failed")
        }
    }

    /** Stops the native tick clock (app backgrounded); declared ticks and their phase are kept. */
    fun framePause() {
        OrbitalKernelBridge.nativeFramePause(handle)
    }

    /** Restarts it after [framePause]; the paused span is never counted, so no tick bursts. */
    fun frameResume() {
        OrbitalKernelBridge.nativeFrameResume(handle)
    }

    /** How many `(trait, tick)` pairs the schema declares; `0` means no frame loop is needed. */
    val tickCount: Int
        get() = OrbitalKernelBridge.nativeTickCount(handle)

    fun state(trait: String, entityId: String?): String? =
        OrbitalKernelBridge.nativeGetState(handle, trait, entityId)?.ifEmpty { null }

    fun snapshot(): KernelSnapshot {
        @Serializable
        data class Wire(val states: Map<String, String>, val theme: KernelResolvedTheme? = null, val canvasTheme: JsonElement? = null, val motion: JsonElement? = null)
        val wire = decode<Wire>(answer(OrbitalKernelBridge.nativeSnapshot(handle)))
        return KernelSnapshot(wire.states, wire.theme, wire.canvasTheme, wire.motion)
    }

    /**
     * The rows a row-set reference (the `ref` of a `$rowSet` marker) resolves to now for the current viewer, in the
     * reference's order (`orbital_kernel_resolve_rows`).
     */
    fun resolveRows(reference: JsonElement): JsonArray {
        @Serializable
        data class Wire(val rows: List<JsonElement>, val total: Long)
        val wire = decode<Wire>(answer(OrbitalKernelBridge.nativeResolveRows(handle, kernelJson.encodeToString(JsonElement.serializer(), reference))))
        return JsonArray(wire.rows)
    }

    /** [node] with every row-set marker replaced by the rows [resolveRows] answers; an unresolvable one throws its typed [KernelWireException] (never an empty list), so the caller fails that slot. */
    fun resolvingRowSets(node: JsonElement): JsonElement = when (node) {
        is JsonObject -> {
            val reference = node["\$rowSet"]?.let { it as? JsonPrimitive }?.booleanOrNull?.takeIf { it }?.let { node["ref"] }
            if (reference != null) resolveRows(reference)
            else JsonObject(node.mapValues { resolvingRowSets(it.value) })
        }
        is JsonArray -> JsonArray(node.map { resolvingRowSets(it) })
        else -> node
    }

    fun setThemeMode(mode: String) {
        OrbitalKernelBridge.nativeSetThemeMode(handle, mode)
    }

    fun setTraitOnScreen(traitName: String, onScreen: Boolean) {
        OrbitalKernelBridge.nativeSetTraitOnScreen(handle, traitName, onScreen)
    }

    fun seed(entityType: String, rowsJson: String) {
        OrbitalKernelBridge.nativeSeed(handle, entityType, rowsJson)
    }

    /**
     * Declares a new mount before its INITs fire (`orbital_kernel_remount`):
     * a routed delivery reaches none of [traits] before that trait's own
     * `INIT`. Pass exactly the traits about to be INITed.
     */
    fun remount(traits: Collection<String>) {
        OrbitalKernelBridge.nativeRemount(handle, kernelJson.encodeToString(traits.toList()))
    }

    /** Adds [traits] to the current mount (`orbital_kernel_mount`) without dropping the traits already mounted. */
    fun mount(traits: Collection<String>) {
        OrbitalKernelBridge.nativeMount(handle, kernelJson.encodeToString(traits.toList()))
    }
}
