package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

internal fun JsonElement.primitive(key: String): JsonPrimitive? =
    (this as? JsonObject)?.get(key) as? JsonPrimitive

internal fun JsonElement.string(key: String, default: String = ""): String =
    primitive(key)?.let { if (it.isString || it.doubleOrNull != null) it.contentOrNull else null } ?: default

internal fun JsonElement.assetUrl(key: String): String =
    when (val value = (this as? JsonObject)?.get(key)) {
        is JsonPrimitive -> if (value.isString) value.content else ""
        is JsonObject -> (value["url"] as? JsonPrimitive)?.contentOrNull ?: ""
        else -> ""
    }

internal fun JsonElement.int(key: String, default: Int = 0): Int =
    primitive(key)?.let { if (it.isString) it.content.toIntOrNull() else it.content.toDoubleOrNull()?.toInt() } ?: default

internal fun JsonElement.float(key: String, default: Float = 0f): Float =
    primitive(key)?.contentOrNull?.toFloatOrNull() ?: default

internal fun JsonElement.bool(key: String, default: Boolean = false): Boolean =
    primitive(key)?.booleanOrNull
        ?: primitive(key)?.contentOrNull?.toBooleanStrictOrNull()
        ?: default

internal fun JsonElement.array(key: String): List<JsonElement> =
    ((this as? JsonObject)?.get(key) as? JsonArray) ?: emptyList()

internal fun JsonElement.jsonObject(key: String): JsonObject? =
    (this as? JsonObject)?.get(key) as? JsonObject

internal fun JsonElement.children(): List<JsonElement> =
    when (val c = (this as? JsonObject)?.get("children")) {
        is JsonArray -> c.toList()
        is JsonPrimitive -> listOf(
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive(c.content),
                    "text" to JsonPrimitive(c.content)
                )
            )
        )
        is JsonObject -> listOf(c)
        else -> emptyList()
    }

internal fun JsonElement.merging(overrides: Map<String, JsonElement>): JsonElement =
    JsonObject(((this as? JsonObject) ?: JsonObject(emptyMap())) + overrides)

internal fun Modifier.verificationId(id: String): Modifier = this.testTag(id)

internal fun Modifier.verificationContainer(id: String): Modifier = this.semantics { testTag = id }

/** A popup, menu or dialog is its own window: its test tags become resource ids only under its own root. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
internal fun Modifier.verificationWindowRoot(): Modifier = this.semantics { testTagsAsResourceId = true }

@Composable
internal fun RenderChild(
    child: JsonElement,
    modifier: Modifier = Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    when (child) {
        is JsonPrimitive -> Text(child.content)
        is JsonObject -> {
            val type = child["type"]?.jsonPrimitive?.contentOrNull ?: "unknown"
            CorePatternRegistry.get(type)(child, modifier, onEvent)
        }
        else -> {}
    }
}

// The one shape every renderer body's `onEvent(name, …)` call must pass as
// its payload (Program C, `docs/Almadar_Compiler_Gaps.md` §157/§162):
// {"sourceTrait": <props' own sourceTrait, verbatim>, "actionPayload": <resolved payload, absent — never JSON null — when there is none>}.
// `actionPayload` resolves to `props`' own DECLARED `actionPayload` when it
// is a non-null object, else the caller's computed `payload` (e.g.
// {searchTerm, entity}, {id}, {href}) when THAT is a non-null object, else
// the key is omitted entirely. The page (`SlotContent`'s `onEvent` closure)
// reads `envelope.sourceTrait ?: traitName` and sends `envelope.actionPayload`.
internal fun eventEnvelope(props: JsonElement, payload: JsonElement? = null): JsonElement {
    val obj = props as? JsonObject
    val sourceTrait = obj?.get("sourceTrait")
    val declared = obj?.get("actionPayload") as? JsonObject
    val resolved = declared ?: (payload as? JsonObject)
    val entries = mutableMapOf<String, JsonElement>()
    if (sourceTrait != null) entries["sourceTrait"] = sourceTrait
    if (resolved != null) entries["actionPayload"] = resolved
    return JsonObject(entries)
}
