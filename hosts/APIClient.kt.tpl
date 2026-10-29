package generated

import generated.patterns.core.KernelEventSource
import generated.patterns.core.KernelServerLeg
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

/**
 * `GET /api/events` push item — mirrors `orbital_core::LiveBroadcastItem`
 * byte-for-byte. `originClientId` is the dispatching client's own id; a
 * subscriber must ignore items whose `originClientId` equals its own
 * (it already realized that dispatch locally).
 */
@Serializable
data class LiveBroadcastItem(
    val event: String,
    val payload: JsonElement? = null,
    val source: KernelEventSource? = null,
    val originClientId: String? = null
)

/**
 * The ONLY transport the generated client speaks: post one server leg (the
 * request `kernel.dispatch` produced) to `/api/orbitals/{{orbital}}/events`,
 * and subscribe to the persistent `/api/events` live-push channel. Every
 * event actually PROCESSES through the in-app kernel (`OrbitalKernel`,
 * `OrbitalKernelBridge.kt`) — this class never decides state, it only moves
 * bytes.
 */
class OrbitalApi(baseUrl: String) {{
    private val baseUrl = baseUrl.trimEnd('/')

    /** Authorization hook: the signed-in session's ID token (`OrbitalAuth`), or the debuggable
     *  build's persona. Read on every request and every live-push connect; unset = anonymous. */
    @Volatile var authToken: String? = null

    /** The server the host sign-in lists dev personas from and exchanges their custom token with. */
    val serverBase: String get() = baseUrl

    private val apiJson = Json {{ ignoreUnknownKeys = true; isLenient = true }}

    private val client = HttpClient(OkHttp) {{
        install(ContentNegotiation) {{
            json(apiJson)
        }}
    }}

    /** Raw OkHttp client for the SSE subscription (`okhttp-sse` needs the
     *  underlying `OkHttpClient`, not ktor's wrapper) — `readTimeout = 0`
     *  keeps the long-lived stream connection open. */
    private val sseClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    /**
     * POST the server leg a `kernel.dispatch`/`kernel.applyServerResponse`
     * round trip produced to the canonical route. Returns the raw response
     * body (undecoded) so the caller can both feed it straight into
     * `kernel.applyServerResponse` and inspect `success`/`error` without a
     * decode/re-encode round trip. `orbital` is the owning orbital the kernel
     * reported for the dispatched trait (`KernelDispatchResult.orbital`).
     */
    suspend fun postServerLeg(orbital: String, request: KernelServerLeg): Result<String> {{
        return try {{
            val response = client.post {{
                url("$baseUrl/api/orbitals/$orbital/events")
                contentType(ContentType.Application.Json)
                authToken?.let {{ header("Authorization", "Bearer $it") }}
                setBody(apiJson.encodeToString(KernelServerLeg.serializer(), request))
            }}
            Result.success(response.body())
        }} catch (e: Throwable) {{
            Result.failure(e)
        }}
    }}

    /**
     * Subscribe to `GET /api/events`, the persistent live-broadcast channel
     * every OTHER connected client's dispatch is fanned out on. Reconnects
     * with exponential backoff (capped at 30s) on any close/failure; a
     * single unparseable `data:` line is logged and skipped, never fatal.
     */
    fun openLivePush(onItem: (LiveBroadcastItem) -> Unit): LivePushSubscription {{
        var backoffMs = 1000L
        val subscription = LivePushSubscription()
        lateinit var connect: () -> Unit
        connect = connect@{{
            if (subscription.cancelled) return@connect
            val requestBuilder = Request.Builder().url("$baseUrl/api/events")
            authToken?.let {{ requestBuilder.header("Authorization", "Bearer $it") }}
            val listener = object : EventSourceListener() {{
                override fun onOpen(eventSource: EventSource, response: Response) {{
                    backoffMs = 1000L
                }}
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {{
                    val item = runCatching {{ apiJson.decodeFromString(LiveBroadcastItem.serializer(), data) }}
                        .getOrElse {{ e ->
                            android.util.Log.w("Orbital", "live push: unparseable SSE data: $data", e)
                            return
                        }}
                    onItem(item)
                }}
                override fun onClosed(eventSource: EventSource) {{
                    reconnect()
                }}
                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {{
                    android.util.Log.e("Orbital", "live push connection failed", t)
                    reconnect()
                }}
                private fun reconnect() {{
                    val delay = backoffMs
                    backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
                    Thread {{
                        Thread.sleep(delay)
                        connect()
                    }}.start()
                }}
            }}
            subscription.current = EventSources.createFactory(sseClient).newEventSource(requestBuilder.build(), listener)
        }}
        connect()
        return subscription
    }}
}}

/** A running `GET /api/events` subscription; [cancel] closes the stream and stops the reconnect loop. */
class LivePushSubscription {{
    @Volatile var cancelled = false
        private set
    @Volatile internal var current: EventSource? = null

    fun cancel() {{
        cancelled = true
        current?.cancel()
    }}
}}
