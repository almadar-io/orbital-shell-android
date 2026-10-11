//! Android backend code generation.

use orbital_core::generate::{Backend, GeneratedFile, GeneratedOutput};
use orbital_core::ir::{
    classify_type, render_default, render_type, OirEntity, OirField, OirModule, OirType,
    TargetLiteralRenderer, TargetTypeRenderer, TypeMappingClass,
};
use orbital_core::schema::{OrbitalSchema, ServiceDefinition};
use orbital_core::str_utils::{
    escape_double_quoted, sanitize_identifier, split_to_camel_case, split_to_pascal_case,
    to_title_case,
};

/// Android code generation backend.
#[derive(Debug, Default, Clone)]
pub struct AndroidBackend {
    /// The schema's resolved form (every `uses` import inlined, every
    /// `TraitReference` flattened to `Inline`) — when set, `generate` bundles
    /// it as `assets/resolved_schema.json` so the in-app kernel (Program B,
    /// §159) mounts without a device-side compiler. `None` for callers with
    /// no resolved schema (existing tests/CLI paths), which emit no extra file.
    resolved_schema: Option<OrbitalSchema>,
}

impl AndroidBackend {
    /// Create a new Android backend.
    pub fn new() -> Self {
        Self::default()
    }

    /// Attach the schema's resolved form so `generate` also emits
    /// `assets/resolved_schema.json` into the app module.
    pub fn with_resolved_schema(mut self, schema: OrbitalSchema) -> Self {
        self.resolved_schema = Some(schema);
        self
    }
}

impl Backend for AndroidBackend {
    fn generate(&self, module: &OirModule) -> GeneratedOutput {
        let mut files = vec![];
        let mut warnings = vec![];

        files.push(generate_entities_file(
            &module.distinct_entities(),
            &module.named_types,
        ));
        files.push(generate_services_file(&module.services));
        files.push(generate_api_client_file(module));
        files.push(generate_slot_role_file());
        files.push(generate_effect_handlers_file());
        files.push(generate_view_model_file(module));
        files.push(generate_await_renders_file(module));
        files.push(generate_pattern_renderer_type_file());
        files.push(generate_fallback_renderer_file());
        files.extend(
            crate::pack::pack()
                .sources
                .iter()
                .map(|(rel, content)| GeneratedFile {
                    path: format!("patterns/core/{rel}"),
                    content: content.clone(),
                }),
        );
        files.extend(generate_game_bridge_files());
        files.push(generate_pattern_snapshot_test_file());
        files.push(generate_app_file(module));
        files.push(generate_generated_activity_file(module));
        if let Some(schema) = &self.resolved_schema {
            files.push(generate_resolved_schema_file(schema));
        }

        if module.entities.is_empty() {
            warnings.push("No entities declared in schema.".to_string());
        }
        if module.services.is_empty() {
            warnings.push("No services declared in schema.".to_string());
        }
        if module.traits.is_empty() {
            warnings.push("No traits declared in schema.".to_string());
        }
        if module.pages.is_empty() {
            warnings.push("No pages declared in schema.".to_string());
        }

        GeneratedOutput { files, warnings }
    }

    fn name(&self) -> &'static str {
        "android"
    }
}

fn generate_entities_file(
    entities: &[OirEntity],
    named_types: &std::collections::BTreeMap<String, OirType>,
) -> GeneratedFile {
    let mut content = String::from(
        "package generated\n\nimport kotlinx.serialization.Serializable\nimport kotlinx.serialization.SerialName\n\n",
    );

    // Named-type declarations (resolver-registered recursive aliases +
    // Phase-3D nominalized anonymous shapes) hoist at file top, before every
    // entity class that references them by bare name — mirroring the TS
    // shell's `emit_named_type_declarations`.
    let file_hoisted = std::cell::RefCell::new(Vec::new());
    for (name, ty) in named_types {
        oir_type_to_kotlin_with(ty, name, &file_hoisted);
    }
    for decl in file_hoisted.into_inner() {
        content.push_str(&decl);
        content.push_str("\n\n");
    }

    for entity in entities {
        content.push_str(&generate_entity(entity));
    }

    GeneratedFile {
        path: "Entities.kt".to_string(),
        content,
    }
}

/// Type names the generated Kotlin (stdlib, Compose and the shell's own runtime) uses: an entity
/// class of the same name would shadow it (`class Result` breaks `runCatching { }.getOrNull()`).
const KOTLIN_RESERVED_TYPES: &[&str] = &[
    "Any",
    "Array",
    "Boolean",
    "Box",
    "Button",
    "Card",
    "Char",
    "Color",
    "Column",
    "Double",
    "Enum",
    "Exception",
    "Float",
    "Image",
    "Int",
    "List",
    "Long",
    "Map",
    "Modifier",
    "Nothing",
    "Number",
    "Pair",
    "Result",
    "Row",
    "Set",
    "Short",
    "State",
    "String",
    "Surface",
    "Text",
    "Throwable",
    "Triple",
    "Unit",
];

/// The Kotlin class an entity is declared as: its PascalCase name, or `<Name>Entity` when that
/// name is a type the generated code uses. Only `Entities.kt` names entity types; the wire JSON
/// (field names) is unchanged.
fn kotlin_entity_type(entity: &str) -> String {
    let name = split_to_pascal_case(entity);
    if KOTLIN_RESERVED_TYPES.contains(&name.as_str()) {
        format!("{name}Entity")
    } else {
        name
    }
}

fn generate_entity(entity: &OirEntity) -> String {
    let mut out = String::new();
    let class_name = kotlin_entity_type(&entity.name);

    // Emit nested enums for enum-valued fields first.
    for field in &entity.fields {
        if let Some(values) = &field.enum_values {
            if !values.is_empty() {
                let enum_name = enum_class_name(&class_name, &field.name);
                out.push_str("@Serializable\n");
                out.push_str(&format!("enum class {} {{", enum_name));
                let variants: Vec<String> = values
                    .iter()
                    .map(|v| {
                        format!(
                            "\n    @SerialName(\"{}\")\n    {}",
                            escape_double_quoted(v),
                            split_to_pascal_case(v)
                        )
                    })
                    .collect();
                out.push_str(&variants.join(","));
                out.push_str("\n}\n\n");
            }
        }
    }

    let hoisted = std::cell::RefCell::new(Vec::new());
    let mut fields_out = String::new();

    for field in entity.fields.iter().filter(|f| f.name != "id") {
        let field_name = kotlin_identifier(&split_to_camel_case(&field.name));
        let (type_hint, optional) = type_for_field(entity, field);
        let kotlin_type = oir_type_to_kotlin_with(
            &type_hint,
            &format!("{}{}", class_name, split_to_pascal_case(&field.name)),
            &hoisted,
        );
        let default = field.default.as_ref().and_then(|v| {
            match render_default(&KotlinLiteral, v, &type_hint, None) {
                Ok(rendered) => rendered,
                Err(mismatch) => Some(mismatch.marker()),
            }
        });
        let opt = if optional { "?" } else { "" };

        let decl = if let Some(d) = default {
            format!(
                "    @SerialName(\"{}\")\n    val {}: {}{} = {}",
                field.name, field_name, kotlin_type, opt, d
            )
        } else if optional {
            format!(
                "    @SerialName(\"{}\")\n    val {}: {}? = null",
                field.name, field_name, kotlin_type
            )
        } else {
            format!(
                "    @SerialName(\"{}\")\n    val {}: {}",
                field.name, field_name, kotlin_type
            )
        };
        fields_out.push_str(&decl);
        fields_out.push_str(",\n");
    }

    // Hoisted struct/union declarations render before the entity class, the
    // same position as the field enums above.
    for decl in hoisted.into_inner() {
        out.push_str(&decl);
        out.push_str("\n\n");
    }

    out.push_str(&format!(
        "/**\n * {} entity.\n */\n",
        entity.description.as_deref().unwrap_or(&entity.name)
    ));
    out.push_str("@Serializable\n");
    out.push_str(&format!("data class {}(\n", class_name));
    out.push_str("    val id: String,\n");
    out.push_str(&fields_out);

    out.push_str(")\n\n");
    out
}

fn oir_type_to_kotlin_with(
    type_: &OirType,
    context_name: &str,
    hoisted: &std::cell::RefCell<Vec<String>>,
) -> String {
    render_type(
        &KotlinRenderer {
            context_name: context_name.to_string(),
            hoisted,
        },
        type_,
        None,
    )
}

/// The type to render at `field`'s position plus whether that position is
/// optional, routed through `OirField::effective_type` so an
/// `Optional`-wrapped `field_type` and a `required: false` flag are read
/// once — never combined into a double `?` downstream. An enum-valued field
/// (`field.enum_values` set) still overrides the rendered type to
/// `<Entity><Field>` regardless of its declared `OirType`, but the
/// optionality comes from `effective_type` the same as every other field.
fn type_for_field(entity: &OirEntity, field: &OirField) -> (OirType, bool) {
    let (ty, optional) = field.effective_type();
    if field.enum_values.is_some() {
        let enum_name = enum_class_name(&kotlin_entity_type(&entity.name), &field.name);
        return (OirType::Enum { name: enum_name }, optional);
    }
    (ty.clone(), optional)
}

fn enum_class_name(entity_name: &str, field_name: &str) -> String {
    format!("{}{}", entity_name, split_to_pascal_case(field_name))
}

fn generate_services_file(services: &[orbital_core::schema::ServiceRef]) -> GeneratedFile {
    let mut content = String::from(
        "package generated\n\nimport kotlinx.serialization.Serializable\nimport kotlinx.serialization.SerialName\n\n",
    );
    content.push_str("/**\n * Service configuration generated from the schema.\n */\n\n");

    for service_ref in services {
        let Some(def) = service_ref.as_inline() else {
            continue;
        };

        match def {
            ServiceDefinition::Rest(rest) => {
                let class_name = format!("{}RestConfig", split_to_pascal_case(&rest.name));
                content.push_str("@Serializable\n");
                content.push_str(&format!("data class {}(\n", class_name));
                content.push_str(&format!(
                    "    @SerialName(\"baseUrl\")\n    val baseUrl: String = \"{}\",\n",
                    escape_double_quoted(&rest.base_url)
                ));
                if let Some(headers) = &rest.headers {
                    let header_entries: Vec<String> = headers
                        .iter()
                        .map(|(k, v)| {
                            format!(
                                "        \"{}\" to \"{}\"",
                                escape_double_quoted(k),
                                escape_double_quoted(v)
                            )
                        })
                        .collect();
                    content.push_str("    @SerialName(\"headers\")\n");
                    content.push_str(&format!(
                        "    val headers: Map<String, String> = mapOf(\n{}\n    ),\n",
                        header_entries.join(",\n")
                    ));
                }
                content.push_str(")\n\n");
            }
            ServiceDefinition::Socket(socket) => {
                let class_name = format!("{}SocketConfig", split_to_pascal_case(&socket.name));
                content.push_str("@Serializable\n");
                content.push_str(&format!("data class {}(\n", class_name));
                content.push_str(&format!(
                    "    @SerialName(\"url\")\n    val url: String = \"{}\"\n) \n\n",
                    escape_double_quoted(&socket.url)
                ));
            }
            ServiceDefinition::Mcp(mcp) => {
                let class_name = format!("{}McpConfig", split_to_pascal_case(&mcp.name));
                content.push_str("@Serializable\n");
                content.push_str(&format!("data class {}(\n", class_name));
                content.push_str(&format!(
                    "    @SerialName(\"serverPath\")\n    val serverPath: String = \"{}\"\n) \n\n",
                    escape_double_quoted(&mcp.server_path)
                ));
            }
        }
    }

    GeneratedFile {
        path: "Services.kt".to_string(),
        content,
    }
}

/// `OrbitalApi` narrows to the two calls a kernel-backed client needs: post
/// one server leg (the "server leg" a local `kernel.dispatch` produced) to
/// the canonical route, and subscribe to the live-push channel. The caller
/// (the generated view model) supplies `orbital` — the codegen-baked
/// first-declared-orbital name (`AndroidBackend::generate`), the SAME name
/// `orbital_kernel_init` resolves on-device, so the route always addresses
/// the orbital the local kernel actually registered.
fn generate_api_client_file(_module: &OirModule) -> GeneratedFile {
    let content = format!(
        r#"package generated

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
     *  build's verify persona. Read on every request and every live-push connect; unset = anonymous. */
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
"#,
    );

    GeneratedFile {
        path: "ApiClient.kt".to_string(),
        content,
    }
}

fn generate_slot_role_file() -> GeneratedFile {
    let content = r#"package generated

/** Layout role for a named UI slot — mirrors orbital_core::SlotRole. */
enum class SlotRole {
    Main, Sidebar, Drawer, Modal, Overlay, Toast, HudTop, HudBottom, Floating, System
}

/** Classify a slot name into its layout role. Mirrors orbital_core::slot_role. */
fun slotRole(name: String): SlotRole = when {
    name == "main" || name == "content" || name == "screen" -> SlotRole.Main
    name == "sidebar" -> SlotRole.Sidebar
    name == "drawer" || name == "dock" -> SlotRole.Drawer
    name == "modal" || name == "center" -> SlotRole.Modal
    name == "overlay" -> SlotRole.Overlay
    name == "toast" -> SlotRole.Toast
    name == "hud-bottom" -> SlotRole.HudBottom
    name == "hud" || name == "hud-top" -> SlotRole.HudTop
    name == "floating" -> SlotRole.Floating
    name == "system" -> SlotRole.System
    name.startsWith("overlay.") -> SlotRole.Overlay
    name.startsWith("hud.") -> SlotRole.HudTop
    else -> SlotRole.Main
}
"#
    .to_string();

    GeneratedFile {
        path: "SlotRole.kt".to_string(),
        content,
    }
}

fn generate_effect_handlers_file() -> GeneratedFile {
    let content = r#"package generated

import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Applies client-side effects the kernel handed back (`KernelEventResponse
 * .clientEffects`, one `["render-ui", slot, config, props?, priority?]` /
 * `["navigate", path, params?, options?]` / `["navigate-back"]` tuple per
 * entry — the canonical wire, `orbital_core::effects::ClientEffect`).
 * `traitContent` indexes every Main-role render by its SOURCE trait (never a
 * slot name) so `@trait.X` composition can resolve it. `sourceTrait` is the
 * CALLER-resolved attribution (the paired `clientEffectsByTrait[i].traitName`,
 * falling back to the request trait) — the wire no longer stamps it inside
 * the content itself. `onEmit` stays wired for `CLIENT_EFFECT_KINDS`
 * coverage (`orbital_core::effects::CLIENT_EFFECT_KINDS` still lists
 * `"emit"`), though the kernel's own `ClientEffect` never actually produces
 * that tuple — `emittedEvents` is informational only (kernel-fanned already).
 */
class EffectHandlers(
    private val uiSlots: MutableStateFlow<Map<String, JsonElement>>,
    private val traitContent: MutableStateFlow<Map<String, JsonElement>>,
    private val mainOrder: MutableStateFlow<List<String>>,
    private val lastMainWriter: MutableStateFlow<String?>,
    private val embeddedTraits: Set<String>,
    private val navigationEvents: MutableSharedFlow<String>,
    private val notifications: MutableSharedFlow<String>,
    private val onEmit: (trait: String, event: String, payload: JsonElement?) -> Unit
) {
    /**
     * Apply a single client effect array (e.g. ["render-ui", "main", {...}]),
     * attributed to `sourceTrait` (from `clientEffectsByTrait`, or the
     * request trait when no pairing exists). A null/absent third element on
     * "render-ui" clears/closes that slot or source's content; a present
     * fourth element (arity-4/5 form) merges its keys on top of the third
     * (config) as the resolved render props.
     */
    suspend fun apply(effect: JsonElement, sourceTrait: String, navController: NavHostController? = null) {
        val arr = effect.jsonArray
        if (arr.isEmpty()) return
        val kind = arr[0].jsonPrimitive.content
        when (kind) {
            "render-ui" -> {
                val slot = arr[1].jsonPrimitive.content
                val config = arr.getOrNull(2)?.takeUnless { it is JsonNull }
                val props = (arr.getOrNull(3) as? JsonObject)
                val content: JsonElement? = when {
                    props == null -> config
                    config is JsonObject -> JsonObject(config + props)
                    else -> props
                }
                if (content == null) {
                    // Main-role content is never stored as a slot — it is indexed by
                    // its source trait, and the page body stacks every live main writer.
                    if (slotRole(slot) == SlotRole.Main) {
                        traitContent.value = traitContent.value - sourceTrait
                        mainOrder.value = mainOrder.value - sourceTrait
                        if (lastMainWriter.value == sourceTrait) lastMainWriter.value = null
                    } else {
                        uiSlots.value = uiSlots.value - slot
                    }
                } else if (slotRole(slot) == SlotRole.Main) {
                    traitContent.value = traitContent.value + (sourceTrait to content)
                    if (sourceTrait !in embeddedTraits) {
                        if (sourceTrait !in mainOrder.value) mainOrder.value = mainOrder.value + sourceTrait
                        lastMainWriter.value = sourceTrait
                    }
                } else {
                    uiSlots.value = uiSlots.value + (slot to content)
                    // Index an overlay peer's content too (composition-only — a
                    // later Main-role render for the same source still wins).
                    if (!traitContent.value.containsKey(sourceTrait)) {
                        traitContent.value = traitContent.value + (sourceTrait to content)
                    }
                }
            }
            "navigate" -> {
                val path = arr[1].jsonPrimitive.content
                navigationEvents.emit(path)
                navController?.navigate(path)
            }
            "navigate-back" -> {
                navController?.popBackStack()
            }
            "notify" -> {
                val title = arr.getOrNull(1)?.jsonPrimitive?.content ?: ""
                val message = arr.getOrNull(2)?.jsonPrimitive?.content ?: ""
                notifications.emit("$title: $message")
            }
            "emit" -> {
                val event = arr.getOrNull(1)?.jsonPrimitive?.content ?: return
                onEmit(sourceTrait, event, arr.getOrNull(2))
            }
        }
    }
}
"#
    .to_string();

    GeneratedFile {
        path: "EffectHandlers.kt".to_string(),
        content,
    }
}

/// Trait name -> declared initial state (`OirTrait::initial_state`, falling
/// back to the first declared state). Seeded client-side because the server
/// is stateless per request and exposes no `GET /state` — mirrors the TS
/// hook's own `initialState` constant (`trait_logic.rs`).
fn kotlin_initial_states_table(module: &OirModule) -> String {
    if module.traits.is_empty() {
        return "emptyMap()".to_string();
    }
    let entries: Vec<String> = module
        .traits
        .iter()
        .map(|t| {
            let initial = t
                .initial_state()
                .map(|s| s.name.clone())
                .or_else(|| t.states.first().map(|s| s.name.clone()))
                .unwrap_or_else(|| "idle".to_string());
            format!(
                "        \"{}\" to \"{}\",",
                escape_double_quoted(&t.name),
                escape_double_quoted(&initial)
            )
        })
        .collect();
    format!("mapOf(\n{}\n    )", entries.join("\n"))
}

/// Traits referenced via `@trait.X` by a sibling's render-ui. They paint only inside the host's
/// frame, so they never own `main` (`orbital_core::ir::build_embedded_trait_set`).
fn kotlin_embedded_traits_set(module: &OirModule) -> String {
    let mut names: Vec<String> = orbital_core::ir::build_embedded_trait_set(module)
        .into_iter()
        .collect();
    names.sort();
    if names.is_empty() {
        return "emptySet()".to_string();
    }
    let quoted: Vec<String> = names
        .iter()
        .map(|n| format!("\"{}\"", escape_double_quoted(n)))
        .collect();
    format!("setOf({})", quoted.join(", "))
}

/// Traits that declare an `INIT` event — only these auto-dispatch on mount,
/// mirroring the TS shell's `generate_auto_init_effect` (a trait with no
/// declared `INIT` handler never sends one).
fn declares_init(module: &OirModule, name: &str) -> bool {
    module
        .traits
        .iter()
        .any(|t| t.name == name && t.events.iter().any(|e| e.key == "INIT"))
}

