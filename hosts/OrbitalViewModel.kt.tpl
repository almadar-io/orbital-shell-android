package generated

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import generated.patterns.core.AlmadarTheme
import generated.patterns.core.ColorMode
import generated.patterns.core.KernelDispatchMode
import generated.patterns.core.KernelDispatchResult
import generated.patterns.core.KernelEventResponse
import generated.patterns.core.KernelFrameOutcome
import generated.patterns.core.KernelFrameResult
import generated.patterns.core.KernelResolvedTheme
import generated.patterns.core.KernelServerLeg
import generated.patterns.core.KernelTraitEventPair
import generated.patterns.core.KernelTraitSnapshot
import generated.patterns.core.KeystoreSessionStore
import generated.patterns.core.OkHttpAuthTransport
import generated.patterns.core.OrbitalAuth
import generated.patterns.core.OrbitalAuthSession
import generated.patterns.core.OrbitalKernel
import generated.patterns.core.OrbitalSessionBinding
import generated.patterns.core.OrbitalVerifyDispatchListener
import generated.patterns.core.ThemeTokensJson
import generated.patterns.core.orbitalAuthConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/**
 * Set by `GeneratedActivity` from the launch Intent extras (`ORBITAL_VERIFY_BRIDGE=1`,
 * `ORBITAL_VERIFY_PORT`, `ORBITAL_API_BASE_URL`, `ORBITAL_INITIAL_ROUTE`, `ORBITAL_AUTH_EMULATOR_HOST`) BEFORE the view model exists, and only in a
 * debuggable build. `port == null` = the verify bridge was not requested; `apiBaseUrl == null` =
 * the compiled-in server URL stands.
 */
object VerifyBridgeLaunch {{
    @Volatile var port: Int? = null
    @Volatile var apiBaseUrl: String? = null
    @Volatile var initialRoute: String? = null
    @Volatile var authToken: String? = null
    @Volatile var authEmulatorHost: String? = null
}}

/**
 * ViewModel that drives the generated Orbital Android client — owns the
 * in-app kernel handle ([OrbitalKernel]) and mirrors
 * `orbital-client/src/client_kernel.rs::ClientKernel::dispatch`'s three-mode
 * circuit (wave B7). Every event PROCESSES locally first; a server leg (when
 * the dispatch mode produces one) is posted afterward and folded back.
 */
class OrbitalViewModel(application: Application) : AndroidViewModel(application) {{
    private val api: OrbitalApi = OrbitalApi(VerifyBridgeLaunch.apiBaseUrl ?: BASE_URL).also {{ client ->
        VerifyBridgeLaunch.authToken?.let {{ client.authToken = it }}
    }}
    private val kernelJson = Json {{ ignoreUnknownKeys = true }}

    /** Native sign-in (`OrbitalAuth`): the provider is build-declared; the session lives in the Keystore. */
    val auth = OrbitalAuth(
        orbitalAuthConfig(application, VerifyBridgeLaunch.authEmulatorHost),
        OkHttpAuthTransport(),
        KeystoreSessionStore(application),
        viewModelScope,
    )
    val session: StateFlow<OrbitalAuthSession?> = auth.session
    val apiBase: String get() = api.serverBase
    private val sessionBinding = OrbitalSessionBinding({{ api.authToken = it }}, {{ json -> kernel?.setCurrentUser(json) }})
    private var liveSubscription: LivePushSubscription? = null

    fun signOut() {{
        auth.signOut()
    }}

    private fun applyResolvedTheme(theme: KernelResolvedTheme) {{
        themeTokens = theme.tokens
        themeMode = if (theme.mode == "dark") ColorMode.Dark else ColorMode.Light
    }}

    /** The host's `toggleMode` (the web `useTheme()` context): asks the kernel for the opposite mode
     *  (`orbital_kernel_set_theme_mode`) and re-resolves the theme from its snapshot. */
    fun toggleThemeMode() {{
        val live = kernel ?: return
        live.setThemeMode(if (themeMode == ColorMode.Dark) "light" else "dark")
        val snap = live.snapshot()
        snap.theme?.let {{ applyResolvedTheme(it) }}
        canvasTheme = snap.canvasTheme
        motion = snap.motion
    }}

    /** A debuggable launch carrying the verify persona token is authenticated by it (the stored session is kept out in that case). */
    val hasLaunchToken: Boolean get() = VerifyBridgeLaunch.authToken != null

    private val _states = MutableStateFlow<Map<String, String>>(emptyMap())
    val states: StateFlow<Map<String, String>> = _states.asStateFlow()

