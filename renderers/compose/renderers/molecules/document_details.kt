package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.clickable

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.semantics.Role

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DocumentDetailsRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val fields = props.array("fields").mapNotNull { raw ->
        when (raw) {
            is JsonPrimitive -> raw.contentOrNull?.takeIf { it.isNotEmpty() }?.let { almadarJson("name" to it) }
            is JsonObject -> {
                val name = raw.string("name", raw.string("field", raw.string("key")))
                val header = raw["header"]
                if (name.isEmpty()) null
                else JsonObject(raw + mapOf("name" to JsonPrimitive(name)) + (if (raw["label"] == null && header != null) mapOf("label" to header) else emptyMap()))
            }
            else -> null
        }
    }
    val record = almadarRows((props as? JsonObject)?.get("entity")).firstOrNull() ?: JsonObject(emptyMap())
    val recordId = almadarScalar(record["id"]) ?: ""
    val editable = props.string("metaCommitEvent").isNotEmpty()
    var editing by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    val commit: (String, JsonElement?, JsonElement?) -> Unit = { name, next, current ->
        editing = null
        val event = props.string("metaCommitEvent")
        if (event.isNotEmpty() && next != null && next != current) {
            onEvent(event, eventEnvelope(props, almadarJson("id" to recordId, "patch" to almadarJson("id" to recordId, name to next))))
        }
    }
    val blank: (JsonElement?) -> Boolean = { it == null || it is JsonNull || (it is JsonPrimitive && it.contentOrNull.isNullOrEmpty()) }
    val fieldText: (JsonObject) -> String = { field ->
        val raw = almadarNested(record, field.string("name"))
        if (raw is JsonArray) raw.mapNotNull { almadarScalar(it) }.joinToString(", ") else formatFieldValue(raw, field.string("format"))
    }
    val chipLabel: (JsonElement, JsonObject) -> String = { item, field ->
        val obj = item as? JsonObject
        if (obj != null) {
            listOf("name", "title", "label").map { obj.string(it) }.firstOrNull { it.isNotEmpty() } ?: obj.string("id")
        } else {
            val text = almadarScalar(item) ?: ""
            field.jsonObject("labels")?.string(text, text) ?: text
        }
    }
    if (fields.isNotEmpty()) {
        AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("md")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) {
                AlmadarText(props.string("title", "Details"), variant = "overline", color = "secondary", weight = "medium")
                fields.forEach { field ->
                    val name = field.string("name")
                    val label = field.string("label", name)
                    val raw = almadarNested(record, name)
                    val empty = blank(raw)
                    val options = almadarStrings(field["options"])
                    val declared = field.string("kind")
                    val type = if (declared.isNotEmpty()) declared
                        else if (field.string("type") == "image") "image"
                        else if (options.isNotEmpty()) "select"
                        else if ((raw as? JsonPrimitive)?.booleanOrNull != null || field.string("format") == "boolean") "boolean"
                        else if (raw is JsonArray || raw is JsonObject) "readonly"
                        else "text"
                    Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                            if (field.string("icon").isNotEmpty()) AlmadarGlyph(field.string("icon"), color = "muted", size = "xs")
                            AlmadarText(label, variant = "caption", color = "muted")
                        }
                        if (type == "image") {
                            if (empty) AlmadarText("—", variant = "small", color = "secondary")
                            else ImageRenderer(
                                almadarJson("src" to (almadarScalar(raw) ?: (raw as? JsonObject)?.string("url") ?: ""), "alt" to label, "aspect" to "16/9", "rounded" to "md", "bordered" to true),
                                Modifier.fillMaxWidth(), AlmadarNoEvent
                            )
                        } else if (type == "boolean") {
                            SwitchRenderer(
                                almadarJson("label" to label, "checked" to ((raw as? JsonPrimitive)?.booleanOrNull ?: false), "disabled" to !editable, "name" to name, "onChange" to "document-field-toggle"),
                                Modifier
                            ) { _, envelope -> commit(name, envelope?.jsonObject("actionPayload")?.get("checked"), raw) }
                        } else if (type == "select") {
                            SelectRenderer(
                                almadarJson(
                                    "name" to name,
                                    "options" to JsonArray(options.map { almadarJson("value" to it, "label" to (field.jsonObject("labels")?.string(it, it) ?: it)) }),
                                    "value" to (almadarScalar(raw) ?: ""), "disabled" to !editable, "aria-label" to label, "onChange" to "document-field-select"
                                ),
                                Modifier
                            ) { _, envelope -> commit(name, envelope?.jsonObject("actionPayload")?.get("value"), raw) }
                        } else if (type == "readonly" || !editable) {
                            val items = if (raw is JsonArray) raw.toList() else if (raw is JsonObject) listOf(raw) else emptyList()
                            if (items.isNotEmpty()) {
                                val relationEvent = props.string("relationEvent")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                    items.forEach { item ->
                                        val text = chipLabel(item, field)
                                        if (relationEvent.isEmpty()) {
                                            AlmadarBadge(text)
                                        } else {
                                            AlmadarButton(
                                                props, text, relationEvent, onEvent,
                                                payload = almadarJson("field" to name, "id" to ((item as? JsonObject)?.string("id") ?: (almadarScalar(item) ?: "")), "name" to text),
                                                variant = "secondary", size = "sm"
                                            )
                                        }
                                    }
                                }
                            } else if (empty) {
                                AlmadarText("—", variant = "small", color = "secondary")
                            } else {
                                AlmadarText(fieldText(field), variant = "small")
                            }
                        } else if (editing == name) {
                            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                                AlmadarInput(
                                    name = name, value = draft, onChange = { draft = it }, modifier = Modifier.weight(1f),
                                    submitEvent = "document-field-commit",
                                    onSubmit = { typed -> commit(name, JsonPrimitive(typed.trim()), raw) }
                                )
                                AlmadarLocalButton(
                                    props, "", { commit(name, JsonPrimitive(draft.trim()), raw) },
                                    variant = "ghost", size = "sm", icon = "check", ariaLabel = "Save " + label
                                )
                            }
                        } else {
                            AlmadarText(
                                if (empty) "—" else fieldText(field),
                                modifier = Modifier.fillMaxWidth()
                                    .clickable(role = Role.Button) { draft = almadarScalar(raw) ?: ""; editing = name }
                                    .almadarTag(VerificationIds.field(name)),
                                variant = "small", color = if (empty) "muted" else "primary"
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier)
    }
}
