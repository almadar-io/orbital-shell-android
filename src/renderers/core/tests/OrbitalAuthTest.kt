package generated.patterns.core

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeTransport : AuthTransport {
    val replies = ArrayDeque<AuthReply>()
    val requests = mutableListOf<Triple<String, String, String?>>()
    override suspend fun send(method: String, url: String, body: String?): AuthReply {
        requests += Triple(method, url, body)
        return replies.removeFirst()
    }
}

private class MemoryStore : AuthSessionStore {
    var data: String? = null
    override fun read(): String? = data
    override fun write(json: String) { data = json }
    override fun delete() { data = null }
}

@OptIn(ExperimentalEncodingApi::class)
private fun jwt(claims: String): String =
    "e30." + Base64.UrlSafe.encode(claims.encodeToByteArray()).trimEnd('=') + ".sig"

private fun signInReply(token: String, expiresIn: Int = 3600) =
    AuthReply(200, """{"idToken":"$token","refreshToken":"r1","expiresIn":"$expiresIn"}""")

class OrbitalAuthTest {
    private val emulator = OrbitalAuthConfig("k", "demo-app", "127.0.0.1:9099")
    private val production = OrbitalAuthConfig("k", "app", null)
    private val epochMs = 1_000_000_000L
    private val neverSleep: suspend (Long) -> Unit = { awaitCancellation() }

    private fun auth(
        config: OrbitalAuthConfig?,
        transport: FakeTransport,
        store: MemoryStore = MemoryStore(),
        nowMs: () -> Long = { epochMs },
        sleepMs: suspend (Long) -> Unit = neverSleep,
    ) = OrbitalAuth(config, transport, store, CoroutineScope(Dispatchers.Unconfined), nowMs, sleepMs)

    @Test
    fun email_sign_in_posts_to_the_emulator_and_stores_the_session() = runBlocking {
        val transport = FakeTransport().apply { replies += signInReply(jwt("""{"sub":"u1"}""")) }
        val store = MemoryStore()
        val sut = auth(emulator, transport, store)
        val seen = mutableListOf<OrbitalAuthSession?>()
        sut.onChange = { seen += it }

        sut.signInWithPassword("a@b.c", "pw")

        assertEquals("http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=k", transport.requests.first().second)
        assertEquals("u1", sut.session.value?.uid)
        assertEquals(epochMs + 3_600_000, sut.session.value?.expiresAtMs)
        assertNotNull(store.data)
        assertEquals(1, seen.size)
    }

    @Test
    fun production_sign_in_uses_the_google_hosts_and_a_failure_stores_nothing() = runBlocking {
        val transport = FakeTransport().apply { replies += AuthReply(400, """{"error":{"code":400,"message":"INVALID_PASSWORD"}}""") }
        val store = MemoryStore()
        val sut = auth(production, transport, store)
        var changes = 0
        sut.onChange = { changes++ }

        val failure = runCatching { sut.signInWithPassword("a@b.c", "bad") }.exceptionOrNull()

        assertTrue(failure is OrbitalAuthException.Provider)
        assertEquals("INVALID_PASSWORD", failure?.message)
        assertTrue(transport.requests.first().second.startsWith("https://identitytoolkit.googleapis.com/"))
        assertNull(sut.session.value)
        assertNull(store.data)
        assertEquals(0, changes)
    }