    private val _uiSlots = MutableStateFlow<Map<String, JsonElement>>(emptyMap())
    val uiSlots: StateFlow<Map<String, JsonElement>> = _uiSlots.asStateFlow()

    // Per-trait Main-role render, keyed by the SOURCE trait that produced it —
    // resolves `@trait.X` composition (mirrors ClientCore's `trait_content`).
    private val _traitContent = MutableStateFlow<Map<String, JsonElement>>(emptyMap())
    val traitContent: StateFlow<Map<String, JsonElement>> = _traitContent.asStateFlow()

    // Traits with a live Main-role render, in first-write order (a re-render keeps
    // its position; a clear removes only that trait) — the page body stacks them.
    private val _mainOrder = MutableStateFlow<List<String>>(emptyList())
    val mainOrder: StateFlow<List<String>> = _mainOrder.asStateFlow()

    // Bumped on every fold; the page reports each generation once it is drawn.
    private val _paintGeneration = MutableStateFlow(0L)
    val paintGeneration: StateFlow<Long> = _paintGeneration.asStateFlow()

    // Predicted skeletons (`AwaitRenders`) of round trips that have outlasted the theme's busy
    // delay: by the trait whose content they stand in for, and by any other slot name.
    private val _awaitingMain = MutableStateFlow<Map<String, AwaitRender>>(emptyMap())
    private val _awaitingSlots = MutableStateFlow<Map<String, AwaitRender>>(emptyMap())
    private val awaitingLegs = mutableMapOf<Long, Int>()
    private val awaitingRaised = mutableMapOf<Long, List<AwaitRender>>()
    private var nextAwaitToken = 0L

    /** `traitContent`, with a `skeleton` node for an awaiting trait that has not rendered yet. */
    val contentWithAwaiting: StateFlow<Map<String, JsonElement>> = combine(_traitContent, _awaitingMain, _paintGeneration) {{ content, awaiting, _ ->
        val painted = paintedRowSets(content)
        painted + awaiting.filterKeys {{ it !in painted }}.mapValues {{ skeletonNode(it.value) }}
    }}.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /**
     * `content` with every row-set marker replaced by the rows the kernel's one resolver answers now, re-checked for
     * the signed-in viewer (EXEC-RENDER-3). The paint generation is a source of the flows below, so a rows-changed
     * repaint (which re-applies an equal render) re-resolves too.
     */
    private suspend fun paintedRowSets(content: Map<String, JsonElement>): Map<String, JsonElement> {{
        val k = kernel ?: return content
        return withContext(Dispatchers.Default) {{ content.mapValues {{ k.resolvingRowSets(it.value) }} }}
    }}

    /** `uiSlots`, with a `skeleton` node in an awaiting slot that is still empty. */
    val slotsWithAwaiting: StateFlow<Map<String, JsonElement>> = combine(_uiSlots, _awaitingSlots, _paintGeneration) {{ slots, awaiting, _ ->
        val painted = paintedRowSets(slots)
        painted + awaiting.filterKeys {{ it !in painted }}.mapValues {{ skeletonNode(it.value) }}
    }}.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** `mainOrder`, then awaiting traits that own no content yet and are not embedded. */
    val mainOrderWithAwaiting: StateFlow<List<String>> = combine(_mainOrder, _traitContent, _awaitingMain) {{ order, content, awaiting ->
        order + awaiting.keys.filter {{ it !in order && it !in content && it !in EMBEDDED_TRAITS }}.sorted()
    }}.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _lastMainWriter = MutableStateFlow<String?>(null)
    val lastMainWriter: StateFlow<String?> = _lastMainWriter.asStateFlow()

    private val _entityData = MutableStateFlow<Map<String, List<JsonElement>>>(emptyMap())
    val entityData: StateFlow<Map<String, List<JsonElement>>> = _entityData.asStateFlow()

    private val _fields = MutableStateFlow<Map<String, JsonObject>>(emptyMap())
    val fields: StateFlow<Map<String, JsonObject>> = _fields.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents = _navigationEvents
    private val _rootReplacements = MutableSharedFlow<String>()
    /** Routes that replace the NavHost's whole back stack (the verify launch's initial route), never push. */
    val rootReplacements = _rootReplacements

    private val _notifications = MutableSharedFlow<String>()
    val notifications = _notifications

    /** True once the kernel is loaded and its first snapshot has been
     *  folded — the generated `App.kt` gates the nav host behind this. */
    var isReady: Boolean by mutableStateOf(false)
        private set

    /** True when the schema declares at least one tick, read once at kernel mount:
     *  the app root runs a frame loop only then. */
    var hasTicks: Boolean by mutableStateOf(false)
        private set

