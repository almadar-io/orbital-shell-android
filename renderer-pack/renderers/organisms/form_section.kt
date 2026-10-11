package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

@Composable
fun FormSectionRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val initialData = props.jsonObject("initialData")
    val sections = props.array("sections").mapNotNull { it as? JsonObject }
    val flatFields = props.array("fields").mapNotNull { it as? JsonObject }
    val allFields = flatFields + sections.flatMap { section -> section.array("fields").mapNotNull { it as? JsonObject } }
    val submitEvent = props.string("submitEvent", "SAVE")
    val cancelEvent = props.string("cancelEvent", "CANCEL")
    val onSubmit = props.string("onSubmit")
    val onCancel = props.string("onCancel")
    val loading = props.bool("isLoading")
    val showSubmit = props.bool("showSubmit", true)
    val showCancel = props.bool("showCancel", true)
    val relations = props.jsonObject("relationsData")
    fun nameOf(field: JsonObject): String = field.string("name", field.string("field"))
    val values = remember(initialData) {
        mutableStateMapOf<String, String>().apply {
            allFields.forEach { field ->
                val name = nameOf(field)
                if (name.isNotEmpty()) put(name, initialData?.get(name)?.let { (it as? JsonPrimitive)?.contentOrNull } ?: field.string("defaultValue"))
            }
        }
    }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    fun optionsOf(field: JsonObject): JsonArray {
        val name = nameOf(field)
        val declared = (field["options"] as? JsonArray) ?: (relations?.get(name) as? JsonArray)
        val labels = field.jsonObject("labels")
        val raw = declared ?: (field["values"] as? JsonArray) ?: ((field.jsonObject("validation")?.get("enum")) as? JsonArray) ?: JsonArray(emptyList())
        return JsonArray(raw.map { item ->
            if (item is JsonObject) item else {
                val value = (item as? JsonPrimitive)?.contentOrNull ?: ""
                almadarJson("value" to value, "label" to (labels?.string(value, value) ?: value))
            }
        })
    }
    fun typed(field: JsonObject, value: String): JsonElement = when (field.string("inputType", field.string("type", "text"))) {
        "checkbox", "boolean" -> JsonPrimitive(value == "true")
        "number", "integer", "float", "currency", "money" -> value.toDoubleOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(value)
        else -> JsonPrimitive(value)
    }
    val submit: (String, JsonElement?) -> Unit = { _, _ ->
        val missing = allFields.filter { it.bool("required") && values[nameOf(it)].isNullOrBlank() }.map { nameOf(it) }
        if (missing.isNotEmpty()) {
            errors = missing.associateWith { "Required" }
            onEvent("VALIDATION_FAILED", eventEnvelope(props, almadarJson("submitEvent" to submitEvent, "missing" to JsonArray(missing.map { JsonPrimitive(it) }))))
        } else {
            errors = emptyMap()
            val merged = mutableMapOf<String, JsonElement>()
            initialData?.forEach { (key, value) -> merged[key] = value }
            allFields.forEach { field ->
                val name = nameOf(field)
                if (name.isNotEmpty()) merged[name] = typed(field, values[name] ?: "")
            }
            val payload = almadarJson("data" to JsonObject(merged))
            onEvent(submitEvent, eventEnvelope(props, payload))
            if (onSubmit.isNotEmpty()) onEvent(onSubmit, eventEnvelope(props, payload))
        }
    }
    val cancel: (String, JsonElement?) -> Unit = { _, _ ->
        onEvent(cancelEvent, eventEnvelope(props))
        onEvent("CLOSE", eventEnvelope(props))
        if (onCancel.isNotEmpty()) onEvent(onCancel, eventEnvelope(props))
    }
    @Composable
    fun renderField(field: JsonObject) {
        val name = nameOf(field)
        if (name.isNotEmpty()) {
            val label = field.string("label", almadarFieldLabel(name)) + (if (field.bool("required")) " *" else "")
            val kind = field.string("inputType", field.string("type", "text"))
            val locked = loading || field.bool("readonly") || field.bool("disabled")
            val hint = field.string("hint")
            val error = errors[name] ?: ""
            val current = values[name] ?: ""
            when {
                kind == "checkbox" || kind == "boolean" -> AlmadarCheckbox(
                    checked = current == "true", event = name, label = label, name = name, disabled = locked,
                    onToggle = { values[name] = it.toString() }
                )
                kind == "select" || kind == "enum" || kind == "relation" || field["options"] != null || field["values"] != null -> SelectRenderer(
                    almadarJson("name" to name, "label" to label, "value" to current, "options" to optionsOf(field), "onChange" to name, "error" to error, "placeholder" to field.string("placeholder")),
                    Modifier.fillMaxWidth()
                ) { _, envelope -> values[name] = envelope?.jsonObject("actionPayload")?.string("value", "") ?: "" }
                kind == "textarea" -> TextareaRenderer(
                    almadarJson("name" to name, "value" to current, "label" to label, "placeholder" to field.string("placeholder"), "helperText" to hint, "error" to error, "onChange" to name),
                    Modifier.fillMaxWidth()
                ) { _, envelope -> values[name] = envelope?.jsonObject("actionPayload")?.string("value", "") ?: "" }
                else -> AlmadarInput(
                    name = name, value = current, onChange = { values[name] = it }, modifier = Modifier.fillMaxWidth(),
                    label = label, placeholder = field.string("placeholder"), helper = hint, disabled = locked, error = error,
                    inputType = when (kind) { "email", "password", "url", "tel" -> kind; "number", "integer", "float", "currency", "money" -> "number"; else -> "text" }
                )
            }
        }
    }
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor(props.string("gap", "md")))) {
        val heading = props.string("title")
        if (heading.isNotEmpty()) AlmadarText(heading, variant = "h4")
        props.jsonObject("error")?.let { AlmadarErrorText(props) }
        if (sections.isNotEmpty()) {
            sections.forEach { section ->
                AlmadarText(section.string("title"), variant = "h5")
                section.array("fields").forEach { (it as? JsonObject)?.let { field -> renderField(field) } }
            }
        }
        flatFields.forEach { renderField(it) }
        props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        if (loading) AlmadarSpinner(Modifier.align(Alignment.CenterHorizontally), "sm", "Saving")
        if (showSubmit || showCancel) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"), Alignment.End)) {
                if (showCancel) AlmadarButton(props, props.string("cancelLabel", "Cancel"), cancelEvent, cancel, variant = "secondary", disabled = loading)
                if (showSubmit) AlmadarButton(props, props.string("submitLabel", "Save"), submitEvent, submit, loading = loading)
            }
        }
    }
}