    @Test
    fun unconfigured_build_refuses_without_a_request() = runBlocking {
        val transport = FakeTransport()
        val sut = auth(null, transport)
        assertFalse(sut.isConfigured)
        val failure = runCatching { sut.signInWithPassword("a@b.c", "pw") }.exceptionOrNull()
        assertEquals(OrbitalAuthException.NotConfigured, failure)
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun persona_sign_in_exchanges_the_server_custom_token() = runBlocking {
        val transport = FakeTransport().apply {
            replies += AuthReply(200, """{"success":true,"customToken":"ct","authEmulatorHost":"127.0.0.1:9099","projectId":"demo-app"}""")
            replies += signInReply(jwt("""{"sub":"p-1","role":"admin"}"""))
        }
        val sut = auth(emulator, transport)

        sut.signInAsPersona("p-1", "http://127.0.0.1:3030")

        assertEquals("http://127.0.0.1:3030/api/personas/sign-in", transport.requests[0].second)
        assertTrue(transport.requests[1].second.contains("accounts:signInWithCustomToken"))
        assertEquals("p-1", sut.session.value?.uid)
    }

    @Test
    fun persona_sign_in_failure_surfaces_the_server_error_and_signs_nobody_in() = runBlocking {
        val transport = FakeTransport().apply { replies += AuthReply(404, """{"success":false,"error":"no persona with id x"}""") }
        val sut = auth(emulator, transport)

        val failure = runCatching { sut.signInAsPersona("x", "http://127.0.0.1:3030") }.exceptionOrNull()

        assertEquals("no persona with id x", failure?.message)
        assertNull(sut.session.value)
        assertEquals(1, transport.requests.size)
    }

    @Test
    fun personas_lists_the_roster_and_is_empty_when_the_server_offers_none() = runBlocking {
        val transport = FakeTransport().apply {
            replies += AuthReply(200, """{"success":true,"personas":[{"id":"p-1","name":"Maya","role":"admin"},{"id":"p-2"}]}""")
            replies += AuthReply(404, "{}")
        }
        val sut = auth(emulator, transport)
        assertEquals(listOf(OrbitalPersona("p-1", "Maya - admin"), OrbitalPersona("p-2", "p-2 - no role")), sut.personas("http://h"))
        assertEquals(emptyList<OrbitalPersona>(), sut.personas("http://h"))
    }

    @Test
    fun refresh_runs_only_when_the_token_is_near_expiry() = runBlocking {
        val transport = FakeTransport().apply {
            replies += signInReply(jwt("""{"sub":"u1"}"""), expiresIn = 30)
            replies += AuthReply(200, """{"id_token":"${jwt("""{"sub":"u1"}""")}","refresh_token":"r2","expires_in":"3600"}""")
        }
        val sut = auth(emulator, transport)
        sut.signInWithPassword("a@b.c", "pw")

        sut.refreshIfNeeded()

        assertTrue(transport.requests[1].second.contains("securetoken.googleapis.com/v1/token?key=k"))
        assertEquals("r2", sut.session.value?.refreshToken)
        assertEquals(epochMs + 3_600_000, sut.session.value?.expiresAtMs)

        sut.refreshIfNeeded()
        assertEquals("a fresh token must not be refreshed again", 2, transport.requests.size)
    }

    @Test
    fun a_session_schedules_its_refresh_a_minute_before_expiry_and_the_refresh_adopts_the_new_token() = runBlocking {
        val transport = FakeTransport().apply {
            replies += signInReply(jwt("""{"sub":"u1"}"""), expiresIn = 3600)
            replies += AuthReply(200, """{"id_token":"${jwt("""{"sub":"u1"}""")}","refresh_token":"r2","expires_in":"3600"}""")
        }
        var clock = epochMs
        val waits = mutableListOf<Long>()
        val refreshed = CompletableDeferred<Unit>()
        val sut = auth(emulator, transport, nowMs = { clock }, sleepMs = { ms ->
            waits += ms
            if (waits.size > 1) awaitCancellation()
            clock += ms
        })
        sut.onChange = { if (it?.refreshToken == "r2") refreshed.complete(Unit) }

        sut.signInWithPassword("a@b.c", "pw")
        withTimeout(2000) { refreshed.await() }

        assertEquals(3_540_000L, waits.first())
        assertEquals("r2", sut.session.value?.refreshToken)
    }

    @Test
    fun sign_out_clears_the_stored_session_and_reports_null() = runBlocking {
        val transport = FakeTransport().apply { replies += signInReply(jwt("""{"sub":"u1"}""")) }
        val store = MemoryStore()
        val sut = auth(emulator, transport, store)
        sut.signInWithPassword("a@b.c", "pw")
        var reported: OrbitalAuthSession? = OrbitalAuthSession("x", "x", 0, "x")
        sut.onChange = { reported = it }

        sut.signOut()

        assertNull(sut.session.value)
        assertNull(store.data)
        assertNull(reported)
    }

    @Test
    fun stored_session_is_adopted_the_stored_session_and_an_empty_store_restores_nothing() = runBlocking {
        val transport = FakeTransport().apply { replies += signInReply(jwt("""{"sub":"u1"}""")) }
        val store = MemoryStore()
        auth(emulator, transport, store).signInWithPassword("a@b.c", "pw")

        val restored = auth(emulator, FakeTransport(), store).also { it.loadStoredSession() }
        assertEquals("u1", restored.session.value?.uid)

        val empty = auth(emulator, FakeTransport()).also { it.loadStoredSession() }
        assertNull(empty.session.value)
    }

    @Test
    fun viewer_normalizes_claims_like_the_web_client() {
        val json = OrbitalAuth.viewerJson(jwt("""{"sub":"u1","name":"Maya","email":"m@x.io","role":"admin","user_id":"u1"}"""))
        val user = Json.parseToJsonElement(requireNotNull(json)).jsonObject
        assertEquals("u1", user["id"]?.jsonPrimitive?.content)
        assertEquals("u1", user["uid"]?.jsonPrimitive?.content)
        assertEquals("Maya", user["name"]?.jsonPrimitive?.content)
        assertEquals("m@x.io", user["email"]?.jsonPrimitive?.content)
        assertEquals("admin", user["role"]?.jsonPrimitive?.content)
    }

    @Test
    fun viewer_is_null_without_a_subject_and_a_malformed_token_throws() {
        assertNull(OrbitalAuth.viewerJson(jwt("""{"role":"admin"}""")))
        assertThrows(OrbitalAuthException.Malformed::class.java) { OrbitalAuth.viewerJson("not-a-jwt") }
    }

    @Test
    fun config_needs_key_and_project_and_the_debug_override_wins() {
        assertEquals("env:9099", OrbitalAuthConfig.declared("k", "p", "manifest:9099", "env:9099")?.emulatorHost)
        assertEquals("manifest:9099", OrbitalAuthConfig.declared("k", "p", "manifest:9099", null)?.emulatorHost)
        assertNull(OrbitalAuthConfig.declared("", "p", null, null))
        assertNull(OrbitalAuthConfig.declared("k", null, null, null))
        assertNull(OrbitalAuthConfig.declared("k", "p", null, null)?.emulatorHost)
    }

    private fun session(uid: String, token: String = jwt("""{"sub":"$uid","role":"admin"}""")) =
        OrbitalAuthSession(token, "r", 2_000_000_000L, uid)

    @Test
    fun a_session_attaches_its_token_and_binds_the_viewer_on_the_kernel() {
        val tokens = mutableListOf<String?>()
        val users = mutableListOf<String>()
        val binding = OrbitalSessionBinding({ tokens += it }, { users += it })
        val signedIn = session("u1")

        assertTrue(binding.apply(signedIn))

        assertEquals(listOf<String?>(signedIn.idToken), tokens)
        val user = Json.parseToJsonElement(users.first()).jsonObject
        assertEquals("u1", user["id"]?.jsonPrimitive?.content)
        assertEquals("admin", user["role"]?.jsonPrimitive?.content)
    }

    @Test
    fun sign_out_clears_the_token_and_the_kernel_viewer() {
        val tokens = mutableListOf<String?>()
        val users = mutableListOf<String>()
        val binding = OrbitalSessionBinding({ tokens += it }, { users += it })
        binding.apply(session("u1"))

        assertTrue(binding.apply(null))

        assertNull(tokens.last())
        assertEquals("null", users.last())
    }

    @Test
    fun a_refresh_of_the_same_identity_is_not_an_identity_change() {
        val binding = OrbitalSessionBinding({}, {})
        assertTrue(binding.apply(session("u1")))
        assertFalse(binding.apply(session("u1", jwt("""{"sub":"u1","iat":2}"""))))
        assertTrue(binding.apply(session("u2")))
    }

    @Test
    fun signing_out_when_nobody_is_signed_in_changes_nothing() {
        assertFalse(OrbitalSessionBinding({}, {}).apply(null))
    }
}