    // Seeded from `ThemeTokensJson.fallback` until the kernel snapshot
    // (loaded off the main thread at construction) resolves `@currentTheme`.
    var themeTokens: ThemeTokensJson by mutableStateOf(ThemeTokensJson.fallback)
        private set
    var themeMode: ColorMode by mutableStateOf(ColorMode.Light)
        private set
    /** The kernel's `canvasTheme` (orbital-core `canvas_theme`): what the Rust-drawn canvases paint with. */
    var canvasTheme: JsonElement? by mutableStateOf(null)
    /** The kernel's resolved `motion` (orbital-core `resolve_motion`): what entries and surfaces animate. */
    var motion: JsonElement? by mutableStateOf(null)
        private set

    /** Per-launch id — the live-broadcast origin-exclusion key AND the
     * kernel's own client id (mirrors `ClientKernel::next_client_id`). */
    private val clientId = UUID.randomUUID().toString()

    private var kernel: OrbitalKernel? = null
    private val kernelReady = CompletableDeferred<Unit>()

    /** The client kernel's server-leg collector is shared per handle, so every call that can drive a cascade runs one at a time, in arrival order. */
    private val kernelCalls = Mutex()

    private val effectHandlers = EffectHandlers(_uiSlots, _traitContent, _mainOrder, _lastMainWriter, EMBEDDED_TRAITS, _navigationEvents, _notifications) {{
        trait, event, payload -> sendEvent(trait, event, payload)
    }}

    init {{
        viewModelScope.launch(Dispatchers.IO) {{
            val schemaJson = runCatching {{
                getApplication<Application>().assets.open(RESOLVED_SCHEMA_ASSET)
                    .bufferedReader().use {{ it.readText() }}
            }}.getOrElse {{ e ->
                android.util.Log.e("Orbital", "failed to read $RESOLVED_SCHEMA_ASSET", e)
                return@launch
            }}
            val created = OrbitalKernel.create(schemaJson, clientId)
            if (created == null) {{
                android.util.Log.e("Orbital", "kernel init failed from $RESOLVED_SCHEMA_ASSET")
                return@launch
            }}
            kernel = created
            VerifyBridgeLaunch.port?.let {{ startVerifyBridge(created, it) }}
            val snap = created.snapshot()
            withContext(Dispatchers.Main) {{
                if (snap.states.isNotEmpty()) {{
                    _states.value = _states.value + snap.states
                }}
                snap.theme?.let {{ applyResolvedTheme(it) }}
                canvasTheme = snap.canvasTheme
                motion = snap.motion
                hasTicks = created.tickCount > 0
                isReady = true
            }}
            adoptStoredSession()
            openLivePush()
            kernelReady.complete(Unit)
        }}
    }}

    /** The stored session (Keystore) is adopted before the first INIT so the first requests are signed.
     *  A debuggable launch carrying the verify persona token keeps it instead. */
    private fun adoptStoredSession() {{
        if (VerifyBridgeLaunch.authToken == null) {{
            auth.loadStoredSession()
            sessionBinding.apply(auth.session.value)
        }}
        auth.onChange = {{ next -> viewModelScope.launch(Dispatchers.Main) {{ applySession(next) }} }}
    }}

    private fun openLivePush() {{
        liveSubscription?.cancel()
        liveSubscription = api.openLivePush {{ item -> onLivePushItem(item) }}
    }}

    /** Binds the session's token and viewer. A different identity re-enters the app under it, as the
     *  web re-renders under a new `UserProvider` viewer: the live-push channel reconnects signed and
     *  every trait INITs again. A token refresh of the same identity changes neither. */
    private fun applySession(next: OrbitalAuthSession?) {{
        val identityChanged = sessionBinding.apply(next)
        if (!identityChanged || !isReady) return
        openLivePush()
        initAllTraits()
    }}

    /** Refreshes the ID token when the app returns to the foreground. */
    fun refreshSession() {{
        viewModelScope.launch {{ runCatching {{ auth.refreshIfNeeded() }} }}
    }}

    @Volatile private var currentRoute: String? = null

    /** Called on every navigation change; the verify bridge serves the latest route in `/snapshot` and `/route`. */
    fun onRouteChanged(route: String?) {{
        currentRoute = route
        if (VerifyBridgeLaunch.port != null) kernel?.setVerifyRoute(route)
    }}

