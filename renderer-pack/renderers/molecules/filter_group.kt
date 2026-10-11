package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FilterGroupRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val entityName = props.string("entity")
    val query = props.string("query")
    val event = props.string("event")
    val clearEvent = props.string("clearEvent")
    val variant = props.string("variant", "default")
    val filters = props.array("filters").mapNotNull { it as? JsonObject }
    var selected by remember { mutableStateOf(mapOf<String, String>()) }
    val choose = { field: String, value: String ->
        selected = if (value.isEmpty()) selected - field else selected + (field to value)
        val payload = almadarJson("entity" to entityName, "field" to field, "value" to value, "query" to query)
        if (event.isNotEmpty() && event != "FILTER") onEvent(event, eventEnvelope(props, payload))
        onEvent("FILTER", eventEnvelope(props, payload))
    }
    val clear = {
        selected = emptyMap()
        val payload = almadarJson("entity" to entityName, "query" to query)
        if (clearEvent.isNotEmpty() && clearEvent != "CLEAR_FILTERS") onEvent(clearEvent, eventEnvelope(props, payload))
        onEvent("CLEAR_FILTERS", eventEnvelope(props, payload))
    }
    val chooseAction = event.ifEmpty { "FILTER" }
    val clearAction = clearEvent.ifEmpty { "CLEAR_FILTERS" }
    val control: @Composable (JsonObject) -> Unit = { filter ->
        val field = filter.string("field")
        val label = filter.string("label", almadarFieldLabel(field))
        val kind = filter.string("filterType", filter.string("type", "select"))
        val options = (filter["options"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
            when (kind) {
                "toggle", "checkbox" -> AlmadarCheckbox(
                    checked = selected[field] == "true", event = chooseAction, label = label, name = field,
                    onToggle = { choose(field, if (it) "true" else "") }
                )
                "text", "date" -> AlmadarInput(
                    name = field, value = selected[field] ?: "", onChange = { choose(field, it) },
                    modifier = Modifier.widthIn(min = LocalAlmadarDensity.current.buttonHeightLg * 4),
                    placeholder = label, clearable = true
                )
                "daterange", "date-range", "numberrange", "number-range" -> {
                    val from = if (kind.startsWith("date")) "_from" else "_min"
                    val to = if (kind.startsWith("date")) "_to" else "_max"
                    AlmadarText(label + ":", variant = "small", color = "muted", weight = "medium")
                    AlmadarInput(
                        name = field + from, value = selected[field + from] ?: "", onChange = { choose(field + from, it) },
                        modifier = Modifier.widthIn(min = LocalAlmadarDensity.current.buttonHeightLg * 3), placeholder = "From", clearable = true
                    )
                    AlmadarInput(
                        name = field + to, value = selected[field + to] ?: "", onChange = { choose(field + to, it) },
                        modifier = Modifier.widthIn(min = LocalAlmadarDensity.current.buttonHeightLg * 3), placeholder = "To", clearable = true
                    )
                }
                else -> {
                    AlmadarText(label + ":", variant = "small", color = "muted", weight = "medium")
                    AlmadarButton(
                        props, "All", chooseAction, { _, _ -> choose(field, "") },
                        variant = if (selected[field] == null) "primary" else "secondary", size = "sm"
                    )
                    options.forEach { option ->
                        AlmadarButton(
                            props, option, chooseAction, { _, _ -> choose(field, option) },
                            variant = if (selected[field] == option) "primary" else "secondary", size = "sm"
                        )
                    }
                }
            }
        }
    }
    val clearButton: @Composable () -> Unit = {
        if (selected.isNotEmpty()) {
            AlmadarButton(props, "Clear", clearAction, { _, _ -> clear() }, variant = "ghost", icon = "x", size = "sm")
        }
    }
    if (props.string("look") != "inline-column-header") {
        if (variant == "vertical") {
            Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
                if (props.bool("showIcon", true)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                        AlmadarGlyph("filter", color = "muted", size = "sm")
                        AlmadarText("Filters", variant = "overline", color = "muted")
                    }
                }
                filters.forEach { control(it) }
                clearButton()
            }
        } else {
            FlowRow(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor(if (variant == "compact") "sm" else "md")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) {
                if (props.bool("showIcon", true)) AlmadarGlyph("filter", color = "muted", size = "sm")
                filters.forEach { control(it) }
                clearButton()
            }
        }
    }
}
