package generated.patterns.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

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

    /** A server leg failed at transport level with no snapshot to restore: lowers the verify bridge's `pending`. Inert in release builds. */
    external fun nativeAbandonServerLeg(handle: Long, traitName: String, event: String?, error: String?)

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

    /** Bind `@user` for every subsequent dispatch on this handle. */
    external fun nativeSetCurrentUser(handle: Long, userJson: String)

    /** Seed `entityType` rows (a JSON array) into the handle's in-memory entity cache. */
    external fun nativeSeed(handle: Long, entityType: String, rowsJson: String)

    /** Declare a new mount (a JSON array of trait names) before its INITs fire. */
    external fun nativeRemount(handle: Long, traitsJson: String)
    external fun nativeMount(handle: Long, traitsJson: String)

    /** Strings the library returned that were not freed yet (the JNI forwarders free every result, so this reads 0). */
    external fun nativeOutstandingStrings(): Long

    /** The handle's last recorded error message. */
    external fun nativeLastError(handle: Long): String

    /**
     * Start the verify bridge (orbital-ffi `verify-bridge`) on 127.0.0.1:[port]; returns the
     * bound port, 0 on failure (read [nativeLastError]). Inert (0) unless the library was
     * built with `make android-lib VERIFY=1`.
     */
    external fun nativeVerifyBridgeStart(handle: Long, port: Int): Int

    external fun nativeVerifyBridgeStop(handle: Long)

    /** Register (or, with null, clear) the listener the bridge's `/send` calls on the bridge thread. */
    external fun nativeVerifyBridgeSetDispatchCallback(handle: Long, listener: OrbitalVerifyDispatchListener?): Boolean

    /** Publish the current route for the bridge's `/snapshot` and `/route`; null clears it. */
    external fun nativeVerifyBridgeSetRoute(handle: Long, route: String?): Boolean

    /** Mark the host mounting (false) or mounted (true: every page-mount `INIT` dispatched); `/idle` is never idle while mounting. */
    external fun nativeVerifyBridgeSetReady(handle: Long, ready: Boolean): Boolean

    /** The view state now holds render generation `generation`; `/idle` waits for its paint. */
    external fun nativeVerifyBridgeFolded(handle: Long, generation: Long): Boolean

    /** The page drew render generation `generation`. */
    external fun nativeVerifyBridgePainted(handle: Long, generation: Long): Boolean
}

/** Host hook for the verify bridge's `/send`; `traitScope`/`payloadJson` are null when absent. */
interface OrbitalVerifyDispatchListener {
    fun onVerifyDispatch(traitScope: String?, event: String, payloadJson: String?)
}

// MARK: - Wire models (mirror orbital-core's canonical EventResponse/EventRequest/
// TraitSnapshot/DispatchMode JSON exactly — see kernel_c_api.rs's doc comment for
// the envelope shape [OrbitalKernelBridge.nativeDispatch] returns). Named `Kernel*`
// to stay distinct from this project's generated `EventResponse`/`EmittedEvent`
// (package `generated`, the OLD per-shell wire §156 is converging away from).

@Serializable
data class KernelEventSource(
    val orbital: String? = null,
    val trait: String? = null
)

@Serializable
data class KernelEmittedEvent(
    val event: String,
    val payload: JsonElement? = null,
    val source: KernelEventSource? = null
)

/** Mirror of `orbital_core::EventResponse`. */
@Serializable
data class KernelEventResponse(
    val success: Boolean,
    val transitioned: Boolean,
    val states: Map<String, String>,
    val emittedEvents: List<KernelEmittedEvent>,
    val data: Map<String, List<JsonElement>>? = null,
    val entityByTrait: Map<String, JsonElement>? = null,
    val clientEffects: List<JsonElement>? = null,
    val clientEffectsByTrait: List<JsonElement>? = null,
    val effectResults: List<JsonElement>? = null,
    val guardFailed: String? = null,
    val error: String? = null
)

@Serializable
data class KernelTraitFrom(val trait: String, val from: String)