    /**
     * Routes a renderer-dispatched [NAVIGATE_EVENT] envelope (`actionPayload.url`) into the same
     * `navigationEvents` sink the kernel `navigate` effect emits to.
     */
    fun navigate(envelope: JsonElement?) {{
        val url = ((envelope as? JsonObject)?.get("actionPayload") as? JsonObject)
            ?.get("url")?.jsonPrimitive?.contentOrNull
        if (url.isNullOrEmpty()) {{
            android.util.Log.e("Orbital", "NAVIGATE dropped: no actionPayload.url")
            return
        }}
        viewModelScope.launch {{ _navigationEvents.emit(url) }}
    }}

    /**
     * `ORBITAL_INITIAL_ROUTE` (a verify launch) replaces the back stack's root with that page, as a
     * web `page.goto` replaces the page: no pushed destination, no back affordance. Consumed once;
     * the start route needs no replacement.
     */
    fun openInitialRoute(startRoute: String) {{
        val route = VerifyBridgeLaunch.initialRoute?.takeIf {{ it.isNotEmpty() }} ?: return
        VerifyBridgeLaunch.initialRoute = null
        if (route != startRoute) viewModelScope.launch {{ _rootReplacements.emit(route) }}
    }}

    /** Set by `GeneratedPage`: the trait a verify-bridge `/send` without a `traitScope` routes to, as a tap on the visible page would. */
    @Volatile var activePageTrait: String? = null

    private fun startVerifyBridge(k: OrbitalKernel, port: Int) {{
        val bound = k.startVerifyBridge(port)
        if (bound == 0) {{
            android.util.Log.e("Orbital", "verify bridge failed to start on $port: ${{k.lastError}}")
            return
        }}
        android.util.Log.i("Orbital", "verify bridge listening on 127.0.0.1:$bound")
        currentRoute?.let {{ k.setVerifyRoute(it) }}
        k.setVerifyDispatchListener(object : OrbitalVerifyDispatchListener {{
            override fun onVerifyDispatch(traitScope: String?, event: String, payloadJson: String?) {{
                viewModelScope.launch {{ handleVerifyDispatch(traitScope, event, payloadJson) }}
            }}
        }})
    }}

    /** A bridge `/send` takes the SAME entry point a renderer's `onEvent` ends in. */
    private fun handleVerifyDispatch(traitScope: String?, event: String, payloadJson: String?) {{
        val trait = traitScope ?: activePageTrait
        if (trait == null) {{
            android.util.Log.e("Orbital", "verify send $event dropped: no traitScope and no active page")
            return
        }}
        val payload = payloadJson?.let {{
            runCatching {{ kernelJson.parseToJsonElement(it) }}.getOrElse {{
                android.util.Log.e("Orbital", "verify send $event dropped: payload is not valid json")
                return
            }}
        }}
        sendEvent(trait, event, payload)
    }}

    /** Ignore a broadcast this SAME client originated (already realized
     * locally by the dispatch that produced it) — feed every other item's
     * event through the kernel exactly like a local dispatch, then fold. */
    private fun onLivePushItem(item: LiveBroadcastItem) {{
        if (item.originClientId == clientId) return
        val trait = item.source?.trait ?: return
        viewModelScope.launch {{
            val k = kernel ?: return@launch
            val payloadJson = item.payload?.let {{ kernelJson.encodeToString(JsonElement.serializer(), it) }}
            val result = kernelCalls.withLock {{ withContext(Dispatchers.Default) {{ k.dispatch(trait, null, item.event, payloadJson) }} }}
            if (result == null) {{
                android.util.Log.e("Orbital", "live-push dispatch failed: trait=$trait event=${{item.event}} error=${{k.lastError}}")
                return@launch
            }}
            android.util.Log.i(
                "Orbital",
                "live-push dispatch trait=$trait event=${{item.event}} mode=${{result.mode}}"
            )
            foldResponse(trait, result.response)
        }}
    }}

    /**
     * Seeds `states[trait]` from the declared initial state if not already
     * set (before the kernel snapshot lands). No dispatch: `INIT` fires
     * exactly once per trait, at app mount, via `initAllTraits`.
     */
    fun initTrait(trait: String) {{
        if (_states.value[trait] == null) {{
            _states.value = _states.value + (trait to (INITIAL_STATES[trait] ?: "idle"))
        }}
    }}

    /**
     * Fire `INIT` once for every trait that declares it, in declaration
     * order, so every `@trait.X` peer's content is indexed into
     * `traitContent` before any page reads it — mirrors
     * `ClientCore::init_all_traits_blocking`. Called once at app mount;
     * awaits the kernel so a fast-mounting Compose tree never races it.
     */
    fun initAllTraits() {{
        viewModelScope.launch {{
            kernelReady.await()
            kernel?.setVerifyReady(false)
            kernel?.remount(TRAITS_WITH_INIT)
            TRAITS_WITH_INIT.forEach {{ trait ->
                initTrait(trait)
                sendEvent(trait, "INIT")
            }}
            kernel?.setVerifyReady(true)
        }}
    }}