/// Traits INITed at app mount: every trait declaring `INIT` except those only
/// gated pages bind (`orbital_core::ir::gated_only_traits`), which INIT when
/// their page first composes for an authenticated viewer.
fn kotlin_traits_with_init_set(module: &OirModule) -> String {
    let gated = orbital_core::ir::gated_only_traits(module);
    let names: Vec<String> = module
        .traits
        .iter()
        .filter(|t| t.events.iter().any(|e| e.key == "INIT"))
        .filter(|t| !gated.contains(&t.name))
        .map(|t| format!("\"{}\"", escape_double_quoted(&t.name)))
        .collect();
    if names.is_empty() {
        "emptySet()".to_string()
    } else {
        format!("setOf({})", names.join(", "))
    }
}

/// `mapOf(path to listOf(trait, ...))` — each gated page's INIT-declaring gated-only traits.
fn kotlin_gated_page_traits(module: &OirModule) -> String {
    let entries: Vec<String> = orbital_core::ir::gated_page_traits(module)
        .into_iter()
        .filter_map(|(path, traits)| {
            let names: Vec<String> = traits
                .iter()
                .filter(|t| declares_init(module, t))
                .map(|t| format!("\"{}\"", escape_double_quoted(t)))
                .collect();
            (!names.is_empty()).then(|| {
                format!(
                    "\"{}\" to listOf({})",
                    escape_double_quoted(&path),
                    names.join(", ")
                )
            })
        })
        .collect();
    if entries.is_empty() {
        "emptyMap()".to_string()
    } else {
        format!("mapOf({})", entries.join(", "))
    }
}

/// `AwaitRenders.kt`: the compiler's `await_render` predictions (`OirModule::await_render_rows`)
/// outside the web's portal slots, read while a server leg is in flight.
fn generate_await_renders_file(module: &OirModule) -> GeneratedFile {
    let rows: Vec<_> = module
        .await_render_rows()
        .into_iter()
        .filter(|row| !orbital_core::slots::is_portal_slot(&row.slot))
        .collect();
    let rows = serde_json::to_string(&rows)
        .expect("await-render rows serialize")
        .replace('$', "${'$'}");
    let content = r###"package generated

import generated.patterns.core.KernelAwaitingTrait
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** What a slot shows while `requestTrait`, having taken `event` from one of `from` (any state when
 *  null), awaits its server round trip: the compiler's `await_render` prediction. */
@Serializable
data class AwaitRender(
    val requestTrait: String,
    val event: String,
    val from: List<String>? = null,
    @SerialName("trait") val trait: String,
    val slot: String,
    val skeleton: JsonElement,
)

object AwaitRenders {
    val all: List<AwaitRender> by lazy {
        runCatching { Json.decodeFromString(ListSerializer(AwaitRender.serializer()), JSON) }
            .onFailure { android.util.Log.e("Orbital", "await-render table failed to decode", it) }
            .getOrDefault(emptyList())
    }

    fun matching(awaiting: KernelAwaitingTrait): List<AwaitRender> = all.filter { row ->
        row.requestTrait == awaiting.trait && row.event == awaiting.event && (row.from?.contains(awaiting.from) ?: true)
    }

    private val JSON = """__ROWS__"""
}
"###
    .replace("__ROWS__", &rows);
    GeneratedFile {
        path: "AwaitRenders.kt".to_string(),
        content,
    }
}

fn generate_view_model_file(module: &OirModule) -> GeneratedFile {
    let base_url = module
        .services
        .iter()
        .filter_map(|s| s.as_inline())
        .find_map(|def| match def {
            ServiceDefinition::Rest(r) => Some(r.base_url.clone()),
            _ => None,
        })
        .unwrap_or_else(|| "http://10.0.2.2:3030".to_string());
    let initial_states = kotlin_initial_states_table(module);
    let traits_with_init = kotlin_traits_with_init_set(module);
    let gated_page_traits = kotlin_gated_page_traits(module);
    let embedded_traits = kotlin_embedded_traits_set(module);

    let content = format!(
        r#"package generated

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
    val contentWithAwaiting: StateFlow<Map<String, JsonElement>> = combine(_traitContent, _awaitingMain) {{ content, awaiting ->
        content + awaiting.filterKeys {{ it !in content }}.mapValues {{ skeletonNode(it.value) }}
    }}.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** `uiSlots`, with a `skeleton` node in an awaiting slot that is still empty. */
    val slotsWithAwaiting: StateFlow<Map<String, JsonElement>> = combine(_uiSlots, _awaitingSlots) {{ slots, awaiting ->
        slots + awaiting.filterKeys {{ it !in slots }}.mapValues {{ skeletonNode(it.value) }}
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
"#,
        base_url = base_url,
        initial_states = initial_states,
        traits_with_init = traits_with_init,
        gated_page_traits = gated_page_traits,
        embedded_traits = embedded_traits,
        host_sign_in_route = orbital_core::schema::HOST_SIGN_IN_ROUTE
    );

    GeneratedFile {
        path: "OrbitalViewModel.kt".to_string(),
        content,
    }
}

fn generate_pattern_renderer_type_file() -> GeneratedFile {
    let content = r#"package generated.patterns

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.JsonElement

/**
 * Common renderer signature for all Almadar patterns.
 */
typealias PatternRenderer = @Composable (
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) -> Unit
"#
    .to_string();

    GeneratedFile {
        path: "patterns/PatternRenderer.kt".to_string(),
        content,
    }
}

fn generate_fallback_renderer_file() -> GeneratedFile {
    let content = r#"package generated.patterns.core

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonElement

/**
 * Fallback renderer for patterns that do not have a native Compose mapping
 * yet. A release build must never show a debug placeholder to a real user,
 * so it renders nothing there; a debug build shows the placeholder card AND
 * logs the unsupported pattern name once (not on every recomposition). There
 * is no `BuildConfig.DEBUG` available here — this file lives in the fixed
 * `generated` package, not the app's `namespace` package `BuildConfig` is
 * generated into, so debuggability is read off the app's own manifest flag
 * instead (`ApplicationInfo.FLAG_DEBUGGABLE`), which works regardless of the
 * app's package name.
 */
private val loggedFallbackPatterns = kotlin.collections.mutableSetOf<String>()

@Composable
fun FallbackRenderer(
    patternName: String,
    props: JsonElement,
    modifier: Modifier = Modifier,
    onEvent: (String, JsonElement?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (!isDebug) return
    if (loggedFallbackPatterns.add(patternName)) {
        Log.w("Orbital", "Unsupported pattern: $patternName")
    }
    Card(modifier = modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Unsupported pattern: $patternName")
            Text("Props: $props")
        }
    }
}
"#
    .to_string();

    GeneratedFile {
        path: "patterns/core/FallbackRenderer.kt".to_string(),
        content,
    }
}

/// The page boundary for `access: authenticated`, the native twin of `PageAccessHost`
/// (`@almadar/ui/providers`): the circuit composes only for an authenticated viewer, keyed by that
/// viewer; any other viewer sees nothing and is sent to the app's declared `signIn` route.
const AUTHENTICATED_PAGE_KOTLIN: &str = r#"/** Page boundary of an `access: authenticated` page (twin of the web `PageAccessHost`). */
@Composable
private fun AuthenticatedPage(
    path: String,
    signIn: String,
    viewModel: OrbitalViewModel,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val session by viewModel.session.collectAsState()
    if (session != null || viewModel.hasLaunchToken) {
        val viewer = session?.uid ?: "launch-token"
        LaunchedEffect(path, viewer) { viewModel.mountGatedPage(path, viewer) }
        key(session?.uid) { content() }
    } else {
        LaunchedEffect(path, signIn) {
            if (signIn.isNotEmpty()) navController.navigate(signIn) { popUpTo(path) { inclusive = true } }
        }
    }
}

"#;

fn generate_app_file(module: &OirModule) -> GeneratedFile {
    let app_name = to_title_case(&module.name);
    let start_route = module
        .pages
        .first()
        .map(|p| p.path.clone())
        .unwrap_or_else(|| "/".to_string());

    let sign_in = escape_double_quoted(
        module
            .site
            .as_ref()
            .and_then(|site| site.sign_in.as_deref())
            .unwrap_or_default(),
    );
    let page_routes: Vec<String> = module
        .pages
        .iter()
        .map(|p| {
            let path = escape_double_quoted(&p.path);
            let name = escape_double_quoted(&p.name);
            let trait_names = p
                .traits
                .iter()
                .map(|t| format!("\"{}\"", escape_double_quoted(&t.trait_name)))
                .collect::<Vec<_>>()
                .join(", ");
            let page = format!("GeneratedPage(\"{name}\", viewModel, listOf({trait_names}))");
            if p.access == Some(orbital_core::schema::PageAccess::Authenticated) {
                format!(
                    "                composable(\"{path}\") {{ AuthenticatedPage(\"{path}\", \"{sign_in}\", viewModel, navController) {{ {page} }} }}"
                )
            } else {
                format!("                composable(\"{path}\") {{ {page} }}")
            }
        })
        .collect();
    let any_gated = module
        .pages
        .iter()
        .any(|p| p.access == Some(orbital_core::schema::PageAccess::Authenticated));
    let authenticated_page = if any_gated {
        AUTHENTICATED_PAGE_KOTLIN
    } else {
        ""
    };
    let host_route = "                composable(OrbitalViewModel.HOST_SIGN_IN_ROUTE) { OrbitalSignInScreen(viewModel) }\n";
    let pages_block = if page_routes.is_empty() {
        format!("{host_route}                // no pages declared\n")
    } else {
        format!("{host_route}{}\n", page_routes.join("\n"))
    };

    let content = format!(
        r#"package generated

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import generated.patterns.core.AlmadarTheme
import generated.patterns.core.AlmadarThemeModeControl
import generated.patterns.core.LocalAlmadarCanvasTheme
import generated.patterns.core.LocalAlmadarResolvedMotion
import generated.patterns.core.AlmadarEnterTiming
import generated.patterns.core.almadarSlotEnter
import generated.patterns.core.almadarSurfaceEnter
import generated.patterns.core.LocalAlmadarThemeMode
import generated.patterns.core.ColorMode
import generated.patterns.core.CorePatternRegistry
import generated.patterns.core.OrbitalSignInScreen
import generated.patterns.core.VerificationIds
import generated.patterns.core.verificationContainer
import generated.patterns.core.verificationWindowRoot
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Root composable generated for {}.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GeneratedApp(viewModel: OrbitalViewModel = viewModel()) {{
    val navController = rememberNavController()
    val snackbarHostState = remember {{ SnackbarHostState() }}

    LaunchedEffect(Unit) {{
        viewModel.initAllTraits()
    }}

    LaunchedEffect(viewModel.isReady) {{
        if (viewModel.isReady) viewModel.openInitialRoute("{1}")
    }}

    // The native tick clock runs one pass per composition frame, only while the app is resumed.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel.isReady, viewModel.hasTicks) {{
        if (!viewModel.isReady || !viewModel.hasTicks) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {{
            viewModel.frameResume()
            try {{
                while (true) {{
                    val nowMs = withFrameNanos {{ it / 1_000_000L }}
                    viewModel.runFrame(nowMs)
                }}
            }} finally {{
                viewModel.framePause()
            }}
        }}
    }}

    LaunchedEffect(Unit) {{
        viewModel.navigationEvents.collect {{ path ->
            try {{
                navController.navigate(path)
            }} catch (e: IllegalArgumentException) {{
                android.util.Log.e("Orbital", "navigate to undeclared route $path", e)
            }}
        }}
    }}

    LaunchedEffect(Unit) {{
        viewModel.rootReplacements.collect {{ path ->
            try {{
                navController.navigate(path) {{
                    popUpTo(navController.graph.startDestinationId) {{ inclusive = true }}
                }}
            }} catch (e: IllegalArgumentException) {{
                android.util.Log.e("Orbital", "initial route is undeclared: $path", e)
            }}
        }}
    }}

    LaunchedEffect(Unit) {{
        navController.currentBackStackEntryFlow.collect {{ entry ->
            viewModel.onRouteChanged(entry.destination.route)
        }}
    }}

    LaunchedEffect(Unit) {{
        viewModel.notifications.collect {{ message ->
            snackbarHostState.showSnackbar(message)
        }}
    }}

    // The signed-in ID token is refreshed whenever the app returns to the foreground.
    val resumeOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {{
        resumeOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {{ viewModel.refreshSession() }}
    }}

    AlmadarTheme(tokens = viewModel.themeTokens, mode = viewModel.themeMode) {{
        CompositionLocalProvider(LocalAlmadarThemeMode provides AlmadarThemeModeControl(viewModel.themeMode == ColorMode.Dark) {{ viewModel.toggleThemeMode() }}, LocalAlmadarCanvasTheme provides viewModel.canvasTheme, LocalAlmadarResolvedMotion provides viewModel.motion) {{
        Scaffold(
            // testTag -> resource-id, so `uiautomator dump` exposes every `action-*`/`pattern-*`/`row-*`/`field-*` tag.
            modifier = Modifier.fillMaxSize().semantics {{ testTagsAsResourceId = true }},
            snackbarHost = {{ SnackbarHost(snackbarHostState) }}
        ) {{ padding ->
            // The kernel loads off the main thread (resolved_schema.json read +
            // register_schema) — show a loading state until its first snapshot
            // lands, never render a page against an unready kernel handle.
            if (!viewModel.isReady) {{
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {{
                    CircularProgressIndicator()
                }}
            }} else {{
                NavHost(
                    navController = navController,
                    startDestination = "{}",
                    modifier = Modifier.padding(padding)
                ) {{
{}}}
                }}
            }}
        }}
        }}
    }}

{}/**
 * Render a JsonElement slot via the pattern registry. `envelope` is always an
 * `eventEnvelope` (`RenderersCommon.kt`) — `{{"sourceTrait": …, "actionPayload":
 * …}}`, `actionPayload` absent when there is none — so a control under
 * `@trait.InlineButtonRender3` dispatches to that trait, not `traitName`/the
 * raw payload, although every SlotContent in a page shares one `traitName`
 * (mirrors `helpers.rs::queue_event`'s `source_trait`).
 */
@Composable
private fun SlotContent(
    content: JsonElement,
    modifier: Modifier = Modifier,
    traitName: String,
    viewModel: OrbitalViewModel,
    slot: String,
    tagged: Boolean = true
) {{
    val type = (content as? JsonObject)?.get("type")?.jsonPrimitive?.content ?: "unknown"
    Box(modifier = if (tagged) Modifier.verificationContainer(VerificationIds.slot(slot)) else Modifier) {{
        CorePatternRegistry.get(type)(
            content,
            modifier,
            {{ event, envelope ->
                if (event == OrbitalViewModel.NAVIGATE_EVENT) {{
                    viewModel.navigate(envelope)
                }} else if (event == OrbitalViewModel.SIGN_OUT_EVENT) {{
                    viewModel.signOut()
                }} else {{
                    val obj = envelope as? JsonObject
                    val source = obj?.get("sourceTrait")?.jsonPrimitive?.contentOrNull ?: traitName
                    val payload = obj?.get("actionPayload")
                    viewModel.sendEvent(source, event, payload = payload)
                }}
            }}
        )
    }}
}}

/**
 * Generated screen for a single page.
 *
 * Renders the Main-role slot as the page body; every other populated slot is
 * dispatched by its SlotRole to the appropriate Compose overlay widget.
 * A null slot value (cleared by "render-ui <slot> null") closes that overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedPage(pageName: String, viewModel: OrbitalViewModel, traitNames: List<String>) {{
    val uiSlots by viewModel.slotsWithAwaiting.collectAsState()
    val traitContentMap by viewModel.contentWithAwaiting.collectAsState()
    val mainOrder by viewModel.mainOrderWithAwaiting.collectAsState()
    val lastMainWriter by viewModel.lastMainWriter.collectAsState()
    val paintGeneration by viewModel.paintGeneration.collectAsState()
    LaunchedEffect(paintGeneration) {{
        withFrameNanos {{ }}
        viewModel.reportPainted(paintGeneration)
    }}

    // Bound traits with a live Main-role render, in first-write order. The trait
    // events fall back to (and the verify bridge routes to) is the most recent
    // writer, else the last live trait, else the first declared one.
    val liveTraits = mainOrder.filter {{ it in traitNames }}
    val traitName = lastMainWriter?.takeIf {{ it in liveTraits }} ?: liveTraits.lastOrNull() ?: traitNames.firstOrNull() ?: ""

    LaunchedEffect(Unit) {{
        traitNames.filter {{ it.isNotEmpty() }}.forEach {{ viewModel.initTrait(it) }}
    }}
    LaunchedEffect(traitName) {{
        if (traitName.isNotEmpty()) viewModel.activePageTrait = traitName
    }}

    // The page body stacks every live writer's Main-role render, each spliced
    // against every peer's current content; every other populated slot is
    // classified by SlotRole and spliced the same way (recomputed whenever
    // `traitContent` changes, so a peer's later render re-splices reactively).
    val mainContents = liveTraits.mapNotNull {{ name ->
        traitContentMap[name]?.let {{ name to spliceContent(it, traitContentMap, name) }}
    }}
    val modalSlot = uiSlots.entries
        .firstOrNull {{ slotRole(it.key) == SlotRole.Modal }}
        ?.let {{ it.key to spliceContent(it.value, traitContentMap, traitName) }}
    val drawerSlot = uiSlots.entries
        .firstOrNull {{ slotRole(it.key) == SlotRole.Drawer }}
        ?.let {{ it.key to spliceContent(it.value, traitContentMap, traitName) }}
    val overlaySlots = uiSlots.entries
        .filter {{ slotRole(it.key) == SlotRole.Overlay }}
        .map {{ it.key to spliceContent(it.value, traitContentMap, traitName) }}
    val toastSlot = uiSlots.entries
        .firstOrNull {{ slotRole(it.key) == SlotRole.Toast }}
        ?.let {{ it.key to spliceContent(it.value, traitContentMap, traitName) }}
    val hudTopSlot = uiSlots.entries
        .firstOrNull {{ slotRole(it.key) == SlotRole.HudTop }}
        ?.let {{ it.key to spliceContent(it.value, traitContentMap, traitName) }}
    val hudBottomSlot = uiSlots.entries
        .firstOrNull {{ slotRole(it.key) == SlotRole.HudBottom }}
        ?.let {{ it.key to spliceContent(it.value, traitContentMap, traitName) }}
    val sidebarSlot = uiSlots.entries
        .firstOrNull {{ slotRole(it.key) == SlotRole.Sidebar }}
        ?.let {{ it.key to spliceContent(it.value, traitContentMap, traitName) }}
    val floatingSlots = uiSlots.entries
        .filter {{ slotRole(it.key) == SlotRole.Floating }}
        .map {{ it.key to spliceContent(it.value, traitContentMap, traitName) }}

    val drawerState = rememberDrawerState(
        initialValue = if (drawerSlot != null) DrawerValue.Open else DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    LaunchedEffect(drawerSlot) {{
        if (drawerSlot != null) scope.launch {{ drawerState.open() }}
        else scope.launch {{ drawerState.close() }}
    }}

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {{
            drawerSlot?.let {{ (name, content) ->
                SlotContent(content, Modifier.fillMaxHeight().wrapContentSize(), traitName, viewModel, name)
            }}
        }}
    ) {{
        Box(modifier = Modifier.fillMaxSize()) {{
            // Page body: sidebar + main content in a Row when sidebar is populated.
            if (sidebarSlot != null) {{
                Row(modifier = Modifier.fillMaxSize()) {{
                    SlotContent(
                        sidebarSlot.second,
                        Modifier.fillMaxHeight().wrapContentSize(),
                        traitName,
                        viewModel,
                        sidebarSlot.first
                    )
                    Column(modifier = Modifier.weight(1f).almadarSurfaceEnter("pageEnter", AlmadarEnterTiming.Page, replayKey = pageName)) {{
                        PageBody(traitName, mainContents, viewModel)
                    }}
                }}
            }} else {{
                Column(modifier = Modifier.fillMaxSize().almadarSurfaceEnter("pageEnter", AlmadarEnterTiming.Page, replayKey = pageName)) {{
                    PageBody(traitName, mainContents, viewModel)
                }}
            }}

            // HUD overlays pinned top/bottom.
            hudTopSlot?.let {{ (name, content) ->
                Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {{
                    SlotContent(content, Modifier.fillMaxWidth(), traitName, viewModel, name)
                }}
            }}
            hudBottomSlot?.let {{ (name, content) ->
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {{
                    SlotContent(content, Modifier.fillMaxWidth(), traitName, viewModel, name)
                }}
            }}

            // Full-screen overlay(s).
            overlaySlots.forEach {{ (name, content) ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {{
                    SlotContent(content, Modifier.wrapContentSize(), traitName, viewModel, name)
                }}
            }}

            // Floating layer: full page size, the content aligns itself; no pointer input so touches pass through.
            floatingSlots.forEach {{ (name, content) ->
                Box(modifier = Modifier.fillMaxSize()) {{
                    SlotContent(content, Modifier.fillMaxSize(), traitName, viewModel, name)
                }}
            }}

            // Toast notification.
            toastSlot?.let {{ (name, content) ->
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).almadarSurfaceEnter("toastEnter", AlmadarEnterTiming.Toast, replayKey = name)) {{
                    SlotContent(content, Modifier.wrapContentSize(), traitName, viewModel, name)
                }}
            }}
        }}
    }}

    // Modal dialog (centered).
    modalSlot?.let {{ (name, content) ->
        Dialog(onDismissRequest = {{
            // The state machine drives clearing — emit nothing here.
        }}) {{
            Box(modifier = Modifier.verificationWindowRoot()) {{
                SlotContent(content, Modifier.wrapContentSize(), traitName, viewModel, name)
            }}
        }}
    }}
}}

/** The slot entry replays when another trait or root pattern takes the slot (`useSlotEnterRestart`). */
private fun slotEnterKey(trait: String, content: JsonElement): String =
    trait + ":" + (((content as? JsonObject)?.get("type") as? JsonPrimitive)?.contentOrNull ?: "")

@Composable
private fun PageBody(
    traitName: String,
    mainContents: List<Pair<String, JsonElement>>,
    viewModel: OrbitalViewModel
) {{
    when (mainContents.size) {{
        0 -> {{}}
        1 -> SlotContent(mainContents[0].second, Modifier.fillMaxWidth().almadarSlotEnter(mainContents[0].second, slotEnterKey(mainContents[0].first, mainContents[0].second)), mainContents[0].first, viewModel, "main")
        else -> Column(
            modifier = Modifier.fillMaxWidth().verificationContainer(VerificationIds.slot("main")),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {{
            mainContents.forEach {{ (name, content) ->
                SlotContent(content, Modifier.fillMaxWidth().almadarSlotEnter(content, slotEnterKey(name, content)), name, viewModel, "main", tagged = false)
            }}
        }}
    }}
}}
"#,
        app_name, start_route, pages_block, authenticated_page
    );

    GeneratedFile {
        path: "App.kt".to_string(),
        content,
    }
}

fn generate_generated_activity_file(_module: &OirModule) -> GeneratedFile {
    let content = r#"package generated

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Generated entry activity for the Android shell.
 *
 * `orb verify` starts the in-app verify bridge with
 * `am start -e ORBITAL_VERIFY_BRIDGE 1 [-e ORBITAL_VERIFY_PORT <port>]` (default 7357); the
 * extras are honoured only in a debuggable build and read before the view model exists.
 * `-e ORBITAL_API_BASE_URL <url>` points the app at a server other than the compiled-in one.
 * `-e ORBITAL_AUTH_TOKEN <idToken>` sends that verify persona's token as `Authorization: Bearer`.
 * `-e ORBITAL_AUTH_EMULATOR_HOST <host:port>` points native sign-in at another Auth emulator.
 */
class GeneratedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debuggable) {
            VerifyBridgeLaunch.apiBaseUrl = intent.getStringExtra("ORBITAL_API_BASE_URL")?.takeIf { it.isNotEmpty() }
            VerifyBridgeLaunch.initialRoute = intent.getStringExtra("ORBITAL_INITIAL_ROUTE")?.takeIf { it.isNotEmpty() }
            VerifyBridgeLaunch.authToken = intent.getStringExtra("ORBITAL_AUTH_TOKEN")?.takeIf { it.isNotEmpty() }
            VerifyBridgeLaunch.authEmulatorHost = intent.getStringExtra("ORBITAL_AUTH_EMULATOR_HOST")?.takeIf { it.isNotEmpty() }
        }
        if (debuggable && intent.getStringExtra("ORBITAL_VERIFY_BRIDGE") == "1") {
            VerifyBridgeLaunch.port = intent.getStringExtra("ORBITAL_VERIFY_PORT")?.toIntOrNull() ?: 7357
        }
        setContent {
            GeneratedApp()
        }
    }
}
"#
    .to_string();

    GeneratedFile {
        path: "GeneratedActivity.kt".to_string(),
        content,
    }
}