/** Mirror of `orbital_core::EventRequest` — the "server leg" a dispatch produces. */
@Serializable
data class KernelServerLeg(
    val event: String,
    val eventId: String? = null,
    val payload: JsonElement? = null,
    val entityId: String? = null,
    val targetTrait: String? = null,
    val user: JsonElement? = null,
    val clientId: String? = null,
    val tick: String? = null,
    val sourceTrait: String? = null,
    val traits: List<KernelTraitFrom>? = null,
    val entityByTrait: Map<String, JsonElement>? = null,
    val behavior: String? = null,
    val delivery: JsonElement? = null,
    val dispatchLog: JsonElement? = null
)

/** The `(trait, event)` that began a dispatch; a leg failure is reported against its event. */
@Serializable
data class KernelSeed(val trait: String, val event: String)

/** A server leg and the orbital that owns the trait it is addressed to (the route it posts to). */
@Serializable
data class KernelRoutedLeg(val orbital: String, val leg: KernelServerLeg)

/** Mirror of `orbital_core::runtime::TraitSnapshot`. */
@Serializable
data class KernelTraitSnapshot(val state: String, val entity: JsonElement)

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

/** Mirror of `orbital_core::runtime::DispatchMode`. */
@Serializable
enum class KernelDispatchMode {
    persistedAwaited,
    hybridClientOnly,
    runtimeOptimistic
}

/** One `(trait, event)` pair, decoded from the `alreadyDelivered`/`applied` wire arrays. */
data class KernelTraitEventPair(val trait: String, val event: String)

private fun decodePairs(raw: List<List<String>>): List<KernelTraitEventPair> =
    raw.filter { it.size == 2 }.map { KernelTraitEventPair(it[0], it[1]) }

@Serializable
private data class KernelDispatchWire(
    val orbital: String,
    val seed: KernelSeed,
    val response: KernelEventResponse,
    val serverLegs: List<KernelRoutedLeg> = emptyList(),
    val mode: KernelDispatchMode,
    val snapshot: KernelTraitSnapshot? = null,
    val alreadyDelivered: List<List<String>> = emptyList(),
    val awaiting: List<KernelAwaitingTrait>
)

data class KernelDispatchResult(
    /** The orbital that owns the dispatched trait; the server leg posts to `/api/orbitals/<orbital>/events`. */
    val orbital: String,
    val seed: KernelSeed,
    val response: KernelEventResponse,
    val serverLegs: List<KernelRoutedLeg>,
    val mode: KernelDispatchMode,
    val snapshot: KernelTraitSnapshot?,
    val alreadyDelivered: List<KernelTraitEventPair>,
    /** The traits awaiting this dispatch's server legs (orbital-core `AwaitingTrait`). */
    val awaiting: List<KernelAwaitingTrait>
)

/** A trait awaiting a server round trip, keyed by the transition its local run took. */
@Serializable
data class KernelAwaitingTrait(val trait: String, val event: String, val from: String)

@Serializable
data class KernelFiredTick(val trait: String, val tick: String)

/** One cascade a tick's `emit` started: the dispatch envelope plus the emitting trait. */
@Serializable
private data class KernelFrameDispatchWire(
    val trait: String,
    val orbital: String,
    val seed: KernelSeed,
    val response: KernelEventResponse,
    val serverLegs: List<KernelRoutedLeg> = emptyList(),
    val mode: KernelDispatchMode,
    val snapshot: KernelTraitSnapshot? = null,
    val alreadyDelivered: List<List<String>> = emptyList(),
    val awaiting: List<KernelAwaitingTrait>
)

@Serializable
private data class KernelFrameWire(
    val fired: List<KernelFiredTick> = emptyList(),
    val response: KernelEventResponse,
    val dispatches: List<KernelFrameDispatchWire> = emptyList(),
    val droppedServerEffects: Int = 0,
    val errors: List<String> = emptyList()
)

@Serializable
private data class KernelErrorWire(val error: String)

data class KernelFrameDispatch(val trait: String, val result: KernelDispatchResult)

/** [OrbitalKernel.frame]'s merged tick response plus one dispatch per tick `emit`. */
data class KernelFrameResult(
    val fired: List<KernelFiredTick>,
    val response: KernelEventResponse,
    val dispatches: List<KernelFrameDispatch>,
    /** Server effects a tick delegated: a tick has no server leg, so they are discarded. */
    val droppedServerEffects: Int,
    val errors: List<String>
)