    /** Viewer each gated trait was last INITed for: a new viewer re-INITs it (the web keys the page by viewer). */
    private val gatedInitViewer = mutableMapOf<String, String>()

    /**
     * Mount a gated page's circuit for an authenticated [viewer] — the twin of the web
     * `PageAccessHost` mounting the page only once its viewer is authenticated. Its gated-only
     * traits join the mount and INIT once per viewer.
     */
    fun mountGatedPage(path: String, viewer: String) {{
        viewModelScope.launch {{
            kernelReady.await()
            val pending = GATED_PAGE_TRAITS[path].orEmpty().filter {{ gatedInitViewer[it] != viewer }}
            if (pending.isEmpty()) return@launch
            kernel?.setVerifyReady(false)
            kernel?.mount(pending)
            pending.forEach {{ trait ->
                gatedInitViewer[trait] = viewer
                initTrait(trait)
                sendEvent(trait, "INIT")
            }}
            kernel?.setVerifyReady(true)
        }}
    }}

    /**
     * Dispatch one event through the in-app kernel and fold the result —
     * `ClientKernel::dispatch`'s three-mode circuit (`DispatchMode`):
     * `hybridClientOnly` posts only the leg its cascade produced; `runtimeOptimistic` posts the server
     * leg in the background and restores the pre-dispatch snapshot on
     * failure; `persistedAwaited` posts and awaits before this call
     * completes. `emittedEvents` on the response is NOT re-dispatched — the
     * kernel already fanned those out internally.
     */
    fun sendEvent(
        trait: String,
        event: String,
        payload: JsonElement? = null
    ) {{
        viewModelScope.launch {{
            val k = kernel
            if (k == null) {{
                android.util.Log.e("Orbital", "sendEvent before kernel ready: trait=$trait event=$event")
                return@launch
            }}
            val payloadJson = payload?.let {{ kernelJson.encodeToString(JsonElement.serializer(), it) }}
            val result = kernelCalls.withLock {{ withContext(Dispatchers.Default) {{ k.dispatch(trait, null, event, payloadJson) }} }}
            if (result == null) {{
                val err = k.lastError
                android.util.Log.e("Orbital", "dispatch failed: trait=$trait event=$event error=$err")
                _notifications.emit("Event failed ($trait/$event): $err")
                return@launch
            }}
            android.util.Log.i("Orbital", "dispatch trait=$trait event=$event mode=${{result.mode}} serverLegs=${{result.serverLegs.size}}")
            foldDispatch(trait, result)
        }}
    }}

    /**
     * The one fold of a kernel dispatch envelope: the local response applies
     * immediately, then the resolved `DispatchMode` decides the server leg's
     * fate. A tapped event and a tick's `emit` cascade both land here.
     */
    private suspend fun foldDispatch(trait: String, result: KernelDispatchResult) {{
        foldResponse(trait, result.response)
        val awaitToken = beginAwaiting(result.awaiting.flatMap {{ AwaitRenders.matching(it) }}, result.serverLegs.size)

        when (result.mode) {{
            KernelDispatchMode.hybridClientOnly -> {{
                // A `local` seed only carries a leg when its cascade reached a
                // server-bound trait; the kernel counted it, so it is posted
                // and applied/restored/abandoned like any other leg.
                for (routed in result.serverLegs) {{
                    android.util.Log.i("Orbital", "posting cascade server leg for local seed trait=$trait event=${{routed.leg.event}}")
                    try {{
                        postServerLegAndFold(trait, routed.orbital, routed.leg, result.alreadyDelivered, result.snapshot, result.seed.event)
                    }} finally {{
                        endAwaitingLeg(awaitToken)
                    }}
                }}
            }}
            KernelDispatchMode.runtimeOptimistic -> {{
                for (routed in result.serverLegs) {{
                    // Posted in the background — this dispatch is already
                    // committed locally, the leg only reconciles/persists.
                    viewModelScope.launch {{
                        try {{
                            postServerLegAndFold(trait, routed.orbital, routed.leg, result.alreadyDelivered, result.snapshot, result.seed.event)
                        }} finally {{
                            endAwaitingLeg(awaitToken)
                        }}
                    }}
                }}
            }}
            KernelDispatchMode.persistedAwaited -> {{
                for (routed in result.serverLegs) {{
                    try {{
                        postServerLegAndFold(trait, routed.orbital, routed.leg, result.alreadyDelivered, result.snapshot, result.seed.event)
                    }} finally {{
                        endAwaitingLeg(awaitToken)
                    }}
                }}
            }}
        }}
    }}

