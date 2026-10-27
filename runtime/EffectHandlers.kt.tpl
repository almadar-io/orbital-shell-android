package generated

import androidx.navigation.NavHostController
import generated.patterns.core.AlmadarBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Applies client-side effects the kernel handed back (each `KernelEventResponse
 * .clientEffectsByTrait` entry's `effect`, one `["render-ui", slot, config, props?, priority?]` /
 * `["navigate", path, params?, options?]` / `["navigate-back"]` tuple per
 * entry — the canonical wire, `orbital_core::effects::ClientEffect`).
 * `traitContent` indexes every Main-role render by its SOURCE trait (never a
 * slot name) so `@trait.X` composition can resolve it. `sourceTrait` is the
 * CALLER-resolved attribution (the entry's own `clientEffectsByTrait` `traitName`)
 * — the wire no longer stamps it inside
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
     * attributed to `sourceTrait` (the `traitName` of its
     * `clientEffectsByTrait` entry). A null/absent third element on
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
                AlmadarBus.post(event, arr.getOrNull(2))
                onEmit(sourceTrait, event, arr.getOrNull(2))
            }
        }
    }
}