/** What one [OrbitalKernel.frame] call produced. */
sealed class KernelFrameOutcome {
    /** No tick was due (or none passed its guard): nothing to fold. */
    object Idle : KernelFrameOutcome()
    data class Fired(val frame: KernelFrameResult) : KernelFrameOutcome()
    data class Failed(val message: String) : KernelFrameOutcome()
}

@Serializable
private data class KernelAppliedWire(val applied: List<List<String>> = emptyList(), val response: KernelEventResponse)

data class KernelAppliedResult(val applied: List<KernelTraitEventPair>, val response: KernelEventResponse)

/** [OrbitalKernel.snapshot] result: every trait's current state plus the
 * resolved `@currentTheme` (null only when the native call itself failed —
 * an app with no declared theme still gets the `minimalist-light` default). */
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
    }

    /** The native side's own last error, read right after a `null`/decode-failure return. */
    val lastError: String
        get() = OrbitalKernelBridge.nativeLastError(handle)

    fun destroy() {
        OrbitalKernelBridge.nativeDestroy(handle)
    }

    fun dispatch(trait: String, entityId: String?, event: String, payloadJson: String?): KernelDispatchResult? {
        val raw = OrbitalKernelBridge.nativeDispatch(handle, trait, entityId, event, payloadJson) ?: return null
        val wire = runCatching { kernelJson.decodeFromString(KernelDispatchWire.serializer(), raw) }.getOrNull()
            ?: return null
        return KernelDispatchResult(
            orbital = wire.orbital,
            seed = wire.seed,
            response = wire.response,
            serverLegs = wire.serverLegs,
            mode = wire.mode,
            snapshot = wire.snapshot,
            alreadyDelivered = decodePairs(wire.alreadyDelivered),
            awaiting = wire.awaiting
        )
    }

    fun applyServerResponse(responseJson: String, alreadyDelivered: List<KernelTraitEventPair>): KernelAppliedResult? {
        val alreadyDeliveredJson: String =
            kernelJson.encodeToString(alreadyDelivered.map { listOf(it.trait, it.event) })
        val raw = OrbitalKernelBridge.nativeApplyServerResponse(handle, responseJson, alreadyDeliveredJson)
            ?: return null
        val wire = runCatching { kernelJson.decodeFromString(KernelAppliedWire.serializer(), raw) }.getOrNull()
            ?: return null
        return KernelAppliedResult(decodePairs(wire.applied), wire.response)
    }

    fun restore(trait: String, entityId: String?, snapshot: KernelTraitSnapshot): KernelEventResponse? {
        val snapshotJson = kernelJson.encodeToString(KernelTraitSnapshot.serializer(), snapshot)
        val raw = OrbitalKernelBridge.nativeRestore(handle, trait, entityId, snapshotJson) ?: return null
        return runCatching { kernelJson.decodeFromString(KernelEventResponse.serializer(), raw) }.getOrNull()
    }

    fun abandonServerLeg(trait: String, event: String?, error: String?) {
        OrbitalKernelBridge.nativeAbandonServerLeg(handle, trait, event, error)
    }

    fun tick(trait: String, tickName: String, entityId: String?): KernelEventResponse? {
        val raw = OrbitalKernelBridge.nativeTick(handle, trait, tickName, entityId) ?: return null
        return runCatching { kernelJson.decodeFromString(KernelEventResponse.serializer(), raw) }.getOrNull()
    }

    /** Runs one host frame of the native tick clock (`nowMs` = the host's monotonic frame time). */
    fun frame(nowMs: Long): KernelFrameOutcome {
        val raw = OrbitalKernelBridge.nativeFrame(handle, nowMs) ?: return KernelFrameOutcome.Idle
        val wire = runCatching { kernelJson.decodeFromString(KernelFrameWire.serializer(), raw) }.getOrNull()
            ?: return KernelFrameOutcome.Failed(
                runCatching { kernelJson.decodeFromString(KernelErrorWire.serializer(), raw).error }.getOrNull()
                    ?: lastError.ifEmpty { "frame failed" }
            )
        return KernelFrameOutcome.Fired(
            KernelFrameResult(
                fired = wire.fired,
                response = wire.response,
                dispatches = wire.dispatches.map { d ->
                    KernelFrameDispatch(
                        trait = d.trait,
                        result = KernelDispatchResult(
                            orbital = d.orbital,
                            seed = d.seed,
                            response = d.response,
                            serverLegs = d.serverLegs,
                            mode = d.mode,
                            snapshot = d.snapshot,
                            alreadyDelivered = decodePairs(d.alreadyDelivered),
                            awaiting = d.awaiting
                        )
                    )
                },
                droppedServerEffects = wire.droppedServerEffects,
                errors = wire.errors
            )
        )
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
        val raw = OrbitalKernelBridge.nativeSnapshot(handle) ?: return KernelSnapshot(emptyMap(), null)
        @Serializable
        data class Wire(val states: Map<String, String>, val theme: KernelResolvedTheme? = null, val canvasTheme: JsonElement? = null, val motion: JsonElement? = null)
        val wire = runCatching { kernelJson.decodeFromString(Wire.serializer(), raw) }.getOrNull()
            ?: return KernelSnapshot(emptyMap(), null)
        return KernelSnapshot(wire.states, wire.theme, wire.canvasTheme, wire.motion)
    }

    /**
     * The rows a row-set reference (the `ref` of a `$rowSet` marker) resolves to now for the current viewer, in the
     * reference's order (`orbital_kernel_resolve_rows`); `null` when the kernel cannot resolve it (read [lastError]).
     */
    fun resolveRows(reference: JsonElement): JsonArray? {
        val raw = OrbitalKernelBridge.nativeResolveRows(handle, kernelJson.encodeToString(JsonElement.serializer(), reference)) ?: return null
        @Serializable
        data class Wire(val rows: List<JsonElement>, val total: Int = 0)
        val wire = runCatching { kernelJson.decodeFromString(Wire.serializer(), raw) }.getOrNull() ?: return null
        return JsonArray(wire.rows)
    }

    /** [node] with every row-set marker replaced by the rows [resolveRows] answers; an unresolvable one becomes an empty list. */
    fun resolvingRowSets(node: JsonElement): JsonElement = when (node) {
        is JsonObject -> {
            val reference = node["\$rowSet"]?.let { it as? JsonPrimitive }?.booleanOrNull?.takeIf { it }?.let { node["ref"] }
            if (reference != null) resolveRows(reference) ?: JsonArray(emptyList())
            else JsonObject(node.mapValues { resolvingRowSets(it.value) })
        }
        is JsonArray -> JsonArray(node.map { resolvingRowSets(it) })
        else -> node
    }

    fun setThemeMode(mode: String) {
        OrbitalKernelBridge.nativeSetThemeMode(handle, mode)
    }

    fun setCurrentUser(json: String) {
        OrbitalKernelBridge.nativeSetCurrentUser(handle, json)
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

    /** Starts the verify bridge; returns the bound port, 0 on failure (read [lastError]). Debuggable builds only. */
    fun startVerifyBridge(port: Int): Int = OrbitalKernelBridge.nativeVerifyBridgeStart(handle, port)

    fun stopVerifyBridge() {
        OrbitalKernelBridge.nativeVerifyBridgeStop(handle)
    }

    fun setVerifyDispatchListener(listener: OrbitalVerifyDispatchListener?): Boolean =
        OrbitalKernelBridge.nativeVerifyBridgeSetDispatchCallback(handle, listener)

    fun setVerifyRoute(route: String?): Boolean = OrbitalKernelBridge.nativeVerifyBridgeSetRoute(handle, route)

    fun setVerifyReady(ready: Boolean): Boolean = OrbitalKernelBridge.nativeVerifyBridgeSetReady(handle, ready)

    fun setVerifyFolded(generation: Long): Boolean = OrbitalKernelBridge.nativeVerifyBridgeFolded(handle, generation)

    fun setVerifyPainted(generation: Long): Boolean = OrbitalKernelBridge.nativeVerifyBridgePainted(handle, generation)
}