    private var loggedDroppedServerEffects = false

    /** Raise `predicted` once the dispatch's legs outlast the theme's busy delay (never when the
     *  theme turns the awaiting skeleton off); null when nothing is predicted or nothing is posted. */
    private fun beginAwaiting(predicted: List<AwaitRender>, legs: Int): Long? {{
        val motion = AlmadarTheme.resolve(themeTokens, themeMode, AlmadarTheme.fallback).motion
        if (!motion.awaitingSkeleton || legs == 0 || predicted.isEmpty()) return null
        val token = nextAwaitToken++
        awaitingLegs[token] = legs
        viewModelScope.launch {{
            delay(motion.busyDelayMs.toLong())
            if (token !in awaitingLegs) return@launch
            awaitingRaised[token] = predicted
            predicted.forEach {{ render ->
                if (framesTrait(render)) _awaitingMain.value = _awaitingMain.value + (render.trait to render)
                else _awaitingSlots.value = _awaitingSlots.value + (render.slot to render)
            }}
        }}
        return token
    }}

    /** One leg of the dispatch settled; the last one clears the skeletons it raised. */
    private fun endAwaitingLeg(token: Long?) {{
        if (token == null) return
        val left = awaitingLegs[token] ?: return
        if (left > 1) {{
            awaitingLegs[token] = left - 1
            return
        }}
        awaitingLegs.remove(token)
        awaitingRaised.remove(token)?.forEach {{ render ->
            if (framesTrait(render)) _awaitingMain.value = _awaitingMain.value - render.trait
            else _awaitingSlots.value = _awaitingSlots.value - render.slot
        }}
    }}

    /** A `main` render, or any render of an embedded trait (its host's frame), stands in as the
     *  trait's content; any other slot's stands in that slot. */
    private fun framesTrait(render: AwaitRender): Boolean = render.slot == "main" || render.trait in EMBEDDED_TRAITS

    private fun skeletonNode(render: AwaitRender): JsonElement =
        JsonObject(mapOf("type" to JsonPrimitive("skeleton"), "spec" to render.skeleton))

    /** Stops the tick clock (app left the foreground); ticks and their phase are kept. */
    fun framePause() {{
        kernel?.framePause()
    }}

    /** Restarts it; the paused span is never counted, so no tick bursts. */
    fun frameResume() {{
        kernel?.frameResume()
    }}

    /**
     * One composition frame: the kernel runs every due tick and hands back ONE envelope, folded
     * through the same paths a dispatch uses. `nowMs` is the frame clock's monotonic time.
     */
    suspend fun runFrame(nowMs: Long) {{
        val k = kernel ?: return
        when (val outcome = kernelCalls.withLock {{ withContext(Dispatchers.Default) {{ k.frame(nowMs) }} }}) {{
            KernelFrameOutcome.Idle -> Unit
            is KernelFrameOutcome.Failed -> android.util.Log.e("Orbital", "frame failed: ${{outcome.message}}")
            is KernelFrameOutcome.Fired -> foldFrame(outcome.frame)
        }}
    }}

    /** The tick response folds like a dispatch response; each tick `emit` cascade folds (and posts its server leg) like a tapped event on the emitting trait. */
    private suspend fun foldFrame(frame: KernelFrameResult) {{
        foldResponse(frame.fired.firstOrNull()?.trait ?: "", frame.response)
        frame.dispatches.forEach {{ foldDispatch(it.trait, it.result) }}
        frame.errors.forEach {{ android.util.Log.e("Orbital", "tick failed: $it") }}
        if (frame.droppedServerEffects > 0 && !loggedDroppedServerEffects) {{
            loggedDroppedServerEffects = true
            android.util.Log.e("Orbital", "a tick delegated ${{frame.droppedServerEffects}} server effect(s); ticks have no server leg, so they were discarded")
        }}
    }}

