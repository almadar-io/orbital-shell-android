package generated.patterns.core

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Where the app's identity provider lives. Declared by the build (`BuildConfig`-style manifest
 * meta-data fed from gradle properties); nothing is inferred.
 */
data class OrbitalAuthConfig(val apiKey: String, val projectId: String, val emulatorHost: String?) {
    val identityToolkitBase: String
        get() = emulatorHost?.let { "http://$it/identitytoolkit.googleapis.com/v1" } ?: "https://identitytoolkit.googleapis.com/v1"
    val secureTokenBase: String
        get() = emulatorHost?.let { "http://$it/securetoken.googleapis.com/v1" } ?: "https://securetoken.googleapis.com/v1"

    companion object {
        /** `null` when the build declares no provider. [emulatorOverride] is the debuggable-build launch override and wins. */
        fun declared(apiKey: String?, projectId: String?, declaredHost: String?, emulatorOverride: String?): OrbitalAuthConfig? {
            if (apiKey.isNullOrEmpty() || projectId.isNullOrEmpty()) return null
            val host = listOf(emulatorOverride, declaredHost).firstOrNull { !it.isNullOrEmpty() }
            return OrbitalAuthConfig(apiKey, projectId, host)
        }
    }
}

@Serializable
data class OrbitalAuthSession(val idToken: String, val refreshToken: String, val expiresAtMs: Long, val uid: String)

data class OrbitalPersona(val id: String, val label: String)

sealed class OrbitalAuthException(message: String) : Exception(message) {
    object NotConfigured : OrbitalAuthException("Sign-in is not configured for this build")
    class Provider(message: String) : OrbitalAuthException(message)
    class Malformed(what: String) : OrbitalAuthException("Unexpected sign-in response: $what")
}

class AuthReply(val status: Int, val body: String)

interface AuthTransport {
    suspend fun send(method: String, url: String, body: String?): AuthReply
}

interface AuthSessionStore {
    fun read(): String?
    fun write(json: String)
    fun delete()
}

/**
 * Native sign-in against the provider's REST API (Firebase Identity Toolkit and Secure Token), the
 * platform twin of `@almadar/auth/browser`. It performs the provider call, holds the session and
 * reports each change; it never routes, hides or gates anything from auth state.
 */