/// Bundle the schema's resolved form (Program B, §159: the in-app
/// `RuntimeKernel` mounts on-device, so it needs the same flattened
/// `OrbitalSchema` `orb verify`/`orb dev` resolve, with no compiler on
/// device) under `app/src/main/assets/`, where `AssetManager` reads it at
/// runtime.
fn generate_resolved_schema_file(schema: &OrbitalSchema) -> GeneratedFile {
    let content = orbital_core::util::canonical_json::to_canonical_string_pretty(schema)
        .unwrap_or_else(|e| format!("{{\"error\": \"failed to serialize resolved schema: {e}\"}}"));
    GeneratedFile {
        path: "app/src/main/assets/resolved_schema.json".to_string(),
        content,
    }
}

/// The Kotlin leaf-renderer behind the shared `orbital_core` walker
/// ([`render_type`]). `context_name` names the `data class` a `Struct` field
/// declares; the declaration is HOISTED into `hoisted` (emitted before the
/// entity class) and the field references it by bare name — a Kotlin type
/// position cannot carry an inline class declaration. Nested structs extend
/// the name positionally (`{Entity}{Field}{Nested}…`), so sibling fields in a
/// shared context never collide. Kotlin merges `Number`/`Integer` into
/// `Double` and ignores the field-level `enum_values` sidecar (enums are
/// pre-rewritten to `OirType::Enum` at the field layer by `type_for_field`,
/// not handled here).
struct KotlinRenderer<'a> {
    context_name: String,
    hoisted: &'a std::cell::RefCell<Vec<String>>,
}

impl TargetTypeRenderer for KotlinRenderer<'_> {
    type Out = String;

    fn string(&self, _enum_values: Option<&[String]>) -> String {
        "String".to_string()
    }
    fn number(&self) -> String {
        "Double".to_string()
    }
    fn integer(&self) -> String {
        "Double".to_string()
    }
    fn boolean(&self) -> String {
        "Boolean".to_string()
    }
    fn date(&self) -> String {
        "String".to_string()
    }
    fn json(&self) -> String {
        "kotlinx.serialization.json.JsonElement".to_string()
    }
    fn scalar(&self) -> String {
        // `JsonPrimitive` IS the closed `string | number | boolean | null`
        // union — the exact supertype of the four legal members — and it is
        // @Serializable, so a data-class field typed `JsonPrimitive` round-trips.
        // Fully qualified: Entities.kt's import header stays unchanged.
        "kotlinx.serialization.json.JsonPrimitive".to_string()
    }
    fn enum_ty(&self, name: &str, _enum_values: Option<&[String]>) -> String {
        split_to_pascal_case(name)
    }
    fn trait_ref(&self) -> String {
        "String".to_string()
    }
    fn sexpr(&self) -> String {
        "kotlinx.serialization.json.JsonElement".to_string()
    }
    fn type_ref(&self, name: &str) -> String {
        // The named declaration hoists at the top of Entities.kt from
        // `OirModule::named_types` (resolver-registered recursive aliases AND
        // Phase-3D nominalized anonymous shapes) — the field references it by
        // bare name, mirroring the TS shell (types.rs `type_ref`).
        name.to_string()
    }
    fn row(&self, entity: &str) -> String {
        kotlin_entity_type(entity)
    }
    fn array(&self, _inner_ty: &OirType, inner: String) -> String {
        format!("List<{}>", inner)
    }
    fn map(&self, _key_ty: &OirType, key: String, _value_ty: &OirType, value: String) -> String {
        format!("Map<{}, {}>", key, value)
    }
    fn optional(&self, _inner_ty: &OirType, inner: String) -> String {
        format!("{}?", inner)
    }
    fn struct_ty(&self, fields: &[OirField]) -> String {
        let name = self.context_name.clone();
        let parts: Vec<String> = fields
            .iter()
            .map(|f| {
                let child = KotlinRenderer {
                    context_name: format!("{}{}", name, split_to_pascal_case(&f.name)),
                    hoisted: self.hoisted,
                };
                let (inner_ty, optional) = f.effective_type();
                let t = render_type(&child, inner_ty, f.enum_values.as_deref());
                let opt = if optional { "?" } else { "" };
                format!(
                    "val {}: {}{}",
                    kotlin_identifier(&split_to_camel_case(&f.name)),
                    t,
                    opt
                )
            })
            .collect();
        self.hoisted.borrow_mut().push(format!(
            "@Serializable\ndata class {}({})",
            name,
            parts.join(", ")
        ));
        name
    }
    fn union_ty(&self, variants: &[OirField]) -> String {
        if variants.is_empty() {
            return self.json();
        }
        let name = self.context_name.clone();
        // A real discriminated union: a sealed class whose @Serializable
        // subtypes are tagged by @SerialName (kotlinx.serialization's
        // discriminator). Each variant's struct fields render through a child
        // renderer so nested shapes hoist under `{Name}{Variant}…`.
        let subtypes: Vec<String> = variants
            .iter()
            .map(|v| {
                let variant_name = format!("{}{}", name, split_to_pascal_case(&v.name));
                let child = KotlinRenderer {
                    context_name: variant_name.clone(),
                    hoisted: self.hoisted,
                };
                let fields = match &v.field_type {
                    OirType::Struct { fields } => fields
                        .iter()
                        .map(|f| {
                            let grandchild = KotlinRenderer {
                                context_name: format!(
                                    "{}{}",
                                    variant_name,
                                    split_to_pascal_case(&f.name)
                                ),
                                hoisted: self.hoisted,
                            };
                            let (inner_ty, optional) = f.effective_type();
                            let t = render_type(&grandchild, inner_ty, f.enum_values.as_deref());
                            let opt = if optional { "?" } else { "" };
                            format!(
                                "val {}: {}{}",
                                kotlin_identifier(&split_to_camel_case(&f.name)),
                                t,
                                opt
                            )
                        })
                        .collect::<Vec<_>>()
                        .join(", "),
                    other => {
                        format!("val value: {}", render_type(&child, other, None))
                    }
                };
                format!(
                    "    @Serializable\n    @SerialName(\"{}\")\n    data class {}({}) : {}()",
                    escape_double_quoted(&v.name),
                    variant_name,
                    fields,
                    name
                )
            })
            .collect();
        self.hoisted.borrow_mut().push(format!(
            "@Serializable\nsealed class {} {{\n{}\n}}",
            name,
            subtypes.join("\n")
        ));
        name
    }

    // Classifications per `docs/Orbital_Type_Chain_Table.md` (Kotlin column)
    // as amended by Phase 3: Number/Integer→Double and the String-carried
    // domains/date are Widening; Json is Lossy; `type_ref` is the bare
    // hoisted name — Faithful (Phase 3D: every entry's declaration, recursive
    // alias or nominalized anonymous shape, carries the full shape at the top
    // of Entities.kt); `scalar`→JsonPrimitive is Faithful (the closed
    // union, Phase 3); `node` is Widening — the json carrier preserves the
    // pattern-tree STRUCTURE, erasing only the pattern TYPE; sexpr is the
    // PERMANENT deliberate Lossy (config/knob-only, no entity-data
    // consequence — gaps §130 item 4); Struct and Union are Faithful (named
    // hoisted `@Serializable` data class / sealed class).
    fn classify_string(&self, _enum_values: Option<&[String]>) -> TypeMappingClass {
        TypeMappingClass::Faithful
    }
    fn classify_number(&self) -> TypeMappingClass {
        TypeMappingClass::Widening
    }
    fn classify_integer(&self) -> TypeMappingClass {
        TypeMappingClass::Widening
    }
    fn classify_boolean(&self) -> TypeMappingClass {
        TypeMappingClass::Faithful
    }
    fn classify_date(&self) -> TypeMappingClass {
        TypeMappingClass::Widening
    }
    fn classify_json(&self) -> TypeMappingClass {
        TypeMappingClass::Lossy
    }
    fn classify_scalar(&self) -> TypeMappingClass {
        // Pair decision with `scalar`: `JsonPrimitive` is exactly the closed
        // `string | number | boolean | null` union — Faithful (Phase 3).
        TypeMappingClass::Faithful
    }
    fn classify_node(&self) -> TypeMappingClass {
        // The json carrier keeps the pattern tree's STRUCTURE and erases
        // only the pattern TYPE — Widening, not the json arm's Lossy. Typed
        // PatternNode codegen is deferred (rides on named_types).
        TypeMappingClass::Widening
    }
    fn classify_enum_ty(&self, _name: &str, _enum_values: Option<&[String]>) -> TypeMappingClass {
        TypeMappingClass::Faithful
    }
    fn classify_trait_ref(&self) -> TypeMappingClass {
        TypeMappingClass::Widening
    }
    fn classify_sexpr(&self) -> TypeMappingClass {
        // PERMANENT deliberate Lossy — sexprs are config/knob-only, never
        // entity data, so the erasure has no at-rest consequence (§130).
        TypeMappingClass::Lossy
    }
    fn classify_row(&self, _entity: &str) -> TypeMappingClass {
        // Pair decision with `row`: the entity's own data class carries the full shape.
        TypeMappingClass::Faithful
    }
    fn classify_type_ref(&self, _name: &str) -> TypeMappingClass {
        // Pair decision with `type_ref`: the bare name's declaration is
        // hoisted from `named_types` at the top of Entities.kt — Faithful
        // (Phase 3D; same flip TS always had).
        TypeMappingClass::Faithful
    }
    fn classify_array(&self, _inner_ty: &OirType, inner: TypeMappingClass) -> TypeMappingClass {
        inner
    }
    fn classify_map(
        &self,
        _key_ty: &OirType,
        key: TypeMappingClass,
        _value_ty: &OirType,
        value: TypeMappingClass,
    ) -> TypeMappingClass {
        key.meet(value)
    }
    fn classify_optional(&self, _inner_ty: &OirType, inner: TypeMappingClass) -> TypeMappingClass {
        inner
    }
    fn classify_struct_ty(&self, _fields: &[OirField]) -> TypeMappingClass {
        TypeMappingClass::Faithful
    }
    fn classify_union_ty(&self, variants: &[OirField]) -> TypeMappingClass {
        // A named sealed class with @Serializable subtypes carries every
        // variant's structure — Faithful (the pair decision with `union_ty`).
        if variants.is_empty() {
            return self.classify_json();
        }
        TypeMappingClass::Faithful
    }
}

#[cfg(test)]
fn oir_type_to_kotlin(type_: &OirType, context_name: &str) -> String {
    let hoisted = std::cell::RefCell::new(Vec::new());
    render_type(
        &KotlinRenderer {
            context_name: context_name.to_string(),
            hoisted: &hoisted,
        },
        type_,
        None,
    )
}

/// Render an `OirType` and return the field type reference together with the
/// struct/union declarations hoisted along the way (emitted before the
/// entity class).
#[cfg(test)]
fn oir_type_to_kotlin_hoisted(type_: &OirType, context_name: &str) -> (String, Vec<String>) {
    let hoisted = std::cell::RefCell::new(Vec::new());
    let ty = render_type(
        &KotlinRenderer {
            context_name: context_name.to_string(),
            hoisted: &hoisted,
        },
        type_,
        None,
    );
    (ty, hoisted.into_inner())
}

/// Classify an `OirType` under the Kotlin renderer's faithfulness table
/// (Phase-1 metadata; the classification never alters rendered output).
pub fn classify_oir_type(type_: &OirType) -> TypeMappingClass {
    let hoisted = std::cell::RefCell::new(Vec::new());
    classify_type(
        &KotlinRenderer {
            context_name: "Ctx".to_string(),
            hoisted: &hoisted,
        },
        type_,
        None,
    )
}

/// [`TargetLiteralRenderer`] leaves for a field's `default` value. Struct/map/
/// json/node/scalar defaults decline via the trait's default `object() ->
/// None` — no Kotlin literal syntax for an arbitrary object today, so no
/// default is emitted rather than a fabricated `emptyMap()`/`{}` that would
/// silently misrepresent the declared shape.
struct KotlinLiteral;

impl TargetLiteralRenderer for KotlinLiteral {
    type Out = String;

    fn null(&self) -> String {
        "null".to_string()
    }
    fn bool(&self, b: bool) -> String {
        b.to_string()
    }
    fn integer(&self, i: i64) -> String {
        i.to_string()
    }
    /// Always carries a decimal point — Rust's `f64` `Display` prints a whole
    /// number like `1.0` as `"1"`, which Kotlin would then infer as `Int`,
    /// not `Double`.
    fn number(&self, n: f64) -> String {
        let s = n.to_string();
        if s.contains('.') || s.contains('e') || s.contains('E') {
            s
        } else {
            format!("{s}.0")
        }
    }
    fn string(&self, s: &str) -> String {
        format!("\"{}\"", escape_double_quoted(s))
    }
    /// `enum_name` is already the Kotlin enum class name (`type_for_field`
    /// builds it via `enum_class_name`, already `split_to_pascal_case`
    /// pieces); only `member` needs the same transform `generate_entity`
    /// applies to each variant name.
    fn enum_member(&self, enum_name: &str, member: &str) -> String {
        format!("{}.{}", enum_name, split_to_pascal_case(member))
    }
    fn array(&self, items: Vec<String>) -> String {
        format!("listOf({})", items.join(", "))
    }
}

const KOTLIN_KEYWORDS: &[&str] = &[
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",
];

fn kotlin_identifier(s: &str) -> String {
    sanitize_identifier(s, KOTLIN_KEYWORDS)
}

/// Turn a Kotlin identifier suffix like `data-grid` into a valid `SCREAMING_SNAKE`
/// companion-object constant name segment (`DATA_GRID`).
fn kotlin_constant_suffix(pattern_name: &str) -> String {
    pattern_name.replace('-', "_").to_uppercase()
}

/// Kotlin triple-quoted raw strings treat `$` as template interpolation —
/// the only character JSON fixture content can contain that a raw string
/// doesn't pass through literally, so it's the only one escaped.
fn kotlin_raw_string_escape(s: &str) -> String {
    s.replace('$', "${'$'}")
}

/// Width in dp of the one phone content column every pattern snapshot is
/// laid out in: a 390dp device minus 16dp horizontal padding per side.
const PATTERN_SNAPSHOT_COLUMN_WIDTH: u32 = 358;

/// Android-only snapshots beyond the shared one-per-pattern fixtures: (snapshot name, pattern, props JSON whose
/// `src` is the staged-asset marker, asset suffix, asset MIME type, asset body). `image-svg` renders `image` from a local `.svg`
/// file so the Coil SVG decoder (G-PATTERN-SYNC-029) is proven by pixels; the shared `image` fixture stays an
/// iOS-compatible plain string source. Coil 2 has no data-URI fetcher, so the asset is staged to a temp file.
pub const ANDROID_EXTRA_SNAPSHOTS: &[(&str, &str, &str, &str, &str, &str)] = &[(
    "image-svg",
    "image",
    r#"{"type":"image","alt":"svg","src":"__ASSET_URI__","aspect":"16/9","fit":"contain"}"#,
    ".svg",
    "image/svg+xml",
    r##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 60" width="120" height="60"><rect width="120" height="60" fill="#2563eb"/><circle cx="60" cy="30" r="22" fill="#f59e0b"/></svg>"##,
)];