    /**
     * Fold a [KernelEventResponse] into view-model
     * state: merge `states`, land `data`/`entityByTrait`, and route every
     * `clientEffects` entry through [effectHandlers] attributed by the
     * paired `clientEffectsByTrait[i].traitName` (falling back to
     * `requestTrait`). A `success = false` response surfaces
     * `error`/`guardFailed` as a notification — read for the message only,
     * never as the success/failure switch (`success` already is that).
     */
    private suspend fun foldResponse(requestTrait: String, response: KernelEventResponse) {{
        if (response.states.isNotEmpty()) {{
            _states.value = _states.value + response.states
        }}
        response.data?.let {{ data -> _entityData.value = _entityData.value + data }}
        response.entityByTrait?.forEach {{ (trait, entity) ->
            val obj = entity as? JsonObject ?: return@forEach
            val existing = _fields.value[trait] ?: JsonObject(emptyMap())
            _fields.value = _fields.value + (trait to JsonObject(existing + obj))
        }}
        val effects = response.clientEffects.orEmpty()
        val byTrait = response.clientEffectsByTrait.orEmpty()
        effects.forEachIndexed {{ i, effect ->
            val paired = (byTrait.getOrNull(i) as? JsonObject)?.get("traitName")?.jsonPrimitive?.contentOrNull
            effectHandlers.apply(effect, paired ?: requestTrait)
        }}
        if (!response.success) {{
            _notifications.emit(response.error ?: response.guardFailed ?: "Event failed")
        }}
        _paintGeneration.value += 1
        kernel?.setVerifyFolded(_paintGeneration.value)
    }}

    /** The page drew render generation `generation` (verify bridge: idle only once the screen matches the kernel). */
    fun reportPainted(generation: Long) {{
        kernel?.setVerifyPainted(generation)
    }}

    /**
     * Post one server leg and fold the outcome — success applies it to the
     * kernel (`kernel.applyServerResponse`, landing every server-assigned
     * trait's new state); a `success:false`/undecodable/transport failure
     * restores the pre-dispatch snapshot (when one exists — `persistedAwaited`
     * never produces one, nothing to restore there) and surfaces the error.
     */
    private suspend fun postServerLegAndFold(
        requestTrait: String,
        orbital: String,
        leg: KernelServerLeg,
        alreadyDelivered: List<KernelTraitEventPair>,
        snapshot: KernelTraitSnapshot?,
        seedEvent: String
    ) {{
        val k = kernel ?: return
        suspend fun restore(error: String) {{
            if (snapshot != null) {{
                val repaint = kernelCalls.withLock {{ k.restore(requestTrait, null, snapshot) }}
                _states.value = _states.value + (requestTrait to snapshot.state)
                if (repaint != null) foldResponse(requestTrait, repaint)
            }} else {{
                kernelCalls.withLock {{ k.abandonServerLeg(leg.targetTrait ?: requestTrait, seedEvent, error) }}
            }}
        }}
        api.postServerLeg(orbital, leg)
            .onSuccess {{ raw ->
                val decoded = runCatching {{ kernelJson.decodeFromString(KernelEventResponse.serializer(), raw) }}
                    .getOrNull()
                if (decoded != null && decoded.success) {{
                    // `KernelAppliedResult.applied` reuses the (trait, event) pair
                    // shape for (trait, newState) — see kernel_c_api.rs's `AppliedWire`.
                    val applied = kernelCalls.withLock {{ k.applyServerResponse(raw, alreadyDelivered) }}
                    if (applied == null) {{
                        val reason = k.lastError
                        restore(reason)
                        android.util.Log.e("Orbital", "applyServerResponse failed: trait=$requestTrait event=${{leg.event}} error=$reason")
                        _notifications.emit("Event failed ($requestTrait/${{leg.event}}): $reason")
                    }} else {{
                        applied.applied.forEach {{ pair ->
                            _states.value = _states.value + (pair.trait to pair.event)
                        }}
                        foldResponse(requestTrait, applied.response)
                    }}
                }} else {{
                    val message = decoded?.error ?: decoded?.guardFailed ?: "malformed server response"
                    restore(message)
                    android.util.Log.e("Orbital", "server leg rejected: trait=$requestTrait event=${{leg.event}} error=$message")
                    _notifications.emit("Event failed ($requestTrait/${{leg.event}}): $message")
                }}
            }}
            .onFailure {{ throwable ->
                restore(throwable.message ?: throwable.toString())
                android.util.Log.e("Orbital", "postServerLeg failed: trait=$requestTrait event=${{leg.event}}", throwable)
                _notifications.emit("Event failed ($requestTrait/${{leg.event}}): ${{throwable.message ?: throwable.toString()}}")
            }}
    }}

