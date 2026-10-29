package generated.patterns.core

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64 as AndroidBase64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import generated.OrbitalViewModel
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Accessibility ids of the host sign-in route, addressed by UI tests (`testTag` is exposed as the resource id). */
object SignInIds {
    const val TITLE = "signin-title"
    const val EMAIL = "field-signin-email"
    const val PASSWORD = "field-signin-password"
    const val SUBMIT = "action-signin-submit"
    const val SIGN_OUT = "action-signin-signout"
    const val ERROR = "signin-error"
    const val STATUS = "signin-status"
    fun persona(id: String): String = "action-signin-persona-$id"
}

/** The provider calls go through OkHttp; the body is returned undecoded so [OrbitalAuth] owns every parse. */
class OkHttpAuthTransport(private val client: OkHttpClient = OkHttpClient()) : AuthTransport {
    override suspend fun send(method: String, url: String, body: String?): AuthReply = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).method(method, body?.toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { AuthReply(it.code, it.body?.string().orEmpty()) }
    }
}

/**
 * The session is AES-GCM encrypted under a non-exportable Android Keystore key and kept in private
 * preferences; an entry that no longer decrypts is dropped, never surfaced.
 */
class KeystoreSessionStore(context: Context) : AuthSessionStore {
    private val prefs = context.getSharedPreferences("orbital_auth", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply { init(spec) }.generateKey()
    }

    override fun read(): String? {
        val stored = prefs.getString(ENTRY, null) ?: return null
        val parts = stored.split(':')
        if (parts.size != 2) return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, AndroidBase64.decode(parts[0], AndroidBase64.NO_WRAP)))
            cipher.doFinal(AndroidBase64.decode(parts[1], AndroidBase64.NO_WRAP)).decodeToString()
        }.getOrElse {
            delete()
            null
        }
    }

    override fun write(json: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val sealed = cipher.doFinal(json.encodeToByteArray())
        val entry = AndroidBase64.encodeToString(cipher.iv, AndroidBase64.NO_WRAP) + ":" + AndroidBase64.encodeToString(sealed, AndroidBase64.NO_WRAP)
        prefs.edit().putString(ENTRY, entry).apply()
    }

    override fun delete() {
        prefs.edit().remove(ENTRY).apply()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "orbital_auth_session"
        const val ENTRY = "session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

/** The build-declared provider: `OrbitalFirebaseApiKey` / `OrbitalFirebaseProjectId` / `OrbitalAuthEmulatorHost` manifest meta-data. */
fun orbitalAuthConfig(context: Context, emulatorOverride: String?): OrbitalAuthConfig? {
    val meta = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData ?: return null
    return OrbitalAuthConfig.declared(
        meta.getString("OrbitalFirebaseApiKey"),
        meta.getString("OrbitalFirebaseProjectId"),
        meta.getString("OrbitalAuthEmulatorHost"),
        emulatorOverride,
    )
}

/**
 * The host's sign-in route (`HOST_SIGN_IN_ROUTE`, `/login`): the native twin of the web template's
 * `Login` and dev `PersonaSwitcher`. The provider call and the session live in [OrbitalAuth]; this
 * screen only collects input and shows the outcome. A failure is shown here and nothing is signed in.
 */
@Composable
fun OrbitalSignInScreen(viewModel: OrbitalViewModel) {
    val theme = LocalAlmadarTheme.current.color
    val auth = viewModel.auth
    val session by auth.session.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var personas by remember { mutableStateOf(emptyList<OrbitalPersona>()) }
    var failure by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { personas = auth.personas(viewModel.apiBase) }

    fun run(signIn: suspend () -> Unit) {
        failure = null
        busy = true
        scope.launch {
            try {
                signIn()
            } catch (e: OrbitalAuthException) {
                failure = e.message
            } catch (e: java.io.IOException) {
                failure = e.message ?: "network error"
            }
            busy = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Sign in", color = theme.foreground, modifier = Modifier.testTag(SignInIds.TITLE))
        val current = session
        when {
            !auth.isConfigured ->
                Text("Sign-in is not configured for this build.", color = theme.mutedForeground, modifier = Modifier.testTag(SignInIds.STATUS))
            current != null -> {
                Text("Signed in as ${current.uid}", color = theme.foreground, modifier = Modifier.testTag(SignInIds.STATUS))
                Button(onClick = { viewModel.signOut() }, modifier = Modifier.testTag(SignInIds.SIGN_OUT)) { Text("Sign out") }
            }
            else -> {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag(SignInIds.EMAIL),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag(SignInIds.PASSWORD),
                )
                Button(
                    onClick = { run { auth.signInWithPassword(email, password) } },
                    enabled = !busy,
                    modifier = Modifier.testTag(SignInIds.SUBMIT),
                ) { Text(if (busy) "Signing in..." else "Sign in") }
                if (personas.isNotEmpty()) {
                    Text("Dev personas", color = theme.mutedForeground)
                    personas.forEach { persona ->
                        OutlinedButton(
                            onClick = { run { auth.signInAsPersona(persona.id, viewModel.apiBase) } },
                            enabled = !busy,
                            modifier = Modifier.testTag(SignInIds.persona(persona.id)),
                        ) { Text(persona.label) }
                    }
                }
            }
        }
        failure?.let { Text(it, color = theme.error, modifier = Modifier.testTag(SignInIds.ERROR)) }
    }
}