/// Program C wave C4 (`docs/Almadar_Compiler_Gaps.md` §157) render gate: one
/// Robolectric+Roborazzi `@Test` per non-deferred pattern, decoding its
/// fixture (`crate::pack::pack().fixtures`, generated by `almadar-sync
/// pattern-fixtures`) and snapshotting `CorePatternRegistry.get(...)`'s
/// Compose output via `captureRoboImage`. Fixtures are embedded as
/// companion-object raw-string constants (compile-time, same "no reaching
/// outside the crate" contract `embed.rs` uses elsewhere) rather than
/// runtime asset resources — Robolectric runs on the JVM directly, with no
/// APK/asset-bundling step to exercise.
fn generate_pattern_snapshot_test_file() -> GeneratedFile {
    let mut test_methods = String::new();
    let mut constants = String::new();
    for (name, content) in crate::pack::pack().fixtures.iter() {
        let method_name = format!("test_{}", name.replace('-', "_"));
        let const_name = format!("FIXTURE_{}", kotlin_constant_suffix(name));
        let escaped_name = escape_double_quoted(name);
        test_methods.push_str(&format!(
            "    @Test\n    fun {method_name}() {{ assertPatternSnapshot(\"{escaped_name}\", \"{escaped_name}\", {const_name}) }}\n\n"
        ));
        constants.push_str(&format!(
            "        const val {const_name}: String = \"\"\"{}\"\"\"\n",
            kotlin_raw_string_escape(content)
        ));
    }

    for (name, pattern, content, suffix, mime, asset) in ANDROID_EXTRA_SNAPSHOTS {
        let method_name = format!("test_{}", name.replace('-', "_"));
        let const_name = format!("EXTRA_{}", kotlin_constant_suffix(name));
        test_methods.push_str(&format!(
            "    @Test\n    fun {method_name}() {{ assertPatternSnapshot(\"{}\", \"{}\", stageAsset({const_name}, \"{}\", \"{}\", {const_name}_ASSET), awaitImages = true) }}\n\n",
            escape_double_quoted(name),
            escape_double_quoted(pattern),
            escape_double_quoted(suffix),
            escape_double_quoted(mime)
        ));
        constants.push_str(&format!(
            "        const val {const_name}: String = \"\"\"{}\"\"\"\n        const val {const_name}_ASSET: String = \"\"\"{}\"\"\"\n",
            kotlin_raw_string_escape(content),
            kotlin_raw_string_escape(asset)
        ));
    }

    let content = format!(
        r#"package generated

import android.os.Looper
import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import generated.patterns.core.CorePatternRegistry
import generated.patterns.core.LocalOrbitalCanvasTarget
import generated.patterns.core.OrbitalCanvasTarget
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Program C wave C4 render gate — one snapshot per non-deferred pattern,
 * driven by the fixtures `almadar-sync pattern-fixtures` embeds. Generated;
 * never hand-edit — re-run `orbital compile --shell android`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp")
class PatternSnapshotTests {{
    @get:Rule
    val composeTestRule = createComposeRule()

    /** Coil fetches and decodes off the main thread; pump the main looper until the bounded decode window has passed. */
    private fun awaitAsyncImages() {{
        repeat(ASYNC_IMAGE_POLLS) {{
            Thread.sleep(ASYNC_IMAGE_POLL_MS)
            shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
        }}
    }}

    /** Writes an asset to a temp file and points the fixture's `src` at it. Robolectric's MimeTypeMap is empty (a device maps `.svg` itself), so the extension's MIME type is declared here and Coil reads it from the file name. */
    private fun stageAsset(fixtureJson: String, suffix: String, mime: String, body: String): String {{
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypeMapping(suffix.removePrefix("."), mime)
        val file = java.io.File.createTempFile("almadar-snapshot-", suffix)
        file.writeText(body)
        file.deleteOnExit()
        return fixtureJson.replace(ASSET_URI_MARKER, file.toURI().toString())
    }}

    private fun assertPatternSnapshot(snapshotName: String, patternName: String, fixtureJson: String, awaitImages: Boolean = false) {{
        val props = Json.parseToJsonElement(fixtureJson)
        composeTestRule.setContent {{
            // One fixed-width column with intrinsic height (the iOS
            // `.sizeThatFits` equivalent). The 1.dp minimum height keeps a
            // pattern whose pre-C5 body renders nothing capturable:
            // Roborazzi's captureRoboImage throws IllegalStateException on a
            // zero-size node.
            Box(
                modifier = Modifier
                    .testTag(HOST_TAG)
                    .width(COLUMN_WIDTH_DP.dp)
                    .heightIn(min = 1.dp)
                    .background(Color.White)
            ) {{
                // A JVM test has no GPU surface: canvases are drawn once by the same Rust renderer into a bitmap.
                CompositionLocalProvider(LocalOrbitalCanvasTarget provides OrbitalCanvasTarget.Raster) {{
                    CorePatternRegistry.get(patternName)(props, Modifier, {{ _, _ -> }})
                }}
            }}
        }}
        if (awaitImages) awaitAsyncImages()
        composeTestRule.onNodeWithTag(HOST_TAG).captureRoboImage(
            "src/test/snapshots/$snapshotName.png",
            roborazziOptions = RoborazziOptions(
                // 0.01 = at most 1% of pixels may differ; on a component-sized
                // image that is a real change in a small atom, not jitter.
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
            )
        )
    }}

{test_methods}    companion object {{
        /** Phone content column in dp (390dp device minus 16dp padding per side); the one width every pattern snapshot is laid out in. */
        const val COLUMN_WIDTH_DP: Int = {PATTERN_SNAPSHOT_COLUMN_WIDTH}
        const val HOST_TAG: String = "pattern-snapshot-host"
        const val ASSET_URI_MARKER: String = "__ASSET_URI__"
        const val ASYNC_IMAGE_POLLS: Int = 40
        const val ASYNC_IMAGE_POLL_MS: Long = 50
{constants}    }}
}}
"#
    );

    GeneratedFile {
        path: "app/src/test/kotlin/generated/PatternSnapshotTests.kt".to_string(),
        content,
    }
}

fn generate_game_bridge_files() -> Vec<GeneratedFile> {
    const BRIDGE_DIR: &str = "patterns/core";
    vec![
        GeneratedFile {
            path: format!("{BRIDGE_DIR}/OrbitalGameBridge.kt"),
            content: include_str!("renderers/core/OrbitalGameBridge.kt").to_string(),
        },
        GeneratedFile {
            path: format!("{BRIDGE_DIR}/OrbitalGameCanvas.kt"),
            content: include_str!("renderers/core/OrbitalGameCanvas.kt").to_string(),
        },
        // B5 (ledger §159): the in-app kernel bridge, same BRIDGE_DIR/library as the
        // game bridge above (`orbital-ffi` carries both C ABIs in one cdylib).
        GeneratedFile {
            path: format!("{BRIDGE_DIR}/OrbitalKernelBridge.kt"),
            content: include_str!("renderers/core/OrbitalKernelBridge.kt").to_string(),
        },
        // Native sign-in: the host `/login` route's provider client, session binding and screen.
        GeneratedFile {
            path: format!("{BRIDGE_DIR}/OrbitalAuth.kt"),
            content: include_str!("renderers/core/OrbitalAuth.kt").to_string(),
        },
        GeneratedFile {
            path: format!("{BRIDGE_DIR}/OrbitalSignIn.kt"),
            content: include_str!("renderers/core/OrbitalSignIn.kt").to_string(),
        },
        GeneratedFile {
            path: "app/src/test/kotlin/generated/patterns/core/OrbitalAuthTest.kt".to_string(),
            content: include_str!("renderers/core/tests/OrbitalAuthTest.kt").to_string(),
        },
    ]
}

#[cfg(test)]
mod tests {

    use super::*;

    fn awaiting_module() -> OirModule {
        let mut module = OirModule::new("App", "1.0.0");
        let mut browse = orbital_core::ir::OirTrait::new("Browse");
        let mut load = orbital_core::ir::OirTransition {
            from: orbital_core::ir::OirTransitionFrom::Single("idle".into()),
            to: "loading".into(),
            event: "LOAD".into(),
            guard: None,
            effects: vec![],
            await_render: vec![],
        };
        load.await_render.push(orbital_core::ir::AwaitRender {
            trait_name: "Browse".into(),
            slot: "main".into(),
            skeleton: orbital_core::ir::SkeletonNode::Shape {
                shape: orbital_core::ir::SkeletonShape {
                    variant: "list".into(),
                    rows: Some(3),
                    columns: None,
                    fields: None,
                },
            },
        });
        browse.transitions.push(load);
        module.traits.push(browse);
        module
    }

    #[test]
    fn the_toast_slot_renders_its_pattern_and_the_modal_window_exposes_its_tags() {
        let app = generate_app_file(&awaiting_module()).content;
        let toast = &app[app.find("// Toast notification.").unwrap()..];
        let toast = &toast[..toast.find("// Modal dialog").unwrap_or(toast.len())];
        assert!(
            toast.contains(
                "SlotContent(content, Modifier.wrapContentSize(), traitName, viewModel, name)"
            ),
            "the toast draws the pattern the trait rendered (web parity), stamped by SlotContent"
        );
        assert!(
            !toast.contains("Snackbar"),
            "control: no text-only stand-in"
        );
        let modal = &app[app.find("// Modal dialog (centered).").unwrap()..];
        assert!(
            modal.contains("Box(modifier = Modifier.verificationWindowRoot())"),
            "a Dialog is its own window: its test tags need their own resource-id root"
        );
    }

    #[test]
    fn the_bridge_is_idle_only_once_the_page_drew_the_last_fold() {
        let vm = generate_view_model_file(&awaiting_module()).content;
        let fold = vm.find("private suspend fun foldResponse(").unwrap();
        let bump = vm[fold..]
            .find("_paintGeneration.value += 1")
            .expect("every fold bumps the generation");
        let folded = vm[fold..]
            .find("kernel?.setVerifyFolded(_paintGeneration.value)")
            .unwrap();
        assert!(bump < folded);
        let app = generate_app_file(&awaiting_module()).content;
        assert!(app.contains(
            "LaunchedEffect(paintGeneration) {\n        withFrameNanos { }\n        viewModel.reportPainted(paintGeneration)"
        ), "reported only after a frame was drawn");
        assert_eq!(
            app.matches("viewModel.reportPainted(").count(),
            1,
            "control: one reporter per page"
        );
    }

    #[test]
    fn the_await_render_table_embeds_the_compilers_predictions() {
        let file = generate_await_renders_file(&awaiting_module());
        assert_eq!(file.path, "AwaitRenders.kt");
        assert!(
            file.content.contains(r#""requestTrait":"Browse""#),
            "{}",
            file.content
        );
        assert!(
            file.content.contains(r#""from":["idle"]"#),
            "{}",
            file.content
        );
        assert!(file
            .content
            .contains("fun matching(awaiting: KernelAwaitingTrait): List<AwaitRender>"));
        let mut modal = awaiting_module();
        modal.traits[0].transitions[0].await_render[0].slot = "modal".into();
        assert!(
            generate_await_renders_file(&modal)
                .content
                .contains("private val JSON = \"\"\"[]\"\"\""),
            "a portal slot never opens for a skeleton"
        );
    }

    #[test]
    fn the_view_model_shows_a_predicted_skeleton_only_after_the_busy_delay_and_clears_it_with_the_leg(
    ) {
        let vm = generate_view_model_file(&awaiting_module()).content;
        assert!(vm.contains(
            "if (!motion.awaitingSkeleton || legs == 0 || predicted.isEmpty()) return null"
        ));
        assert!(vm.contains("delay(motion.busyDelayMs.toLong())"));
        assert!(vm.contains(
            "val awaitToken = beginAwaiting(result.awaiting.flatMap { AwaitRenders.matching(it) }, result.serverLegs.size)"
        ));
        assert_eq!(
            vm.matches("endAwaitingLeg(awaitToken)").count(),
            3,
            "every leg's outcome ends its share"
        );
        assert!(vm.contains("it !in order && it !in content && it !in EMBEDDED_TRAITS"));
        let app = generate_app_file(&awaiting_module()).content;
        assert!(app.contains("val uiSlots by viewModel.slotsWithAwaiting.collectAsState()"));
        assert!(app.contains("val mainOrder by viewModel.mainOrderWithAwaiting.collectAsState()"));
        assert!(
            !app.contains("viewModel.uiSlots.collectAsState()"),
            "control: every slot read goes through the awaiting view"
        );
    }
    use orbital_core::ir::{
        OirEntity, OirEvent, OirField, OirPage, OirState, OirTrait, OirTraitBinding, OirValue,
    };
    use orbital_core::schema::{RestServiceDefInner, ServiceDefinition, ServiceRef};
    use orbital_core::slots::{slot_role as core_slot_role, SlotRole as CoreSlotRole};
    use std::collections::HashMap;

    fn trait_rendering_main(name: &str, children: &[&str]) -> orbital_core::ir::OirTrait {
        use orbital_core::ir::{OirEffect, OirExpr, OirPattern, OirTransition, OirValue};
        let mut pattern = OirPattern::new("stack");
        pattern.config.insert(
            "children".to_string(),
            OirValue::Array(
                children
                    .iter()
                    .map(|c| OirValue::String(format!("@trait.{c}")))
                    .collect(),
            ),
        );
        let mut t = OirTrait::new(name);
        t.states.push(OirState::initial("idle"));
        let mut tr = OirTransition::new("idle", "idle", "INIT");
        tr.effects.push(OirExpr::effect(OirEffect::RenderUi {
            slot: "main".to_string(),
            pattern,
        }));
        t.transitions.push(tr);
        t
    }

    fn embedding_module() -> OirModule {
        let mut module = OirModule::new("TestApp", "1.0.0");
        module
            .traits
            .push(trait_rendering_main("NoteAppLayout", &["NoteBrowseList"]));
        module
            .traits
            .push(trait_rendering_main("NoteBrowseList", &[]));
        module.traits.push(trait_rendering_main("PeerA", &[]));
        module.traits.push(trait_rendering_main("PeerB", &[]));
        module
    }

    #[test]
    fn embedded_trait_is_excluded_from_the_main_stack_but_kept_for_the_splice() {
        let vm = generate_view_model_file(&embedding_module()).content;
        assert!(vm.contains("EMBEDDED_TRAITS: Set<String> = setOf(\"NoteBrowseList\")"));
        assert!(!vm.contains("setOf(\"NoteAppLayout\""));
        let handlers = generate_effect_handlers_file().content;
        let index = handlers
            .find("traitContent.value = traitContent.value + (sourceTrait to content)")
            .unwrap();
        let guard = handlers
            .find("if (sourceTrait !in embeddedTraits) {")
            .unwrap();
        let append = handlers.find("if (sourceTrait !in mainOrder.value) mainOrder.value = mainOrder.value + sourceTrait").unwrap();
        assert!(
            index < guard && guard < append,
            "traitContent feeds the splice before the main-owner guard"
        );
        assert!(vm.contains(
            "EffectHandlers(_uiSlots, _traitContent, _mainOrder, _lastMainWriter, EMBEDDED_TRAITS,"
        ));
    }

    #[test]
    fn module_without_embedding_has_an_empty_embedded_set_so_peers_stack() {
        let mut module = OirModule::new("TestApp", "1.0.0");
        module.traits.push(trait_rendering_main("PeerA", &[]));
        module.traits.push(trait_rendering_main("PeerB", &[]));
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains("EMBEDDED_TRAITS: Set<String> = emptySet()"));
    }

    #[test]
    fn host_splice_still_reads_trait_content_for_the_embedded_trait() {
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(vm.contains("val refContent = traitContent[ref] ?: return JsonNull"));
    }

    #[test]
    fn test_backend_name() {
        let backend = AndroidBackend::new();
        assert_eq!(backend.name(), "android");
    }

    fn entities_file(module: &OirModule) -> String {
        AndroidBackend::new()
            .generate(module)
            .files
            .into_iter()
            .find(|f| f.path == "Entities.kt")
            .map(|f| f.content)
            .unwrap_or_default()
    }

    #[test]
    fn test_entities_declared_once_when_module_carries_same_named_clones() {
        let mut module = OirModule::new("TestApp", "1.0.0");
        module.entities.push(OirEntity::new("User"));
        module.entities.push(OirEntity::new("User"));
        assert_eq!(
            entities_file(&module).matches("data class User(").count(),
            1
        );
    }

    #[test]
    fn test_entities_keeps_distinct_names() {
        let mut module = OirModule::new("TestApp", "1.0.0");
        module.entities.push(OirEntity::new("User"));
        module.entities.push(OirEntity::new("UserProfile"));
        let content = entities_file(&module);
        assert_eq!(content.matches("data class User(").count(), 1);
        assert!(content.contains("data class UserProfile"));
    }

    #[test]
    fn test_generate_produces_files() {
        let backend = AndroidBackend::new();
        let mut module = OirModule::new("TestApp", "1.0.0");
        let mut entity = OirEntity::new("User");
        entity.fields.push(OirField::new("email", OirType::String));
        module.entities.push(entity);

        let output = backend.generate(&module);
        let paths: Vec<_> = output.files.iter().map(|f| f.path.as_str()).collect();
        assert!(paths.contains(&"Entities.kt"));
        assert!(paths.contains(&"Services.kt"));
        assert!(paths.contains(&"ApiClient.kt"));
        assert!(paths.contains(&"OrbitalViewModel.kt"));
        assert!(paths.contains(&"EffectHandlers.kt"));
        assert!(paths.contains(&"patterns/PatternRenderer.kt"));
        assert!(paths.contains(&"patterns/core/FallbackRenderer.kt"));
        assert!(paths.contains(&"patterns/core/RenderersCommon.kt"));
        assert!(paths.contains(&"patterns/core/CorePatternRegistry.kt"));
        assert!(paths.contains(&"patterns/core/atoms/button.kt"));
        assert!(paths.contains(&"patterns/core/atoms/typography.kt"));
        assert!(paths.contains(&"patterns/core/molecules/page_header.kt"));
        assert!(paths.contains(&"App.kt"));
        assert!(paths.contains(&"GeneratedActivity.kt"));
    }

    #[test]
    fn test_generate_omits_resolved_schema_without_one() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        assert!(!output
            .files
            .iter()
            .any(|f| f.path == "app/src/main/assets/resolved_schema.json"));
    }

    #[test]
    fn test_generate_with_resolved_schema_emits_resource_file() {
        let schema = orbital_core::schema::parse_schema(r#"{"name":"Test","orbitals":[]}"#)
            .expect("minimal schema parses");
        let backend = AndroidBackend::new().with_resolved_schema(schema);
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let resource = output
            .files
            .iter()
            .find(|f| f.path == "app/src/main/assets/resolved_schema.json")
            .expect("resolved_schema.json not emitted");
        let round_tripped: orbital_core::OrbitalSchema =
            serde_json::from_str(&resource.content).expect("emitted content is valid JSON");
        assert_eq!(round_tripped.name, "Test");
    }

    #[test]
    fn test_registry_file_contains_fallback_and_known_patterns() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let registry = output
            .files
            .iter()
            .find(|f| f.path == "patterns/core/CorePatternRegistry.kt")
            .unwrap();
        assert!(registry.content.contains("object CorePatternRegistry"));
        assert!(registry.content.contains("FallbackRenderer"));
        assert!(registry.content.contains("\"button\" to"));
        assert!(registry.content.contains("\"typography\" to"));
        assert!(registry.content.contains("\"card\" to"));
    }

    /// C0 (§157): a release build must never show the fallback placeholder to
    /// a real user, and a debug build must log the unsupported pattern once
    /// (not once per recomposition). `BuildConfig.DEBUG` is not reachable from
    /// the fixed `generated` package (the app's namespace is a runtime
    /// template value), so debuggability comes off the manifest flag instead.
    #[test]
    fn fallback_renderer_is_debug_only_and_logs_once() {
        let file = generate_fallback_renderer_file();
        assert!(file.content.contains("ApplicationInfo.FLAG_DEBUGGABLE"));
        assert!(file.content.contains("if (!isDebug) return"));
        assert!(file
            .content
            .contains("loggedFallbackPatterns.add(patternName)"));
        assert!(file
            .content
            .contains("Log.w(\"Orbital\", \"Unsupported pattern: $patternName\")"));
    }