    companion object {{
        /** The event a renderer emits for a `navigatesTo` action or a nav item; `SlotContent` routes it to [navigate] before kernel dispatch. */
        const val NAVIGATE_EVENT = "NAVIGATE"
        /** The system event `UI:SIGN_OUT`: the host ends the session, the kernel never sees it. */
        const val SIGN_OUT_EVENT = "SIGN_OUT"
        /** The host's own sign-in route (`HOST_SIGN_IN_ROUTE`): no page declares it, the NavHost renders `OrbitalSignInScreen`. */
        const val HOST_SIGN_IN_ROUTE = "{host_sign_in_route}"
        private const val RESOLVED_SCHEMA_ASSET = "resolved_schema.json"
        private const val BASE_URL = "{base_url}"
        private val INITIAL_STATES: Map<String, String> = {initial_states}
        private val TRAITS_WITH_INIT: Set<String> = {traits_with_init}
        private val GATED_PAGE_TRAITS: Map<String, List<String>> = {gated_page_traits}
        /** Traits embedded via `@trait.X` by a sibling: indexed in `traitContent` for the splice, never a `main` owner. */
        private val EMBEDDED_TRAITS: Set<String> = {embedded_traits}
    }}
}}

/**
 * The trait name in a `@trait.X`[.slot] leaf — the prefix up to the next
 * `.`, mirroring `orbital_core::ir::OirBindingRoot::Trait`
 * (`orbital-client`'s `recurse.rs::trait_ref_name`).
 */
fun traitRefName(s: String): String? {{
    if (!s.startsWith("@trait.")) return null
    val rest = s.removePrefix("@trait.")
    val dot = rest.indexOf('.')
    val name = if (dot >= 0) rest.substring(0, dot) else rest
    return name.ifEmpty {{ null }}
}}

/**
 * Resolve every `@trait.X` leaf in `node` against `traitContent` — the
 * mirror of `orbital-client`'s `recurse.rs::child_patterns` +
 * `helpers.rs::render_children`; Compose's generated renderers recurse
 * blindly through `children` (an unspliced ref would render as literal
 * text), so this must run before content reaches `CorePatternRegistry`. A
 * `children`-array ref that fails to resolve is dropped; any other prop's
 * ref becomes null. Every "type" node is stamped with the nearest
 * ancestor's source trait (its own if already present); a trait already on
 * the splice stack is dropped (cycle guard).
 */
fun spliceContent(
    node: JsonElement,
    traitContent: Map<String, JsonElement>,
    ambientSource: String,
    seen: Set<String> = emptySet()
): JsonElement = when (node) {{
    is JsonObject -> {{
        val hasType = node.containsKey("type")
        val own = if (hasType) {{
            (node["sourceTrait"] as? JsonPrimitive)?.contentOrNull ?: ambientSource
        }} else {{
            ambientSource
        }}
        val out = LinkedHashMap<String, JsonElement>()
        for ((key, value) in node) {{
            if (key == "sourceTrait") continue
            out[key] = if (key == "children") {{
                spliceChildren(value, traitContent, own, seen)
            }} else {{
                spliceProp(value, traitContent, own, seen)
            }}
        }}
        if (hasType) out["sourceTrait"] = JsonPrimitive(own)
        JsonObject(out)
    }}
    is JsonArray -> JsonArray(node.map {{ spliceProp(it, traitContent, ambientSource, seen) }})
    else -> node
}}

private fun spliceChildren(
    value: JsonElement,
    traitContent: Map<String, JsonElement>,
    ambientSource: String,
    seen: Set<String>
): JsonElement {{
    val arr = value as? JsonArray
        ?: return spliceProp(value, traitContent, ambientSource, seen).takeIf {{ it !is JsonNull }} ?: JsonArray(emptyList())
    val out = mutableListOf<JsonElement>()
    for (child in arr) {{
        if (child is JsonNull) continue
        val ref = (child as? JsonPrimitive)?.takeIf {{ it.isString }}?.let {{ traitRefName(it.content) }}
        if (ref != null) {{
            if (ref in seen) continue
            val refContent = traitContent[ref] ?: continue
            out.add(spliceContent(refContent, traitContent, ref, seen + ref))
        }} else {{
            out.add(spliceContent(child, traitContent, ambientSource, seen))
        }}
    }}
    return JsonArray(out)
}}

private fun spliceProp(
    value: JsonElement,
    traitContent: Map<String, JsonElement>,
    ambientSource: String,
    seen: Set<String>
): JsonElement {{
    if (value is JsonPrimitive && value.isString) {{
        val ref = traitRefName(value.content)
        if (ref != null) {{
            if (ref in seen) return JsonNull
            val refContent = traitContent[ref] ?: return JsonNull
            return spliceContent(refContent, traitContent, ref, seen + ref)
        }}
        return value
    }}
    return spliceContent(value, traitContent, ambientSource, seen)
}}