class OrbitalAuth(
    private val config: OrbitalAuthConfig?,
    private val transport: AuthTransport,
    private val store: AuthSessionStore,
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val sleepMs: suspend (Long) -> Unit = { delay(it) },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()
    private var refreshJob: Job? = null
    private val _session = MutableStateFlow<OrbitalAuthSession?>(null)
    val session: StateFlow<OrbitalAuthSession?> = _session.asStateFlow()

    /** Fired after every change of the session: sign-in, refresh, restore, sign-out (`null`). */
    @Volatile var onChange: ((OrbitalAuthSession?) -> Unit)? = null

    val isConfigured: Boolean get() = config != null

    fun loadStoredSession() {
        val stored = store.read()?.let { runCatching { json.decodeFromString(OrbitalAuthSession.serializer(), it) }.getOrNull() } ?: return
        adopt(stored, persist = false)
    }

    suspend fun signInWithPassword(email: String, password: String) {
        val cfg = requireConfig()
        val body = buildJsonObject { put("email", email); put("password", password); put("returnSecureToken", true) }
        val reply = post("${cfg.identityToolkitBase}/accounts:signInWithPassword?key=${cfg.apiKey}", body)
        adopt(sessionFromSignIn(reply), persist = true)
    }

    suspend fun signInWithCustomToken(customToken: String) {
        val cfg = requireConfig()
        val body = buildJsonObject { put("token", customToken); put("returnSecureToken", true) }
        val reply = post("${cfg.identityToolkitBase}/accounts:signInWithCustomToken?key=${cfg.apiKey}", body)
        adopt(sessionFromSignIn(reply), persist = true)
    }

    /** The dev persona roster a server's `personasRouter` offers (only while it runs on the Auth emulator). */
    suspend fun personas(apiBase: String): List<OrbitalPersona> {
        val reply = runCatching { transport.send("GET", "$apiBase/api/personas", null) }.getOrNull() ?: return emptyList()
        if (reply.status !in 200..299) return emptyList()
        val rows = runCatching { json.parseToJsonElement(reply.body).jsonObject["personas"] as? JsonArray }.getOrNull() ?: return emptyList()
        return rows.mapNotNull { row ->
            val obj = row as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: id
            val role = obj["role"]?.jsonPrimitive?.contentOrNull ?: "no role"
            OrbitalPersona(id, "$name - $role")
        }
    }

    suspend fun signInAsPersona(id: String, apiBase: String) {
        val reply = transport.send("POST", "$apiBase/api/personas/sign-in", buildJsonObject { put("id", id) }.toString())
        val parsed = runCatching { json.parseToJsonElement(reply.body).jsonObject }.getOrNull()
            ?: throw OrbitalAuthException.Malformed("persona sign-in body (${reply.status})")
        val customToken = parsed["customToken"]?.jsonPrimitive?.contentOrNull
        if (reply.status !in 200..299 || customToken == null) {
            throw OrbitalAuthException.Provider(parsed["error"]?.jsonPrimitive?.contentOrNull ?: "persona sign-in failed (${reply.status})")
        }
        signInWithCustomToken(customToken)
    }

    /** Refreshes the ID token when [marginMs] or less remains. */
    suspend fun refreshIfNeeded(marginMs: Long = 60_000) {
        val held = _session.value ?: return
        if (held.expiresAtMs - nowMs() > marginMs) return
        val cfg = requireConfig()
        val body = buildJsonObject { put("grant_type", "refresh_token"); put("refresh_token", held.refreshToken) }
        val reply = post("${cfg.secureTokenBase}/token?key=${cfg.apiKey}", body)
        val idToken = reply["id_token"]?.jsonPrimitive?.contentOrNull
        val refreshToken = reply["refresh_token"]?.jsonPrimitive?.contentOrNull
        val expiresIn = reply["expires_in"]?.jsonPrimitive?.doubleOrNull
        if (idToken == null || refreshToken == null || expiresIn == null) throw OrbitalAuthException.Malformed("token refresh")
        val uid = claimsOf(idToken)["sub"]?.jsonPrimitive?.contentOrNull ?: held.uid
        adopt(OrbitalAuthSession(idToken, refreshToken, nowMs() + (expiresIn * 1000).toLong(), uid), persist = true)
    }

    fun signOut() {
        synchronized(lock) {
            refreshJob?.cancel()
            refreshJob = null
            _session.value = null
        }
        store.delete()
        onChange?.invoke(null)
    }

    private fun requireConfig(): OrbitalAuthConfig = config ?: throw OrbitalAuthException.NotConfigured

    private fun adopt(next: OrbitalAuthSession, persist: Boolean) {
        if (persist) store.write(json.encodeToString(OrbitalAuthSession.serializer(), next))
        val wait = maxOf(next.expiresAtMs - nowMs() - 60_000, 0)
        synchronized(lock) {
            _session.value = next
            refreshJob?.cancel()
            refreshJob = scope.launch {
                sleepMs(wait)
                runCatching { refreshIfNeeded() }
            }
        }
        onChange?.invoke(next)
    }

    private suspend fun post(url: String, body: JsonObject): JsonObject {
        val reply = transport.send("POST", url, body.toString())
        val parsed = runCatching { json.parseToJsonElement(reply.body).jsonObject }.getOrNull()
            ?: throw OrbitalAuthException.Malformed("non-JSON body (${reply.status})")
        if (reply.status !in 200..299) {
            val message = (parsed["error"] as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
            throw OrbitalAuthException.Provider(message ?: "sign-in failed (${reply.status})")
        }
        return parsed
    }

    private fun sessionFromSignIn(reply: JsonObject): OrbitalAuthSession {
        val idToken = reply["idToken"]?.jsonPrimitive?.contentOrNull
        val refreshToken = reply["refreshToken"]?.jsonPrimitive?.contentOrNull
        val expiresIn = reply["expiresIn"]?.jsonPrimitive?.doubleOrNull
        if (idToken == null || refreshToken == null || expiresIn == null) throw OrbitalAuthException.Malformed("sign-in")
        val uid = claimsOf(idToken)["sub"]?.jsonPrimitive?.contentOrNull ?: throw OrbitalAuthException.Malformed("id token has no subject")
        return OrbitalAuthSession(idToken, refreshToken, nowMs() + (expiresIn * 1000).toLong(), uid)
    }

    companion object {
        /** The ID token's payload claims. The server verifies the token; the client only reads it, as the web does. */
        @OptIn(ExperimentalEncodingApi::class)
        fun claimsOf(idToken: String): JsonObject {
            val parts = idToken.split('.')
            if (parts.size != 3) throw OrbitalAuthException.Malformed("id token is not a JWT")
            val padded = parts[1] + "=".repeat((4 - parts[1].length % 4) % 4)
            val payload = runCatching { Base64.UrlSafe.decode(padded).decodeToString() }.getOrNull()
                ?: throw OrbitalAuthException.Malformed("id token payload")
            return runCatching { Json.parseToJsonElement(payload).jsonObject }.getOrNull()
                ?: throw OrbitalAuthException.Malformed("id token payload")
        }

        /** `@user` for the kernel: `normalizeUserContext({...claims, uid: sub})`. `null` for a token with no subject. */
        fun viewerJson(idToken: String): String? {
            val claims = claimsOf(idToken)
            val uid = claims["sub"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() } ?: return null
            val derived = setOf("id", "name", "displayName", "email")
            val name = (claims["name"] ?: claims["displayName"])?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }
            val email = claims["email"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }
            val user = buildJsonObject {
                put("id", uid)
                put("uid", uid)
                for ((key, value) in claims) if (key !in derived && key != "uid") put(key, value)
                if (name != null) put("name", name)
                if (email != null) put("email", email)
            }
            return user.toString()
        }
    }
}

/**
 * Applies a session to the two places a signed-in viewer shows up: the `Authorization: Bearer`
 * plumbing every request reads, and the kernel's `@user`. It only reports whether the signed-in
 * identity changed; what an identity change repaints is the host's call, never inferred here.
 */
class OrbitalSessionBinding(private val setToken: (String?) -> Unit, private val setViewer: (String) -> Unit) {
    private var uid: String? = null

    fun apply(session: OrbitalAuthSession?): Boolean {
        setToken(session?.idToken)
        setViewer(session?.let { OrbitalAuth.viewerJson(it.idToken) } ?: "null")
        val changed = uid != session?.uid
        uid = session?.uid
        return changed
    }
}