    #[test]
    fn test_native_renderer_files_generated() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let paths: Vec<_> = output.files.iter().map(|f| f.path.as_str()).collect();
        assert!(paths.contains(&"patterns/core/atoms/button.kt"));
        assert!(paths.contains(&"patterns/core/atoms/typography.kt"));
        assert!(paths.contains(&"patterns/core/organisms/stat_card.kt"));
        assert!(paths.contains(&"patterns/core/molecules/page_header.kt"));
        assert!(paths.contains(&"patterns/core/molecules/flex.kt"));
    }

    #[test]
    fn test_registry_uses_native_renderers() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let registry = output
            .files
            .iter()
            .find(|f| f.path == "patterns/core/CorePatternRegistry.kt")
            .unwrap();
        assert!(registry.content.contains("ButtonRenderer("));
        assert!(registry.content.contains("TextRenderer("));
        assert!(registry.content.contains("StatCardRenderer("));
        assert!(registry.content.contains("PageHeaderRenderer("));
        // All non-deferred patterns now have native renderers; FallbackRenderer is only the default branch.
        assert!(registry.content.contains("FallbackRenderer(patternName"));
    }

    #[test]
    fn test_gpu_bridge_files_are_generated() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let paths: Vec<_> = output.files.iter().map(|f| f.path.as_str()).collect();
        assert!(
            paths.contains(&"patterns/core/OrbitalGameBridge.kt"),
            "OrbitalGameBridge.kt must be generated"
        );
        assert!(
            paths.contains(&"patterns/core/OrbitalGameCanvas.kt"),
            "OrbitalGameCanvas.kt must be generated"
        );
        let bridge = output
            .files
            .iter()
            .find(|f| f.path == "patterns/core/OrbitalGameBridge.kt")
            .unwrap();
        assert!(bridge.content.contains("external fun nativeInit"));
        assert!(bridge.content.contains("external fun nativeRender"));
        assert!(bridge.content.contains("OrbitalGameSurfaceView"));
        assert!(bridge
            .content
            .contains("System.loadLibrary(\"orbital_ffi\")"));
    }

    #[test]
    fn test_registry_routes_canvas_patterns_to_gpu_renderer() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let registry = output
            .files
            .iter()
            .find(|f| f.path == "patterns/core/CorePatternRegistry.kt")
            .unwrap();
        // The three canvas aliases must all route to GameCanvas2dRenderer (GPU path).
        assert!(
            registry.content.contains("\"canvas\" to"),
            "canvas pattern must be routed in CorePatternRegistry"
        );
        assert!(
            registry.content.contains("\"game-canvas\" to"),
            "game-canvas pattern must be routed in CorePatternRegistry"
        );
        assert!(
            registry.content.contains("\"game-canvas-2d\" to"),
            "game-canvas-2d pattern must be routed in CorePatternRegistry"
        );
        assert!(
            registry.content.contains("GameCanvas2dRenderer("),
            "canvas patterns must delegate to GameCanvas2dRenderer"
        );
    }

    #[test]
    fn test_entity_with_enum_and_defaults() {
        let mut entity = OirEntity::new("Task");
        entity.fields.push(OirField::new("title", OirType::String));
        entity.fields.push({
            let mut f = OirField::new("priority", OirType::String);
            f.enum_values = Some(vec!["low".to_string(), "high".to_string()]);
            f
        });
        entity.fields.push({
            let mut f = OirField::new("completed", OirType::Boolean);
            f.default = Some(OirValue::Bool(false));
            f
        });
        entity.fields.push({
            let mut f = OirField::new(
                "tags",
                OirType::Array {
                    inner: Box::new(OirType::String),
                },
            );
            f.required = false;
            f
        });

        let content = generate_entity(&entity);
        assert!(content.contains("enum class TaskPriority"));
        assert!(content.contains("@SerialName(\"low\")\n    Low"));
        assert!(content.contains("@SerialName(\"high\")\n    High"));
        assert!(content.contains("val completed: Boolean = false"));
        assert!(content.contains("val tags: List<String>? = null"));
    }

    /// `OrbitalApi` speaks the ONE canonical route (`ClientKernel`/
    /// `http.rs::send_request`'s own `/api/orbitals/{orbital}/events`), never
    /// the retired per-trait `/api/<kebab-trait>/events` path.
    #[test]
    fn api_client_posts_canonical_orbital_route() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_api_client_file(&module).content;
        assert!(content.contains("class OrbitalApi(baseUrl: String)"));
        assert!(content.contains(
            "suspend fun postServerLeg(orbital: String, request: KernelServerLeg): Result<String>"
        ));
        assert!(content.contains(r#"url("$baseUrl/api/orbitals/$orbital/events")"#));
        assert!(content.contains("fun openLivePush(onItem: (LiveBroadcastItem) -> Unit)"));
        assert!(content.contains(r#"Request.Builder().url("$baseUrl/api/events")"#));
        assert!(!content.contains("/api/$path/events"));
        assert!(!content.contains("traitPaths"));
    }

    #[test]
    fn test_view_model_generated() {
        let _backend = AndroidBackend::new();
        let mut module = OirModule::new("TestApp", "1.0.0");
        let mut page = OirPage::new("Home", "/");
        page.traits.push(OirTraitBinding::new("HomeLifecycle"));
        module.pages.push(page);

        let content = generate_view_model_file(&module).content;
        assert!(content.contains("class OrbitalViewModel"));
        assert!(content.contains("fun initTrait"));
        assert!(content.contains("fun sendEvent"));
        assert!(content.contains("BASE_URL"));
    }

    /// `OrbitalViewModel` owns exactly one `OrbitalKernel` handle (never a
    /// second parallel dispatch path) and its `sendEvent` drives dispatch
    /// through it, mirroring `ClientKernel::dispatch`'s mode circuit —
    /// every mode posts the server leg the kernel produced (a `local`
    /// seed only has one when its cascade reached a server-bound trait).
    #[test]
    fn view_model_owns_kernel_handle() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains("private var kernel: OrbitalKernel? = null"));
        assert!(content.contains("val result = kernelCalls.withLock { withContext(Dispatchers.Default) { k.dispatch(trait, null, event, payloadJson) } }"));
        assert!(content.contains("KernelDispatchMode.hybridClientOnly ->"));
        assert!(content.contains("KernelDispatchMode.runtimeOptimistic ->"));
        assert!(content.contains("KernelDispatchMode.persistedAwaited ->"));
    }

    /// `resolved_schema.json` is read off the main thread (`Dispatchers.IO`)
    /// before `OrbitalKernel.create` runs, and the kernel's own client id is
    /// a per-launch UUID (same identity today's live-broadcast dedup key
    /// already uses).
    #[test]
    fn view_model_loads_resolved_schema_off_main_thread() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains("viewModelScope.launch(Dispatchers.IO) {"));
        assert!(
            content.contains(r#"getApplication<Application>().assets.open(RESOLVED_SCHEMA_ASSET)"#)
        );
        assert!(content.contains("val created = OrbitalKernel.create(schemaJson, clientId)"));
        assert!(content.contains("private val clientId = UUID.randomUUID().toString()"));
        assert!(content.contains("withContext(Dispatchers.Main) {"));
    }

    /// A dispatch that produces no server leg (a plain local `set`, or a
    /// cascade with no server-only effect) must never call `postServerLeg` —
    /// `sendEvent` only posts the legs `result.serverLegs` carries.
    #[test]
    fn view_model_posts_server_leg_only_when_present() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains("for (routed in result.serverLegs) {"));
    }

    /// `hybridClientOnly` (a `local` seed) posts only the leg the kernel produced
    /// for its cascade's server-bound traits; a `local` seed with no leg posts
    /// nothing (the empty `serverLegs` loop).
    #[test]
    fn view_model_hybrid_local_posts_only_its_cascade_leg() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        let start = content
            .find("KernelDispatchMode.hybridClientOnly -> {")
            .expect("missing hybridClientOnly arm");
        let end = content[start..]
            .find("KernelDispatchMode.runtimeOptimistic ->")
            .expect("missing runtimeOptimistic arm");
        let arm = &content[start..start + end];
        assert!(arm.contains("for (routed in result.serverLegs) {"));
        assert!(arm.contains("postServerLegAndFold(trait, routed.orbital, routed.leg, result.alreadyDelivered, result.snapshot, result.seed.event)"));
        assert!(arm.contains("posting cascade server leg for local seed"));
    }

    /// A rejected/undecodable/failed server leg restores the pre-dispatch
    /// snapshot (`kernel.restore`) and folds the restored state back into
    /// `_states` — `RuntimeOptimistic`'s rollback path
    /// (`ClientKernel::dispatch`'s own restore-on-failure branches).
    #[test]
    fn view_model_restores_on_server_error() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains("suspend fun restore(error: String) {"));
        assert!(content.contains("k.restore(requestTrait, null, snapshot)"));
        assert!(
            content.contains("_states.value = _states.value + (requestTrait to snapshot.state)")
        );
        assert!(content.contains(
            ".onFailure { throwable ->\n                restore(throwable.message ?: throwable.toString())"
        ));
    }

    /// Applying a server response dedupes against `alreadyDelivered` (the
    /// pairs `kernel.dispatch` already realized locally) — the rewrite of
    /// the retired `emittedEvents` re-dispatch loop.
    #[test]
    fn view_model_applies_server_response_with_echo_dedup() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains("applied.applied.forEach { pair ->"));
        assert!(!content.contains("response.emittedEvents?.forEach"));
        assert!(!content.contains("sendEvent(trait, ev.event, ev.payload)"));
    }

    /// `launch` per event runs dispatches concurrently on the Default pool, and the
    /// kernel's server-leg collector is per handle: one dispatch drained another's
    /// persist, so a `persistedAwaited` INIT reported no server leg (G-ORB-163).
    #[test]
    fn view_model_serializes_every_kernel_call_that_drives_a_cascade() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains("private val kernelCalls = Mutex()"));
        assert!(vm.contains(
            "kernelCalls.withLock { withContext(Dispatchers.Default) { k.dispatch(trait, null, event, payloadJson) } }"
        ));
        assert!(
            vm.contains("kernelCalls.withLock { k.applyServerResponse(raw, alreadyDelivered) }")
        );
        assert!(vm.contains("kernelCalls.withLock { k.restore(requestTrait, null, snapshot) }"));
        assert!(vm.contains(
            "kernelCalls.withLock { k.abandonServerLeg(leg.targetTrait ?: requestTrait, seedEvent, error) }"
        ));
        // Control: the unlocked dispatch spelling is gone.
        assert!(!vm.contains("val result = withContext(Dispatchers.Default) { k.dispatch("));
        assert!(!vm.contains("= k.applyServerResponse("));
    }

    /// An unresolved `@trait.X` scalar `children` ref splices to `JsonNull`, which
    /// `JsonElement.children()` reads as a primitive child and renders as a
    /// `{type:"null"}` node ("Unsupported pattern: null").
    #[test]
    fn splice_drops_a_scalar_children_ref_that_resolves_to_null() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        let start = vm.find("private fun spliceChildren(").unwrap();
        let body = &vm[start..start + vm[start..].find("private fun spliceProp(").unwrap()];
        assert!(body.contains("takeIf { it !is JsonNull } ?: JsonArray(emptyList())"));
        // Control: the array path still drops an unresolved ref and keeps resolved ones.
        assert!(body.contains("val refContent = traitContent[ref] ?: continue"));
        assert!(body.contains("out.add(spliceContent(refContent, traitContent, ref, seen + ref))"));
    }

    /// `INITIAL_STATES` must carry a declared initial state for every trait —
    /// seeded before the kernel's own snapshot lands.
    #[test]
    fn view_model_initial_states_table_has_every_trait() {
        let mut module = OirModule::new("TestApp", "1.0.0");
        let mut with_init = OirTrait::new("NoteAppLayout");
        with_init.states.push(OirState::new("editing"));
        with_init.states.push(OirState::initial("browsing"));
        with_init.events.push(OirEvent::new("INIT"));
        module.traits.push(with_init);
        let mut without_init = OirTrait::new("FavoritesLayout");
        without_init.states.push(OirState::initial("idle"));
        module.traits.push(without_init);

        let content = generate_view_model_file(&module).content;
        assert!(content.contains(r#""NoteAppLayout" to "browsing""#));
        assert!(content.contains(r#""FavoritesLayout" to "idle""#));
        // Only the trait that declares INIT auto-dispatches it.
        assert!(content.contains(r#"TRAITS_WITH_INIT: Set<String> = setOf("NoteAppLayout")"#));
    }

    /// With no declared REST service, the default `BASE_URL` must be the
    /// Android emulator's host-loopback alias (`10.0.2.2`), never
    /// `localhost` — the emulator's own network namespace makes
    /// `localhost` resolve to the emulator itself, not the host machine
    /// running the dev server.
    #[test]
    fn view_model_default_base_url_is_emulator_alias() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains(r#"BASE_URL = "http://10.0.2.2:3030""#));
    }

    /// The server-leg route addresses the orbital the kernel reports on the
    /// dispatch wire (`result.orbital`); no orbital name is baked.
    #[test]
    fn server_leg_route_comes_from_the_dispatch_wire_orbital() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        assert_eq!(
            vm.matches("postServerLegAndFold(trait, routed.orbital, routed.leg,")
                .count(),
            3,
            "the local-seed, optimistic and persisted legs all post to the wire orbital"
        );
        assert!(vm.contains("api.postServerLeg(orbital, leg)"));
        // Control: no baked orbital constant survives.
        assert!(!vm.contains("ORBITAL_NAME"));
        let bridge = include_str!("renderers/core/OrbitalKernelBridge.kt");
        assert!(bridge.contains("orbital = wire.orbital,"));
    }

    /// `initAllTraits` must fire `INIT` for every `TRAITS_WITH_INIT` member once,
    /// at app mount, and await the kernel first — the per-page `initTrait`
    /// must NOT also dispatch it (a second dispatch would double-init a
    /// page's own trait every navigation).
    #[test]
    fn view_model_has_init_all_traits_and_page_mount_only_seeds() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains("fun initAllTraits"));
        assert!(content.contains("kernelReady.await()"));
        assert!(content.contains("TRAITS_WITH_INIT.forEach"));
        assert!(
            !content.contains("if (TRAITS_WITH_INIT.contains(trait))"),
            "initTrait must no longer conditionally dispatch INIT itself"
        );
    }

    /// The mount is declared before the first `INIT` fires (mirrors
    /// `ClientCore::init_all_traits_blocking`), so a seed one trait's INIT
    /// emits is held until its receiver's own INIT instead of being dropped.
    #[test]
    fn init_all_traits_declares_the_mount_before_any_init() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        let remount = content
            .find("kernel?.remount(TRAITS_WITH_INIT)")
            .expect("initAllTraits must declare the mount");
        let first_init = content.find(r#"sendEvent(trait, "INIT")"#).unwrap();
        assert!(remount < first_init, "remount must precede the INIT loop");
    }

    /// The bridge is told the host is mounting before the first `INIT` and mounted after the last,
    /// so `/idle` cannot answer idle between the app launching and its INIT legs being counted.
    #[test]
    fn init_all_traits_brackets_the_init_loop_with_verify_ready() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        let not_ready = content
            .find("kernel?.setVerifyReady(false)")
            .expect("ready=false");
        let remount = content.find("kernel?.remount(TRAITS_WITH_INIT)").unwrap();
        let first_init = content.find(r#"sendEvent(trait, "INIT")"#).unwrap();
        let ready = content
            .find("kernel?.setVerifyReady(true)")
            .expect("ready=true");
        assert!(not_ready < remount && remount < first_init && first_init < ready);
        assert_eq!(
            content.matches("setVerifyReady(").count(),
            4,
            "only the INIT brackets (app mount + a gated page mount) flip readiness"
        );
        let gated = content.find("fun mountGatedPage").unwrap();
        let tail = &content[gated..];
        assert!(
            tail.find("setVerifyReady(false)").unwrap()
                < tail.find("kernel?.mount(pending)").unwrap()
        );
        assert!(
            tail.find(r#"sendEvent(trait, "INIT")"#).unwrap()
                < tail.find("setVerifyReady(true)").unwrap()
        );
    }

    /// Every `clientEffects` entry must be routed through the shared
    /// `EffectHandlers` instance, attributed by the paired
    /// `clientEffectsByTrait[i].traitName` (falling back to the request
    /// trait) — never the retired content-embedded `sourceTrait`.
    #[test]
    fn view_model_routes_client_effects_through_effect_handlers() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains("private val effectHandlers = EffectHandlers("));
        assert!(content.contains("val paired = (byTrait.getOrNull(i) as? JsonObject)?.get(\"traitName\")?.jsonPrimitive?.contentOrNull"));
        assert!(content.contains("effectHandlers.apply(effect, paired ?: requestTrait)"));
        // The "emit" client effect still routes through onEmit == sendEvent
        // (CLIENT_EFFECT_KINDS coverage; the kernel never actually produces it).
        assert!(content.contains("trait, event, payload -> sendEvent(trait, event, payload)"));
    }

    /// A transport/dispatch failure (network error, blocked cleartext,
    /// timeout, an unready kernel, …) must never be silent: every failure
    /// path logs via `android.util.Log.e` and surfaces through
    /// `_notifications` — the same channel a `success: false` response uses.
    #[test]
    fn view_model_logs_and_surfaces_send_event_failures() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains(r#"android.util.Log.e("Orbital", "dispatch failed: trait=$trait event=$event error=$err")"#));
        assert!(content.contains(r#"_notifications.emit("Event failed ($trait/$event): $err")"#));
        assert!(content.contains(".onFailure { throwable ->"));
        assert!(content.contains(r#"android.util.Log.e("Orbital", "postServerLeg failed: trait=$requestTrait event=${leg.event}", throwable)"#));
    }

    /// `traitContent` indexes render-ui content by its SOURCE trait
    /// (`sourceTrait` on the wire) — the `@trait.X` resolution index.
    #[test]
    fn view_model_exposes_trait_content_flow() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains(
            "private val _traitContent = MutableStateFlow<Map<String, JsonElement>>(emptyMap())"
        ));
        assert!(content.contains(
            "val traitContent: StateFlow<Map<String, JsonElement>> = _traitContent.asStateFlow()"
        ));
    }

    /// The splice function is the `@trait.X`[.slot] token contract: prefix
    /// `@trait.` then the name up to the next `.`, an unresolved children-array
    /// ref dropped, an unresolved prop ref nulled, cycle-guarded.
    #[test]
    fn view_model_defines_splice_content_with_trait_token_contract() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains("fun traitRefName(s: String): String?"));
        assert!(content.contains(r#"if (!s.startsWith("@trait."))"#));
        assert!(content.contains("fun spliceContent("));
        assert!(content.contains("fun spliceChildren("));
        assert!(content.contains("fun spliceProp("));
        assert!(
            content.contains("if (ref in seen)"),
            "cycle guard must be present"
        );
    }

    /// Live-push items are fed through the kernel exactly like a local
    /// dispatch (one fold path), and a broadcast this SAME client
    /// originated is ignored — never re-applied to its own origin.
    #[test]
    fn live_push_subscriber_feeds_kernel_and_ignores_own_client_id() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains("api.openLivePush { item -> onLivePushItem(item) }"));
        assert!(content.contains("private fun onLivePushItem(item: LiveBroadcastItem) {"));
        assert!(content.contains("if (item.originClientId == clientId) return"));
        assert!(content.contains("k.dispatch(trait, null, item.event, payloadJson)"));
        assert!(content.contains("foldResponse(trait, result.response)"));
    }

    /// The theme comes from the kernel's own snapshot (`OrbitalKernel
    /// .snapshot().theme`), not a device dark-mode query — `themeTokens`/
    /// `themeMode` are Compose-observable `var`s seeded from the baked
    /// fallback until that snapshot lands.
    #[test]
    fn theme_comes_from_kernel_snapshot() {
        let module = OirModule::new("TestApp", "1.0.0");
        let content = generate_view_model_file(&module).content;
        assert!(content.contains(
            "var themeTokens: ThemeTokensJson by mutableStateOf(ThemeTokensJson.fallback)"
        ));
        assert!(content.contains("var themeMode: ColorMode by mutableStateOf(ColorMode.Light)"));
        assert!(content.contains("val snap = created.snapshot()"));
        assert!(content.contains("snap.theme?.let { applyResolvedTheme(it) }"));
        assert!(content.contains("themeTokens = theme.tokens"));
        assert!(content.contains(
            r#"themeMode = if (theme.mode == "dark") ColorMode.Dark else ColorMode.Light"#
        ));
        assert!(!content.contains("isSystemInDarkTheme"));
    }

    #[test]
    fn test_app_generates_navigation() {
        let backend = AndroidBackend::new();
        let mut module = OirModule::new("TestApp", "1.0.0");
        let mut page = OirPage::new("Home", "/");
        page.traits.push(OirTraitBinding::new("HomeLifecycle"));
        module.pages.push(page);

        let output = backend.generate(&module);
        let paths: Vec<_> = output.files.iter().map(|f| f.path.as_str()).collect();
        assert!(paths.contains(&"OrbitalViewModel.kt"));
        assert!(paths.contains(&"EffectHandlers.kt"));

        let app = output.files.iter().find(|f| f.path == "App.kt").unwrap();
        assert!(app.content.contains("NavHost"));
        assert!(app.content.contains("composable(\"/\")"));
        assert!(app.content.contains("GeneratedPage"));
        assert!(app
            .content
            .contains("AlmadarTheme(tokens = viewModel.themeTokens, mode ="));
        assert!(!app.content.contains("MaterialTheme {"));
    }

    /// `initAllTraits` fires once, at `GeneratedApp` mount — not per page —
    /// and the debug `Text("State: …")` scaffolding line is gone (real
    /// content now renders in its place).
    #[test]
    fn app_calls_init_all_traits_once_and_drops_debug_state_line() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let app = output.files.iter().find(|f| f.path == "App.kt").unwrap();
        assert_eq!(
            app.content.matches("viewModel.initAllTraits()").count(),
            1,
            "initAllTraits must be called exactly once"
        );
        assert!(
            !app.content.contains("Text(\"State: "),
            "the debug state line must be removed from GeneratedPage/PageBody"
        );
    }

    /// The page body and every overlay slot resolve `@trait.X` composition via
    /// `spliceContent` before reaching `SlotContent`/`CorePatternRegistry` — a
    /// raw `traitContent[traitName]`/slot value must never reach the registry.
    #[test]
    fn app_splices_main_and_overlay_content_before_rendering() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let app = output.files.iter().find(|f| f.path == "App.kt").unwrap();
        assert!(app
            .content
            .contains("val traitContentMap by viewModel.contentWithAwaiting.collectAsState()"));
        assert!(app.content.contains(
            "traitContentMap[name]?.let { name to spliceContent(it, traitContentMap, name) }"
        ));
        // Every classified overlay slot is spliced too (not just main).
        assert!(app
            .content
            .contains("?.let { it.key to spliceContent(it.value, traitContentMap, traitName) }"));
        assert!(app
            .content
            .contains(".map { it.key to spliceContent(it.value, traitContentMap, traitName) }"));
    }

    /// `SlotContent`'s `onEvent` reads the `eventEnvelope` every renderer body
    /// passes as its payload (`sourceTrait`/`actionPayload`) — the native
    /// mirror of `helpers.rs::queue_event`'s per-subtree source.
    #[test]
    fn app_slot_content_routes_events_by_envelope_source_trait() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let app = output.files.iter().find(|f| f.path == "App.kt").unwrap();
        assert!(app.content.contains("obj?.get(\"sourceTrait\")"));
        assert!(app.content.contains("obj?.get(\"actionPayload\")"));
        assert!(app
            .content
            .contains("viewModel.sendEvent(source, event, payload = payload)"));
    }

    /// `SlotContent.onEvent` resolves a control's dispatch trait straight
    /// from the clicked node itself (`sourceTrait`/`traitName` fallback) —
    /// there is no separate routing-by-`sourceTrait` lookup table to delete
    /// on Android (unlike the iOS twin's per-page dispatch table); this
    /// pins that absence so a future edit can't reintroduce one.
    #[test]
    fn app_no_longer_routes_by_source_trait() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let app = output.files.iter().find(|f| f.path == "App.kt").unwrap();
        assert!(!app.content.contains("routeTable"));
        assert!(!app.content.contains("sourceTraitTable"));
        // Exactly one dispatch call site in App.kt — the SlotContent onEvent
        // closure — no page-level indirection routing to it.
        assert_eq!(app.content.matches("viewModel.sendEvent(").count(), 1);
    }

    #[test]
    fn test_service_config_generation() {
        let mut headers = HashMap::new();
        headers.insert("Authorization".to_string(), "Bearer token".to_string());

        let service_ref = ServiceRef::Inline(ServiceDefinition::Rest(RestServiceDefInner {
            name: "backend".to_string(),
            description: Some("Backend API".to_string()),
            base_url: "https://api.example.com".to_string(),
            headers: Some(headers),
            auth: None,
            timeout: Some(5000),
        }));

        let content = generate_services_file(&[service_ref]).content;
        assert!(content.contains("data class BackendRestConfig"));
        assert!(content.contains("val baseUrl: String = \"https://api.example.com\""));
        assert!(content.contains("\"Authorization\" to \"Bearer token\""));
    }

    #[test]
    fn test_reference_field_type() {
        // A declared relation field stores the referenced row's id, not the
        // entity's shape — the core `TargetTypeRenderer::reference` default
        // (`self.string(None)`) applies now that the Kotlin backend no
        // longer overrides it to the entity's own class name.
        let mut entity = OirEntity::new("Order");
        entity.fields.push(OirField::new(
            "customer",
            OirType::Reference {
                entity: "Customer".to_string(),
            },
        ));
        entity.fields.push(OirField::optional(
            "backupCustomer",
            OirType::Reference {
                entity: "Customer".to_string(),
            },
        ));

        let content = generate_entity(&entity);
        assert!(content.contains("val customer: String"));
        assert!(content.contains("val backupCustomer: String? = null"));
        assert!(
            !content.contains("val backupCustomer: String?? = null"),
            "an optional reference must render exactly one `?`"
        );
    }

    #[test]
    fn an_entity_named_like_a_kotlin_type_is_declared_and_referenced_under_one_renamed_class() {
        let mut result = OirEntity::new("Result");
        let mut kind = OirField::new("kind", OirType::String);
        kind.enum_values = Some(vec!["ok".to_string()]);
        result.fields.push(kind);
        let content = generate_entity(&result);
        assert!(content.contains("class ResultEntity("), "{content}");
        assert!(content.contains("enum class ResultEntityKind"), "{content}");
        let mut run = OirEntity::new("Run");
        run.fields.push(OirField::new(
            "results",
            OirType::array(OirType::row("Result")),
        ));
        assert!(
            generate_entity(&run).contains("List<ResultEntity>"),
            "{}",
            generate_entity(&run)
        );
    }

    #[test]
    fn control_an_entity_name_no_kotlin_type_uses_keeps_its_name() {
        let content = generate_entity(&OirEntity::new("Note"));
        assert!(content.contains("class Note("), "{content}");
        assert!(!content.contains("NoteEntity"), "{content}");
    }

    #[test]
    fn a_row_field_is_the_entity_class_where_a_reference_field_is_an_id() {
        let mut entity = OirEntity::new("Note");
        entity.fields.push(OirField::new(
            "revisions",
            OirType::array(OirType::row("NoteRevision")),
        ));
        entity.fields.push(OirField::new(
            "tagIds",
            OirType::array(OirType::reference("NoteTag")),
        ));
        let content = generate_entity(&entity);
        assert!(
            content.contains("val revisions: List<NoteRevision>"),
            "{content}"
        );
        assert!(content.contains("val tagIds: List<String>"), "{content}");
        assert_eq!(
            classify_oir_type(&OirType::row("NoteRevision")),
            TypeMappingClass::Faithful
        );
    }

    #[test]
    fn enum_default_renders_the_declared_entry_name() {
        let mut entity = OirEntity::new("Task");
        let mut f = OirField::new("priority", OirType::String);
        f.enum_values = Some(vec!["low".to_string(), "high".to_string()]);
        f.default = Some(OirValue::String("high".to_string()));
        entity.fields.push(f);

        let content = generate_entity(&entity);
        assert!(content.contains("val priority: TaskPriority = TaskPriority.High"));
    }

    #[test]
    fn number_default_always_carries_a_decimal_point() {
        let mut entity = OirEntity::new("Product");
        let mut f = OirField::new("price", OirType::Number);
        f.default = Some(OirValue::Number(1.0));
        entity.fields.push(f);

        let content = generate_entity(&entity);
        assert!(content.contains("val price: Double = 1.0"));
    }

    #[test]
    fn nested_struct_optional_child_renders_exactly_one_question_mark() {
        // `nick_name`: `required: false` flag only, no `Optional` wrapper.
        // `note`: `OirField::optional` — both the flag AND the `Optional`
        // wrapper. Before the `effective_type` fix, the wrapper's own `?`
        // (from `render_type`'s `Optional` handling) combined with
        // `struct_ty`'s own `required`-based `?` into `String??`.
        let mut nick_name = OirField::new("nick_name", OirType::String);
        nick_name.required = false;
        let note = OirField::optional("note", OirType::String);
        let struct_ty = OirType::Struct {
            fields: vec![OirField::new("full_name", OirType::String), nick_name, note],
        };

        let (_, hoisted) = oir_type_to_kotlin_hoisted(&struct_ty, "Ctx");
        let decl = &hoisted[0];
        assert!(decl.contains("val nickName: String?"));
        assert!(decl.contains("val note: String?"));
        assert!(
            !decl.contains("??"),
            "optional struct field must never double up: {decl}"
        );
    }

    #[test]
    fn literal_type_mismatch_emits_a_marker_instead_of_panicking() {
        // A relation field defaulting to an object — the exact shape std
        // organisms currently declare for `seedRow : <Entity> = {}` (a `.lolo`
        // fix, not a codegen fix; see the task's own note). `Reference` isn't
        // an object-shaped arm in `render_default`, so this is a genuine
        // `LiteralMismatch`.
        let mut entity = OirEntity::new("Note");
        let mut f = OirField::new(
            "seedRow",
            OirType::Reference {
                entity: "Note".to_string(),
            },
        );
        let mut fields = orbital_core::ir::OirObject::new();
        fields.insert("x".to_string(), OirValue::Number(1.0));
        f.default = Some(OirValue::Object(fields));
        entity.fields.push(f);

        let content = generate_entity(&entity);
        assert!(content.contains(orbital_core::ir::LITERAL_TYPE_MISMATCH_MARKER));
        assert!(content.contains("expected reference(Note) got object"));
    }

    /// Characterization matrix for the `OirType → Kotlin` walk (now the shared
    /// `orbital_core::render_type` + `KotlinRenderer`). Leaf and container
    /// arms are pinned byte-for-byte; the `Struct`/`Union` arms pin the
    /// Phase-2 named-declaration output — the field references the type by
    /// bare name and the declaration hoists (see
    /// `oir_type_to_kotlin_hoisted`).
    fn kt_ty(ty: &OirType) -> String {
        oir_type_to_kotlin(ty, "Ctx")
    }

    #[test]
    fn char_map_type_to_kotlin_arms() {
        assert_eq!(kt_ty(&OirType::String), "String");
        assert_eq!(kt_ty(&OirType::Number), "Double");
        assert_eq!(kt_ty(&OirType::Integer), "Double");
        assert_eq!(kt_ty(&OirType::Boolean), "Boolean");
        assert_eq!(kt_ty(&OirType::Date), "String");
        assert_eq!(kt_ty(&OirType::DateTime), "String");
        assert_eq!(
            kt_ty(&OirType::Json),
            "kotlinx.serialization.json.JsonElement"
        );
        assert_eq!(
            kt_ty(&OirType::Scalar),
            "kotlinx.serialization.json.JsonPrimitive"
        );
        assert_eq!(
            kt_ty(&OirType::SExpr),
            "kotlinx.serialization.json.JsonElement"
        );
        // A declared relation field stores the referenced row's id — the
        // core `reference` default (`self.string(None)`) applies since the
        // Kotlin backend no longer overrides it to the entity's class name.
        assert_eq!(kt_ty(&OirType::reference("Customer")), "String");
        assert_eq!(kt_ty(&OirType::enum_type("task_status")), "TaskStatus");
        assert_eq!(kt_ty(&OirType::Trait), "String");

        // Recursive arms.
        assert_eq!(kt_ty(&OirType::array(OirType::Number)), "List<Double>");
        assert_eq!(
            kt_ty(&OirType::map(OirType::String, OirType::Number)),
            "Map<String, Double>"
        );
        assert_eq!(kt_ty(&OirType::optional(OirType::Boolean)), "Boolean?");
        assert_eq!(
            kt_ty(&OirType::optional(OirType::optional(OirType::Integer))),
            "Double?"
        );

        // Struct: one required + one optional field. The field position now
        // carries the bare name and the `@Serializable data class` declaration
        // hoists (Phase-2 fix — the old arm spliced a declaration into type
        // position, which is not Kotlin). Optional here means
        // `required: false` on a bare `field_type` (NOT wrapped in
        // `OirType::Optional`) — matching how `type_for_field` hands fields to
        // this mapper.
        let mut nick_name = OirField::new("nick_name", OirType::String);
        nick_name.required = false;
        let struct_ty = OirType::Struct {
            fields: vec![OirField::new("full_name", OirType::String), nick_name],
        };
        let (ty, hoisted) = oir_type_to_kotlin_hoisted(&struct_ty, "Ctx");
        assert_eq!(ty, "Ctx");
        assert_eq!(
            hoisted,
            vec!["@Serializable\ndata class Ctx(val fullName: String, val nickName: String?)"]
        );

        // Nested structs extend the positional name — no shared-context
        // same-name collision.
        let nested = OirType::Struct {
            fields: vec![
                OirField::new("geo", struct_ty.clone()),
                OirField::new("alt", struct_ty),
            ],
        };
        let (ty, hoisted) = oir_type_to_kotlin_hoisted(&nested, "Ctx");
        assert_eq!(ty, "Ctx");
        assert_eq!(
            hoisted,
            vec![
                "@Serializable\ndata class CtxGeo(val fullName: String, val nickName: String?)",
                "@Serializable\ndata class CtxAlt(val fullName: String, val nickName: String?)",
                "@Serializable\ndata class Ctx(val geo: CtxGeo, val alt: CtxAlt)",
            ]
        );

        // Union: a named sealed class whose subtypes are @Serializable
        // @SerialName-tagged data classes; nested variant shapes hoist under
        // `{Name}{Variant}…`.
        let union_ty = OirType::Union {
            variants: vec![
                OirField::new(
                    "book",
                    OirType::Struct {
                        fields: vec![OirField::new("kind", OirType::String)],
                    },
                ),
                OirField::new(
                    "movie",
                    OirType::Struct {
                        fields: vec![OirField::new("kind", OirType::String)],
                    },
                ),
            ],
        };
        let (ty, hoisted) = oir_type_to_kotlin_hoisted(&union_ty, "Ctx");
        assert_eq!(ty, "Ctx");
        assert_eq!(
            hoisted,
            vec![
                "@Serializable\nsealed class Ctx {\n    @Serializable\n    @SerialName(\"book\")\n    data class CtxBook(val kind: String) : Ctx()\n    @Serializable\n    @SerialName(\"movie\")\n    data class CtxMovie(val kind: String) : Ctx()\n}"
            ]
        );
    }

    // -----------------------------------------------------------------------
    // Parity gate: emitted Kotlin slotRole arms match orbital_core::slot_role
    // for every canonical slot name.
    // -----------------------------------------------------------------------

    #[test]
    fn slot_role_kotlin_parity_with_orbital_core() {
        let kt = generate_slot_role_file().content;

        // Each entry is (slot_name, expected_core_role, per_name_kt_arm_substring).
        // The arm substring must appear verbatim in the emitted SlotRole.kt `when` block
        // and contains BOTH the exact name literal AND the role — so corrupting any single
        // name (e.g. "modal" → "moodal") or its role (SlotRole.Modal → SlotRole.Main)
        // fails this entry's assertion independently of every other entry.
        let cases: &[(&str, CoreSlotRole, &str)] = &[
            // main-group: three names share one arm; each needle covers name+role.
            (
                "main",
                CoreSlotRole::Main,
                "name == \"main\" || name == \"content\" || name == \"screen\" -> SlotRole.Main",
            ),
            (
                "content",
                CoreSlotRole::Main,
                "name == \"content\" || name == \"screen\" -> SlotRole.Main",
            ),
            (
                "screen",
                CoreSlotRole::Main,
                "name == \"screen\" -> SlotRole.Main",
            ),
            // single-name arms
            (
                "sidebar",
                CoreSlotRole::Sidebar,
                "name == \"sidebar\" -> SlotRole.Sidebar",
            ),
            (
                "drawer",
                CoreSlotRole::Drawer,
                "name == \"drawer\" || name == \"dock\" -> SlotRole.Drawer",
            ),
            (
                "dock",
                CoreSlotRole::Drawer,
                "name == \"dock\" -> SlotRole.Drawer",
            ),
            // modal-group: two names share one arm
            (
                "modal",
                CoreSlotRole::Modal,
                "name == \"modal\" || name == \"center\" -> SlotRole.Modal",
            ),
            (
                "center",
                CoreSlotRole::Modal,
                "name == \"center\" -> SlotRole.Modal",
            ),
            // overlay exact-match arm (distinct from the prefix arm)
            (
                "overlay",
                CoreSlotRole::Overlay,
                "name == \"overlay\" -> SlotRole.Overlay",
            ),
            // overlay prefix arm
            (
                "overlay.dialogue",
                CoreSlotRole::Overlay,
                "name.startsWith(\"overlay.\") -> SlotRole.Overlay",
            ),
            (
                "toast",
                CoreSlotRole::Toast,
                "name == \"toast\" -> SlotRole.Toast",
            ),
            // hud-group: hud and hud-top share one arm
            (
                "hud",
                CoreSlotRole::HudTop,
                "name == \"hud\" || name == \"hud-top\" -> SlotRole.HudTop",
            ),
            (
                "hud-top",
                CoreSlotRole::HudTop,
                "name == \"hud-top\" -> SlotRole.HudTop",
            ),
            // hud prefix arm
            (
                "hud.health",
                CoreSlotRole::HudTop,
                "name.startsWith(\"hud.\") -> SlotRole.HudTop",
            ),
            (
                "hud-bottom",
                CoreSlotRole::HudBottom,
                "name == \"hud-bottom\" -> SlotRole.HudBottom",
            ),
            (
                "floating",
                CoreSlotRole::Floating,
                "name == \"floating\" -> SlotRole.Floating",
            ),
            (
                "system",
                CoreSlotRole::System,
                "name == \"system\" -> SlotRole.System",
            ),
        ];

        for (name, expected_core_role, arm_needle) in cases {
            // Confirm orbital_core canonical owner agrees.
            assert_eq!(
                core_slot_role(name),
                *expected_core_role,
                "orbital_core::slot_role({:?}) mismatch",
                name
            );

            // Confirm the emitted Kotlin `when` arm contains BOTH the exact name literal
            // AND the correct SlotRole variant.  A per-name needle ensures that changing
            // "modal" -> SlotRole.Main (while "center" -> SlotRole.Modal still exists)
            // would fail *this* entry — the test is non-vacuous per slot name.
            assert!(
                kt.contains(arm_needle),
                "SlotRole.kt missing exact arm for slot {:?}; expected substring:\n  {}\ngot:\n{}",
                name,
                arm_needle,
                kt
            );
        }
    }

    #[test]
    fn slot_role_file_emitted_in_generate_output() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let paths: Vec<&str> = output.files.iter().map(|f| f.path.as_str()).collect();
        assert!(
            paths.contains(&"SlotRole.kt"),
            "generate() must emit SlotRole.kt; got: {:?}",
            paths
        );
    }

    #[test]
    fn app_file_imports_slot_role_and_dispatches_overlays() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let app = output.files.iter().find(|f| f.path == "App.kt").unwrap();

        // Root composable uses slotRole for dispatch.
        assert!(
            app.content.contains("slotRole("),
            "App.kt must call slotRole() to dispatch slot roles"
        );
        assert!(
            app.content.contains("SlotRole.Modal"),
            "App.kt must handle Modal slots"
        );
        assert!(
            app.content.contains("SlotRole.Drawer"),
            "App.kt must handle Drawer slots"
        );
        assert!(
            app.content.contains("SlotRole.Overlay"),
            "App.kt must handle Overlay slots"
        );
        assert!(
            app.content.contains("SlotRole.Toast"),
            "App.kt must handle Toast slots"
        );
        assert!(
            app.content.contains("SlotRole.HudTop"),
            "App.kt must handle HudTop slots"
        );
        assert!(
            app.content.contains("SlotRole.HudBottom"),
            "App.kt must handle HudBottom slots"
        );
        assert!(
            app.content.contains("SlotRole.Sidebar"),
            "App.kt must handle Sidebar slots"
        );
        assert!(
            app.content.contains("SlotRole.Floating"),
            "App.kt must handle Floating slots"
        );
        // Dialog is the Compose widget for Modal slots.
        assert!(
            app.content.contains("Dialog("),
            "App.kt must use Dialog for Modal slots"
        );
        // ModalNavigationDrawer is the ONLY widget for Drawer slots — it used to
        // be duplicated with a ModalBottomSheet rendering the same drawerContent
        // a second time; that duplicate is removed (C0, §157).
        assert!(
            app.content.contains("ModalNavigationDrawer("),
            "App.kt must use ModalNavigationDrawer for Drawer slots"
        );
        assert!(
            !app.content.contains("ModalBottomSheet("),
            "App.kt must not duplicate the Drawer slot as a ModalBottomSheet"
        );
        // The debug "Page: <name>" placeholder never reaches a real screen.
        assert!(
            !app.content.contains("Text(\"Page: "),
            "App.kt must not render a debug page-name Text"
        );
    }

    #[test]
    fn effect_handler_clears_slot_on_null_content() {
        let content = generate_effect_handlers_file().content;
        // Null content must remove the slot from the map (close the overlay).
        assert!(
            content.contains("uiSlots.value = uiSlots.value - slot"),
            "EffectHandlers must remove slot from map when content is null/JsonNull"
        );
        assert!(
            content.contains("JsonNull"),
            "EffectHandlers must import/check JsonNull to detect null slot content"
        );
    }

    /// Coverage gate: the EffectHandlers `when` must handle every client-effect kind the kernel
    /// can hand back (orbital_core::effects::CLIENT_EFFECT_KINDS) — no silent fall-through.
    #[test]
    fn effect_handlers_cover_all_client_effect_kinds() {
        let content = generate_effect_handlers_file().content;
        for kind in orbital_core::effects::CLIENT_EFFECT_KINDS {
            let arm = format!("\"{kind}\" ->");
            assert!(
                content.contains(&arm),
                "EffectHandlers.kt missing an arm for client effect `{kind}` (expected `{arm}`)"
            );
        }
    }

    /// Regression: `EffectHandlers.kt` called `.contentOrNull` without importing
    /// `kotlinx.serialization.json.contentOrNull` — a structural-assertion-only
    /// test suite never catches an unresolved-reference compile error, only a
    /// real `./gradlew assembleDebug` does (which is exactly what caught this).
    /// Every generated file that USES a `kotlinx.serialization.json` extension
    /// (`.jsonPrimitive`/`.jsonArray`/`.jsonObject`/`.contentOrNull`) must import it.
    #[test]
    fn generated_files_import_every_json_extension_they_use() {
        let backend = AndroidBackend::new();
        let module = OirModule::new("TestApp", "1.0.0");
        let output = backend.generate(&module);
        let authored = [
            "ApiClient.kt",
            "EffectHandlers.kt",
            "OrbitalViewModel.kt",
            "App.kt",
        ];
        let extensions = ["contentOrNull", "jsonPrimitive", "jsonArray"];
        for path in authored {
            let file = output.files.iter().find(|f| f.path == path).unwrap();
            for ext in extensions {
                if file.content.contains(&format!(".{ext}")) {
                    let import = format!("import kotlinx.serialization.json.{ext}");
                    assert!(
                        file.content.contains(&import),
                        "{path} uses `.{ext}` without `{import}`"
                    );
                }
            }
        }
    }

    #[test]
    fn verify_bridge_starts_only_from_a_debuggable_activity_launch_extra() {
        let module = OirModule::new("TestApp", "1.0.0");
        let activity = generate_generated_activity_file(&module).content;
        assert!(activity.contains("ApplicationInfo.FLAG_DEBUGGABLE"));
        assert!(activity.contains(r#"intent.getStringExtra("ORBITAL_VERIFY_BRIDGE") == "1""#));
        assert!(activity
            .contains(r#"intent.getStringExtra("ORBITAL_VERIFY_PORT")?.toIntOrNull() ?: 7357"#));
        let gate = activity.find("if (debuggable &&").unwrap();
        let assign = activity.find("VerifyBridgeLaunch.port =").unwrap();
        let set_content = activity.find("setContent {").unwrap();
        assert!(
            gate < assign && assign < set_content,
            "the request must be recorded before the view model exists"
        );
    }

    #[test]
    fn view_model_starts_the_bridge_after_kernel_create_and_before_init() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        let created = vm.find("kernel = created").unwrap();
        let start = vm
            .find("VerifyBridgeLaunch.port?.let { startVerifyBridge(created, it) }")
            .unwrap();
        let ready = vm.find("kernelReady.complete(Unit)").unwrap();
        assert!(created < start && start < ready);
        assert!(vm.contains("object VerifyBridgeLaunch"));
        assert!(vm.contains("import generated.patterns.core.OrbitalVerifyDispatchListener"));
    }

    #[test]
    fn view_model_tracks_live_main_writers_in_first_write_order() {
        let view_model = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(view_model
            .contains("private val _mainOrder = MutableStateFlow<List<String>>(emptyList())"));
        assert!(
            view_model.contains("private val _lastMainWriter = MutableStateFlow<String?>(null)")
        );
        assert!(view_model
            .contains("EffectHandlers(_uiSlots, _traitContent, _mainOrder, _lastMainWriter,"));
        let vm = generate_effect_handlers_file().content;
        let append =
            "if (sourceTrait !in mainOrder.value) mainOrder.value = mainOrder.value + sourceTrait";
        let remove = "mainOrder.value = mainOrder.value - sourceTrait";
        assert!(
            vm.contains(append),
            "a new writer is appended; a re-render keeps its position"
        );
        assert!(vm.contains(remove), "a clear removes only that trait");
        assert!(vm.contains("lastMainWriter.value = sourceTrait"));
        assert!(vm.contains("if (lastMainWriter.value == sourceTrait) lastMainWriter.value = null"));
        let overlay = vm
            .find("uiSlots.value = uiSlots.value + (slot to content)")
            .unwrap();
        assert!(
            vm.find(append).unwrap() < overlay,
            "only the main branch marks a main owner"
        );
        // control: no previous-writer fallback and no wholesale reset of other writers
        assert!(!vm.contains("mainOrder.value = emptyList()"));
        assert!(!vm.contains("mainOrder.value = listOf("));
    }

    #[test]
    fn page_carries_every_bound_trait_and_stacks_live_writers() {
        let mut module = OirModule::new("TestApp", "1.0.0");
        let mut page = OirPage::new("Browse", "/threadposts");
        page.traits.push(OirTraitBinding::new("AuthorPresence"));
        page.traits.push(OirTraitBinding::new("ThreadPostBrowse"));
        module.pages.push(page);
        let app = generate_app_file(&module).content;
        assert!(app.contains(
            "GeneratedPage(\"Browse\", viewModel, listOf(\"AuthorPresence\", \"ThreadPostBrowse\"))"
        ));
        assert!(app.contains("val liveTraits = mainOrder.filter { it in traitNames }"));
        assert!(app.contains("1 -> SlotContent(mainContents[0].second"));
        assert!(app.contains("verticalArrangement = Arrangement.spacedBy(24.dp)"));
        assert!(app.contains("import androidx.compose.foundation.layout.Arrangement"));
        // control: a one-trait page keeps a one-element list; no first-trait-only binding
        let mut single = OirModule::new("TestApp", "1.0.0");
        let mut p = OirPage::new("Home", "/");
        p.traits.push(OirTraitBinding::new("HomeLifecycle"));
        single.pages.push(p);
        assert!(generate_app_file(&single)
            .content
            .contains("listOf(\"HomeLifecycle\"))"));
        assert!(!app.contains("traitContentMap[traitName]"));
    }

    #[test]
    fn page_active_trait_follows_the_most_recent_main_writer() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(app.contains(
            "val traitName = lastMainWriter?.takeIf { it in liveTraits } ?: liveTraits.lastOrNull() ?: traitNames.firstOrNull() ?: \"\""
        ));
        assert!(app.contains("LaunchedEffect(traitName) {\n        if (traitName.isNotEmpty()) viewModel.activePageTrait = traitName"));
    }

    #[test]
    fn verify_dispatch_routes_through_send_event_with_scope_or_active_page() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains("val trait = traitScope ?: activePageTrait"));
        assert!(vm.contains("sendEvent(trait, event, payload)"));
        assert!(vm.contains("override fun onVerifyDispatch(traitScope: String?, event: String, payloadJson: String?)"));
    }

    #[test]
    fn page_publishes_its_trait_and_root_exposes_test_tags_as_resource_ids() {
        let module = OirModule::new("TestApp", "1.0.0");
        let app = generate_app_file(&module).content;
        assert!(app.contains("viewModel.activePageTrait = traitName"));
        assert!(app.contains("semantics { testTagsAsResourceId = true }"));
        assert!(app.contains("import androidx.compose.ui.semantics.testTagsAsResourceId"));
        assert!(
            app.contains("@OptIn(ExperimentalComposeUiApi::class)\n@Composable\nfun GeneratedApp")
        );
    }

    #[test]
    fn kernel_bridge_declares_a_native_for_every_verify_bridge_symbol() {
        let bridge = include_str!("renderers/core/OrbitalKernelBridge.kt");
        for name in [
            "nativeVerifyBridgeStart",
            "nativeVerifyBridgeStop",
            "nativeVerifyBridgeSetDispatchCallback",
            "nativeVerifyBridgeSetRoute",
            "nativeVerifyBridgeSetReady",
        ] {
            assert!(
                bridge.contains(&format!("external fun {name}(")),
                "missing {name}"
            );
            let jni = format!("Java_generated_patterns_core_OrbitalKernelBridge_{name}");
            assert!(
                orbital_ffi::jni_kernel_bridge::KERNEL_JNI_SYMBOLS.contains(&jni.as_str()),
                "{jni} not exported"
            );
        }
        assert!(bridge.contains("interface OrbitalVerifyDispatchListener"));
        assert!(bridge.contains(
            "fun onVerifyDispatch(traitScope: String?, event: String, payloadJson: String?)"
        ));
    }

    /// Item 2 of G-ORB-COMPILER-038 stays open: the envelope copies the node's own
    /// `sourceTrait`, which only the splice stamps onto nested nodes.
    #[test]
    fn envelope_routing_still_depends_on_the_splice_stamp() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains(r#"if (hasType) out["sourceTrait"] = JsonPrimitive(own)"#));
        let app = generate_app_file(&module).content;
        assert!(
            app.contains(r#"obj?.get("sourceTrait")?.jsonPrimitive?.contentOrNull ?: traitName"#)
        );
    }

    #[test]
    fn api_base_url_override_is_a_debuggable_only_launch_extra() {
        let module = OirModule::new("TestApp", "1.0.0");
        let activity = generate_generated_activity_file(&module).content;
        let extra = activity
            .find(r#"intent.getStringExtra("ORBITAL_API_BASE_URL")"#)
            .expect("API base override extra missing");
        let gate = activity.find("if (debuggable").unwrap();
        let set_content = activity.find("setContent {").unwrap();
        assert!(gate < extra && extra < set_content);
        assert!(activity.contains("VerifyBridgeLaunch.apiBaseUrl ="));
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains("var apiBaseUrl: String? = null"));
        assert!(vm.contains("OrbitalApi(VerifyBridgeLaunch.apiBaseUrl ?: BASE_URL)"));
    }

    #[test]
    fn auth_token_extra_is_debuggable_only_and_reaches_the_api_client() {
        let module = OirModule::new("TestApp", "1.0.0");
        let activity = generate_generated_activity_file(&module).content;
        let extra = activity
            .find(r#"intent.getStringExtra("ORBITAL_AUTH_TOKEN")"#)
            .expect("auth token extra missing");
        let gate = activity.find("if (debuggable").unwrap();
        let set_content = activity.find("setContent {").unwrap();
        assert!(gate < extra && extra < set_content);
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains("@Volatile var authToken: String? = null"));
        assert!(vm.contains("VerifyBridgeLaunch.authToken?.let { client.authToken = it }"));
        let api = generate_api_client_file(&module).content;
        assert!(api.contains(
            r#"authToken?.let { requestBuilder.header("Authorization", "Bearer $it") }"#
        ));
        assert!(api.contains(r#"authToken?.let { header("Authorization", "Bearer $it") }"#));
    }

    #[test]
    fn auth_token_extra_control_nothing_else_assigns_the_token() {
        let module = OirModule::new("TestApp", "1.0.0");
        let activity = generate_generated_activity_file(&module).content;
        assert_eq!(activity.matches("ORBITAL_AUTH_TOKEN").count(), 2);
        let vm = generate_view_model_file(&module).content;
        assert_eq!(vm.matches("client.authToken =").count(), 1);
        assert!(!vm.contains("ORBITAL_AUTH_TOKEN"));
    }

    #[test]
    fn native_sign_in_adopts_the_stored_session_before_the_live_push_and_the_first_init() {
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        let adopt = vm
            .find("            adoptStoredSession()")
            .expect("session never adopted");
        let live = vm.find("            openLivePush()").unwrap();
        let ready = vm.find("kernelReady.complete(Unit)").unwrap();
        assert!(
            adopt < live && live < ready,
            "signed requests need the session first"
        );
        assert!(vm.contains("KeystoreSessionStore(application)"));
        assert!(vm.contains("OrbitalSessionBinding({ api.authToken = it }, { json -> kernel?.setCurrentUser(json) })"));
        assert!(vm.contains(r#"const val HOST_SIGN_IN_ROUTE = "/login""#));
    }

    #[test]
    fn native_sign_in_identity_change_reconnects_live_push_and_reinits_only_after_ready() {
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        let apply = vm.find("private fun applySession").unwrap();
        let body = &vm[apply..];
        assert!(body.contains("if (!identityChanged || !isReady) return"));
        assert!(body.find("openLivePush()").unwrap() < body.find("initAllTraits()").unwrap());
    }

    #[test]
    fn native_sign_in_control_the_verify_persona_token_keeps_the_stored_session_out() {
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        let adopt = vm.find("private fun adoptStoredSession()").unwrap();
        assert!(vm[adopt..].contains(
            "if (VerifyBridgeLaunch.authToken == null) {\n            auth.loadStoredSession()"
        ));
    }

    #[test]
    fn native_sign_in_emulator_override_is_a_debuggable_only_extra() {
        let module = OirModule::new("TestApp", "1.0.0");
        let activity = generate_generated_activity_file(&module).content;
        let extra = activity
            .find(r#"intent.getStringExtra("ORBITAL_AUTH_EMULATOR_HOST")"#)
            .expect("extra missing");
        let gate = activity.find("if (debuggable").unwrap();
        let set_content = activity.find("setContent {").unwrap();
        assert!(gate < extra && extra < set_content);
        assert_eq!(
            activity
                .matches(r#"getStringExtra("ORBITAL_AUTH_EMULATOR_HOST")"#)
                .count(),
            1
        );
    }

    #[test]
    fn native_sign_in_live_push_is_cancellable_and_reads_the_current_token_per_connect() {
        let api = generate_api_client_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(api.contains(
            "fun openLivePush(onItem: (LiveBroadcastItem) -> Unit): LivePushSubscription"
        ));
        assert!(api.contains("if (subscription.cancelled) return@connect"));
        let connect = api.find("connect = connect@{").unwrap();
        assert!(
            api[connect..].contains("authToken?.let { requestBuilder.header("),
            "token must be read inside connect"
        );
    }

    #[test]
    fn native_sign_in_host_route_is_in_the_nav_host_and_pages_cannot_shadow_it() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(app.contains(
            "composable(OrbitalViewModel.HOST_SIGN_IN_ROUTE) { OrbitalSignInScreen(viewModel) }"
        ));
        assert!(app.contains("import generated.patterns.core.OrbitalSignInScreen"));
    }

    #[test]
    fn native_sign_in_token_refresh_rides_the_composition_lifecycle_not_the_activity() {
        let module = OirModule::new("TestApp", "1.0.0");
        let activity = generate_generated_activity_file(&module).content;
        assert!(!activity.contains("viewModels"), "touching the view model in the activity builds it before the first composition, which races the initial-route replacement");
        let app = generate_app_file(&module).content;
        assert!(app
            .contains("repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.refreshSession() }"));
    }

    fn access_module(sign_in: Option<&str>) -> OirModule {
        let mut module = OirModule::new("TestApp", "1.0.0");
        let mut private = OirPage::new("Account", "/account");
        private.access = Some(orbital_core::schema::PageAccess::Authenticated);
        let mut public = OirPage::new("Home", "/home");
        public.access = Some(orbital_core::schema::PageAccess::Public);
        module.pages = vec![private, public, OirPage::new("About", "/about")];
        module.site = sign_in.map(|route| orbital_core::schema::SiteMeta {
            sign_in: Some(route.to_string()),
            ..Default::default()
        });
        module
    }

    #[test]
    fn page_access_an_authenticated_page_composes_through_the_boundary_with_the_declared_sign_in() {
        let app = generate_app_file(&access_module(Some("/login"))).content;
        assert!(app.contains(
            r#"composable("/account") { AuthenticatedPage("/account", "/login", viewModel, navController) { GeneratedPage("Account""#
        ));
        assert!(app.contains("private fun AuthenticatedPage("));
        assert!(app.contains("key(session?.uid) { content() }"));
        assert!(
            app.contains("navController.navigate(signIn) { popUpTo(path) { inclusive = true } }")
        );
    }

    fn gated_trait_module() -> OirModule {
        let mut module = access_module(Some("/login"));
        for name in ["AccountView", "HomeView"] {
            let mut t = orbital_core::ir::OirTrait::new(name);
            t.events.push(orbital_core::ir::OirEvent::new("INIT"));
            module.traits.push(t);
        }
        let bind = |name: &str| orbital_core::ir::OirTraitBinding {
            trait_name: name.to_string(),
            trait_id: None,
            config: None,
        };
        module.pages[0].traits = vec![bind("AccountView")];
        module.pages[1].traits = vec![bind("HomeView")];
        module
    }

    #[test]
    fn page_access_a_gated_only_trait_inits_when_its_page_mounts_for_a_viewer_not_at_app_start() {
        let module = gated_trait_module();
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains(r#"TRAITS_WITH_INIT: Set<String> = setOf("HomeView")"#));
        assert!(vm.contains(r#"GATED_PAGE_TRAITS: Map<String, List<String>> = mapOf("/account" to listOf("AccountView"))"#));
        assert!(vm.contains("fun mountGatedPage(path: String, viewer: String)"));
        assert!(vm.contains("kernel?.mount(pending)"));
        let app = generate_app_file(&module).content;
        assert!(
            app.contains("LaunchedEffect(path, viewer) { viewModel.mountGatedPage(path, viewer) }")
        );
    }

    #[test]
    fn page_access_control_an_ungated_app_inits_every_trait_at_start() {
        let mut module = gated_trait_module();
        module.pages[0].access = None;
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains(r#"TRAITS_WITH_INIT: Set<String> = setOf("AccountView", "HomeView")"#));
        assert!(vm.contains("GATED_PAGE_TRAITS: Map<String, List<String>> = emptyMap()"));
    }

    #[test]
    fn page_access_control_public_and_undeclared_pages_compose_directly() {
        let app = generate_app_file(&access_module(Some("/login"))).content;
        assert!(app.contains(r#"composable("/home") { GeneratedPage("Home""#));
        assert!(app.contains(r#"composable("/about") { GeneratedPage("About""#));
        let ungated = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(!ungated.contains("AuthenticatedPage"));
    }

    #[test]
    fn page_access_without_a_declared_sign_in_names_none_and_denial_navigates_nowhere() {
        let app = generate_app_file(&access_module(None)).content;
        assert!(app.contains(r#"AuthenticatedPage("/account", "", viewModel, navController)"#));
        assert!(app.contains("if (signIn.isNotEmpty()) navController.navigate(signIn)"));
    }

    #[test]
    fn page_access_viewer_is_authenticated_by_a_session_or_the_debug_persona_token() {
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(
            vm.contains("val hasLaunchToken: Boolean get() = VerifyBridgeLaunch.authToken != null")
        );
        let app = generate_app_file(&access_module(None)).content;
        assert!(app.contains("if (session != null || viewModel.hasLaunchToken)"));
    }

    #[test]
    fn theme_mode_the_host_provides_the_toggle_through_a_composition_local_and_the_kernel() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(app.contains("LocalAlmadarThemeMode provides AlmadarThemeModeControl(viewModel.themeMode == ColorMode.Dark) { viewModel.toggleThemeMode() }, LocalAlmadarCanvasTheme provides viewModel.canvasTheme"));
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        let start = vm
            .find("fun toggleThemeMode()")
            .expect("toggleThemeMode missing");
        let toggle = &vm[start..start + vm[start..].find("A debuggable launch").unwrap()];
        assert!(toggle.contains(
            r#"live.setThemeMode(if (themeMode == ColorMode.Dark) "light" else "dark")"#
        ));
        assert!(toggle.contains("snap.theme?.let { applyResolvedTheme(it) }"));
        assert!(toggle.contains("canvasTheme = snap.canvasTheme"));
        assert!(toggle.contains("motion = snap.motion"), "the mode switch re-reads the resolved motion");
        assert!(app.contains("LocalAlmadarResolvedMotion provides viewModel.motion"));
        assert!(app.contains(".almadarSurfaceEnter(\"pageEnter\", AlmadarEnterTiming.Page, replayKey = pageName)"));
        assert!(app.contains("Modifier.fillMaxWidth().almadarSlotEnter(content, slotEnterKey(name, content))"));
        assert!(app.contains(".almadarSurfaceEnter(\"toastEnter\", AlmadarEnterTiming.Toast, replayKey = name)"));
        assert!(
            !toggle.contains("sendEvent"),
            "the toggle is a local control, not a bus event"
        );
    }

    #[test]
    fn native_sign_in_files_and_tests_are_emitted_and_the_host_gates_nothing() {
        let out = AndroidBackend::new().generate(&OirModule::new("TestApp", "1.0.0"));
        let find = |path: &str| {
            out.files
                .iter()
                .find(|f| f.path == path)
                .unwrap_or_else(|| panic!("{path} not emitted"))
        };
        let auth = &find("patterns/core/OrbitalAuth.kt").content;
        let screen = &find("patterns/core/OrbitalSignIn.kt").content;
        assert!(
            auth.contains("accounts:signInWithPassword")
                && auth.contains("accounts:signInWithCustomToken")
        );
        assert!(screen.contains("KeyProperties.KEY_ALGORITHM_AES"));
        assert!(
            find("app/src/test/kotlin/generated/patterns/core/OrbitalAuthTest.kt")
                .content
                .contains("class OrbitalAuthTest")
        );
        for source in [auth, screen] {
            for forbidden in [
                "navController",
                "navigate(",
                "navigationEvents",
                "rootReplacements",
            ] {
                assert!(
                    !source.contains(forbidden),
                    "host auth code must not route from auth state: {forbidden}"
                );
            }
        }
    }

    #[test]
    fn api_base_url_override_control_baked_url_is_still_the_default() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains(r#"BASE_URL = "http://10.0.2.2:3030""#));
        assert!(!vm.contains("ORBITAL_NAME"));
    }

    #[test]
    fn every_server_leg_failure_branch_restores() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        let start = vm.find("private suspend fun postServerLegAndFold").unwrap();
        let end = vm[start..].find("companion object").unwrap();
        let body = &vm[start..start + end];
        assert!(body.contains(
            ".onFailure { throwable ->\n                restore(throwable.message ?: throwable.toString())"
        ));
        assert!(body.contains("restore(message)"));
        assert!(
            body.contains("val applied = kernelCalls.withLock { k.applyServerResponse(raw, alreadyDelivered) }\n"),
            "an apply that returns null must be handled:\n{body}"
        );
        assert!(body.contains("val reason = k.lastError\n                        restore(reason)"));
    }

    #[test]
    fn applied_server_response_folds_through_the_dispatch_fold() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains("foldResponse(trait, result.response)"));
        assert!(vm.contains("foldResponse(requestTrait, applied.response)"));
        assert_eq!(vm.matches("private suspend fun foldResponse(").count(), 1);
        assert_eq!(vm.matches("effectHandlers.apply(effect,").count(), 1);
        let rejected = vm.find("val message = decoded?.error").unwrap();
        assert!(!vm[rejected..].contains("foldResponse(requestTrait, applied.response)"));
    }

    #[test]
    fn server_leg_success_branch_never_restores() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        let fold = vm
            .find("applied.applied.forEach")
            .expect("success fold missing");
        let outer_else = vm[fold..].find("}\n                } else {").unwrap();
        assert!(!vm[fold..fold + outer_else].contains("restore("));
        let rejected = vm.find("val message = decoded?.error").unwrap();
        assert!(vm[rejected..].contains("restore(message)"));
    }

    #[test]
    fn floating_slot_is_a_full_page_non_consuming_layer() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        let at = app
            .find("floatingSlots.forEach { (name, content) ->")
            .expect("floating layer");
        let layer = &app[at..(at + 320).min(app.len())];
        assert!(layer.contains("Box(modifier = Modifier.fillMaxSize())"));
        assert!(layer.contains("SlotContent(content, Modifier.fillMaxSize()"));
        assert!(
            !layer.contains("clickable") && !layer.contains("pointerInput"),
            "layer must not consume touches"
        );
        assert!(
            !layer.contains("Modifier.align(Alignment.BottomEnd)"),
            "the renderer, not the host, positions the content"
        );
    }

    #[test]
    fn page_body_adds_no_inset_padding_around_the_layout() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        let at = app
            .find("// Page body: sidebar + main content")
            .expect("page body");
        let end = app[at..].find("// HUD overlays").expect("hud");
        let body = &app[at..at + end];
        assert!(
            !body.contains("padding(16.dp)"),
            "the web page has no inset around its layout"
        );
        assert!(body.contains("PageBody(traitName, mainContents, viewModel)"));
    }

    #[test]
    fn floating_control_toast_overlay_keeps_its_corner_alignment() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(app.contains("Modifier.align(Alignment.TopEnd).padding(8.dp)"));
    }

    #[test]
    fn page_stamps_main_and_every_overlay_slot_by_canonical_name_without_shadowing_the_pattern_tag()
    {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        let slot_content = app.find("private fun SlotContent(").unwrap();
        let body = &app[slot_content..];
        let box_at = body
            .find("Box(modifier = if (tagged) Modifier.verificationContainer(VerificationIds.slot(slot)) else Modifier)")
            .expect("slot stamp wraps the pattern in its own Box");
        let registry_at = body.find("CorePatternRegistry.get(type)(").unwrap();
        assert!(box_at < registry_at);
        assert!(app.contains("PageBody"));
        assert!(app.contains("viewModel, \"main\")"));
        assert!(app.contains("viewModel, \"main\", tagged = false)"));
        for var in [
            "modalSlot",
            "drawerSlot",
            "sidebarSlot",
            "hudTopSlot",
            "hudBottomSlot",
            "toastSlot",
        ] {
            assert!(
                app.contains(&format!("{var}?.let {{ (name, content)"))
                    || app.contains(&format!("{var} != null")),
                "{var} not consumed with its slot name"
            );
        }
        assert!(app.contains("overlaySlots.forEach { (name, content)"));
        assert!(app.contains("floatingSlots.forEach { (name, content)"));
        assert_eq!(
            app.matches("VerificationIds.slot(").count(),
            2,
            "two stamp sites: `main` in the page body, every other slot in SlotContent"
        );
        assert!(!app.contains("VerificationIds.slot(\"modal\")"));
        assert!(!app.contains("VerificationIds.slot(\"toast\")"));
    }

    #[test]
    fn slot_content_routes_navigate_before_kernel_dispatch_and_other_events_still_dispatch() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        let nav = app
            .find("if (event == OrbitalViewModel.NAVIGATE_EVENT)")
            .unwrap();
        let route = app.find("viewModel.navigate(envelope)").unwrap();
        let other = app
            .find("viewModel.sendEvent(source, event, payload = payload)")
            .unwrap();
        assert!(nav < route && route < other);
        assert!(app[route..other].contains("} else {"));
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(vm.contains("const val NAVIGATE_EVENT = \"NAVIGATE\""));
        assert!(vm.contains("viewModelScope.launch { _navigationEvents.emit(url) }"));
        assert!(vm.contains(
            "android.util.Log.e(\"Orbital\", \"NAVIGATE dropped: no actionPayload.url\")"
        ));
    }

    /// The system event `UI:SIGN_OUT` (`@almadar/core` `SYSTEM_UI_EVENTS`) ends the session in the
    /// host and never reaches the kernel (G-PATTERN-SYNC-031).
    #[test]
    fn slot_content_routes_sign_out_to_the_session_before_kernel_dispatch() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        let branch = app.find("} else if (event == OrbitalViewModel.SIGN_OUT_EVENT) {").expect("sign-out branch");
        let session = app.find("viewModel.signOut()").unwrap();
        let other = app.find("viewModel.sendEvent(source, event, payload = payload)").unwrap();
        assert!(branch < session && session < other);
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(vm.contains("const val SIGN_OUT_EVENT = \"SIGN_OUT\""));
    }

    #[test]
    fn an_undeclared_route_is_logged_not_thrown_by_the_single_navigation_sink() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        let sink = app.find("viewModel.navigationEvents.collect").unwrap();
        let tail = &app[sink..];
        let nav = tail.find("navController.navigate(path)").unwrap();
        let catch = tail.find("catch (e: IllegalArgumentException)").unwrap();
        let log = tail.find("navigate to undeclared route $path").unwrap();
        assert!(nav < catch && catch < log);
        let effects = generate_effect_handlers_file().content;
        assert!(effects.contains("navigationEvents.emit(path)"));
    }

    #[test]
    fn server_leg_failure_without_a_snapshot_abandons_and_with_one_restores() {
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        let restore = vm.find("fun restore(error: String)").unwrap();
        let tail = &vm[restore..];
        let restored = tail
            .find("k.restore(requestTrait, null, snapshot)")
            .unwrap();
        let else_at = tail.find("} else {").unwrap();
        let abandon = tail
            .find("k.abandonServerLeg(leg.targetTrait ?: requestTrait, seedEvent, error)")
            .unwrap();
        assert!(restored < else_at && else_at < abandon);
        assert_eq!(
            vm.matches("restore(").count() - vm.matches("k.restore(").count(),
            4
        );
        assert_eq!(vm.matches("abandonServerLeg(").count(), 1);
    }

    #[test]
    fn initial_route_extra_is_read_in_debug_only_and_opens_through_the_navigation_sink() {
        let module = OirModule::new("TestApp", "1.0.0");
        let activity = generate_generated_activity_file(&module).content;
        let debuggable = activity.find("if (debuggable) {").unwrap();
        let read = activity
            .find(r#"intent.getStringExtra("ORBITAL_INITIAL_ROUTE")"#)
            .unwrap();
        let set_content = activity.find("setContent {").unwrap();
        assert!(debuggable < read && read < set_content);
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains("@Volatile var initialRoute: String? = null"));
        let open = vm.find("fun openInitialRoute(startRoute: String)").unwrap();
        assert!(vm[open..].contains(
            "if (route != startRoute) viewModelScope.launch { _rootReplacements.emit(route) }"
        ));
        let app = generate_app_file(&module).content;
        assert!(app.contains("LaunchedEffect(viewModel.isReady) {"));
        assert!(app.contains("if (viewModel.isReady) viewModel.openInitialRoute(\""));
    }

    #[test]
    fn no_initial_route_extra_means_the_sink_is_never_emitted_to() {
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        let open = vm.find("fun openInitialRoute(startRoute: String)").unwrap();
        let body = &vm[open..];
        let early = body
            .find("VerifyBridgeLaunch.initialRoute?.takeIf { it.isNotEmpty() } ?: return")
            .unwrap();
        let emit = body.find("_rootReplacements.emit(route)").unwrap();
        assert!(early < emit);
    }

    #[test]
    fn initial_route_replaces_the_back_stack_root_instead_of_pushing() {
        let module = OirModule::new("TestApp", "1.0.0");
        let app = generate_app_file(&module).content;
        let sink = app.find("viewModel.rootReplacements.collect").unwrap();
        let tail = &app[sink..sink + app[sink..].find("LaunchedEffect(Unit)").unwrap()];
        assert!(tail.contains("navController.navigate(path) {"));
        assert!(
            tail.contains("popUpTo(navController.graph.startDestinationId) { inclusive = true }")
        );
        let vm = generate_view_model_file(&module).content;
        let open = vm.find("fun openInitialRoute(startRoute: String)").unwrap();
        assert!(
            !vm[open..open + vm[open..].find("\n    }\n").unwrap()].contains("_navigationEvents")
        );
        // Control: an ordinary navigate effect still pushes through its own sink, without popUpTo.
        let nav = app.find("viewModel.navigationEvents.collect").unwrap();
        let nav_tail = &app[nav..nav + app[nav..].find("LaunchedEffect(Unit)").unwrap()];
        assert!(nav_tail.contains("navController.navigate(path)\n"));
        assert!(!nav_tail.contains("popUpTo"));
    }

    #[test]
    fn route_is_published_from_the_nav_back_stack_when_the_verify_bridge_is_requested() {
        let app = generate_app_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(app.contains("navController.currentBackStackEntryFlow.collect"));
        assert!(app.contains("viewModel.onRouteChanged(entry.destination.route)"));
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(vm.contains("if (VerifyBridgeLaunch.port != null) kernel?.setVerifyRoute(route)"));
        assert!(vm.contains("currentRoute?.let { k.setVerifyRoute(it) }"));
    }

    #[test]
    fn kernel_bridge_declares_abandon_and_set_route_natives_with_jni_exports() {
        let bridge = include_str!("renderers/core/OrbitalKernelBridge.kt");
        for (name, sig) in [
            (
                "nativeAbandonServerLeg",
                "handle: Long, traitName: String, event: String?, error: String?",
            ),
            ("nativeVerifyBridgeSetRoute", "handle: Long, route: String?"),
        ] {
            assert!(
                bridge.contains(&format!("external fun {name}({sig})")),
                "{name}"
            );
            let jni = format!("Java_generated_patterns_core_OrbitalKernelBridge_{name}");
            assert!(orbital_ffi::jni_kernel_bridge::KERNEL_JNI_SYMBOLS.contains(&jni.as_str()));
        }
    }

    /// Snapshots capture one fixed-width node (not the device root) with an
    /// explicit change threshold.
    #[test]
    fn pattern_snapshots_use_one_column_width_and_strict_tolerance() {
        let test = generate_pattern_snapshot_test_file().content;
        assert!(test.contains("const val COLUMN_WIDTH_DP: Int = 358"));
        assert!(test.contains(".width(COLUMN_WIDTH_DP.dp)"));
        assert!(test.contains("composeTestRule.onNodeWithTag(HOST_TAG).captureRoboImage("));
        assert!(test.contains("RoborazziOptions.CompareOptions(changeThreshold = 0.01f)"));
        // Control: the full-device root capture is gone.
        assert!(!test.contains("onRoot()"));
        assert!(!test.contains("fillMaxSize"));
    }

    /// G-PATTERN-SYNC-029: an extra snapshot renders `image` with an SVG data-URI source, so the
    /// SVG decoder is proven by pixels; it waits for Coil's off-thread decode before capturing.
    #[test]
    fn svg_image_snapshot_is_an_extra_that_renders_the_image_pattern_and_awaits_the_decode() {
        let test = generate_pattern_snapshot_test_file().content;
        assert!(test.contains("assertPatternSnapshot(\"image-svg\", \"image\", stageAsset(EXTRA_IMAGE_SVG, \".svg\", \"image/svg+xml\", EXTRA_IMAGE_SVG_ASSET), awaitImages = true)"));
        assert!(
            test.contains("<svg xmlns="),
            "the SVG markup is embedded and staged to a .svg file Coil reads by MIME type"
        );
        assert!(
            !test.contains("data:image/svg+xml"),
            "Coil 2 has no data-URI fetcher"
        );
        assert!(test.contains("awaitAsyncImages()"));
        // Control: every generated fixture snapshot still names its own pattern for both file and renderer.
        assert!(test.contains("assertPatternSnapshot(\"alert\", \"alert\", FIXTURE_ALERT)"));
        assert!(!test
            .contains("assertPatternSnapshot(\"alert\", \"alert\", FIXTURE_ALERT, awaitImages"));
        // Control: the decode wait is opt-in per snapshot, not applied to every pattern.
        assert_eq!(
            test.matches("awaitAsyncImages()").count(),
            2,
            "the declaration and the one gated call"
        );
        assert_eq!(
            test.matches("awaitImages = true").count(),
            1,
            "only the SVG extra opts in"
        );
    }

    /// G-ORB-174: a composition-frame loop drives the kernel's tick clock; its output folds
    /// through the dispatch fold (`foldDispatch`) a tapped event also uses.
    #[test]
    fn frame_loop_calls_the_kernel_frame_api_and_folds_through_the_dispatch_fold() {
        let vm = generate_view_model_file(&OirModule::new("TestApp", "1.0.0")).content;
        assert!(vm.contains("k.frame(nowMs)"));
        let fold = vm.find("private suspend fun foldFrame(").unwrap();
        let body = &vm[fold..fold + vm[fold..].find("\n    }\n").unwrap()];
        assert!(
            body.contains("foldResponse(frame.fired.firstOrNull()?.trait ?: \"\", frame.response)")
        );
        assert!(body.contains("foldDispatch(it.trait, it.result)"));
        assert_eq!(
            vm.matches("private suspend fun foldDispatch(").count(),
            1,
            "one dispatch fold"
        );
        let send = vm.find("fun sendEvent(").unwrap();
        let send_body = &vm[send..send + vm[send..].find("\n    }\n").unwrap()];
        assert!(
            send_body.contains("foldDispatch(trait, result)"),
            "a tap folds through the same function"
        );
        assert_eq!(vm.matches("private suspend fun foldResponse(").count(), 1);
    }

    #[test]
    fn frame_loop_runs_only_with_ticks_in_the_resumed_state_and_pauses_in_finally() {
        let module = OirModule::new("TestApp", "1.0.0");
        let vm = generate_view_model_file(&module).content;
        assert!(vm.contains("hasTicks = created.tickCount > 0"));
        let app = generate_app_file(&module).content;
        let gate = app
            .find("if (!viewModel.isReady || !viewModel.hasTicks) return@LaunchedEffect")
            .unwrap();
        let repeat = app
            .find("repeatOnLifecycle(Lifecycle.State.RESUMED)")
            .unwrap();
        let resume = app.find("viewModel.frameResume()").unwrap();
        let frames = app.find("withFrameNanos { it / 1_000_000L }").unwrap();
        let run = app.find("viewModel.runFrame(nowMs)").unwrap();
        let pause = app.find("viewModel.framePause()").unwrap();
        assert!(gate < repeat && repeat < resume && resume < frames && frames < run && run < pause);
        let finally = app.find("} finally {").unwrap();
        assert!(
            finally < pause,
            "the pause runs when the lifecycle leaves RESUMED or the effect cancels"
        );
    }

    #[test]
    fn kernel_bridge_wraps_the_frame_api() {
        let bridge = include_str!("renderers/core/OrbitalKernelBridge.kt");
        for sym in [
            "nativeFrame",
            "nativeFramePause",
            "nativeFrameResume",
            "nativeTickCount",
        ] {
            assert!(bridge.contains(&format!("external fun {sym}(")), "{sym}");
        }
        assert!(bridge.contains("fun frame(nowMs: Long): KernelFrameOutcome"));
    }
}
