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

@Composable
fun RelationSelectRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val options = props.array("options").mapNotNull { it as? JsonObject }
    val name = props.string("name")
    val incoming = props.string("value")
    var value by remember(incoming) { mutableStateOf(incoming) }
    var query by remember { mutableStateOf("") }
    val shown = if (query.isBlank()) options else options.filter {
        it.string("label").contains(query, ignoreCase = true) || it.string("description").contains(query, ignoreCase = true)
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        if (props.bool("isLoading")) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarSpinner(size = "sm", label = "Loading")
                AlmadarText("Loading...", color = "muted")
            }
        } else {
            AlmadarLocalInput(query, { query = it }, Modifier.fillMaxWidth(), placeholder = props.string("searchPlaceholder", "Search"), ariaLabel = props.string("searchPlaceholder", "Search"))
            if (shown.isEmpty()) {
                AlmadarText(props.string("emptyMessage", "No options found"), variant = "small", color = "muted")
            } else {
                SelectRenderer(
                    almadarJson(
                        "name" to name,
                        "options" to JsonArray(shown.map { almadarJson("value" to it["value"], "label" to it.string("label"), "disabled" to it.bool("disabled")) }),
                        "value" to value, "placeholder" to props.string("placeholder", "Select..."), "error" to props.string("error"),
                        "clearable" to props.bool("clearable", true), "disabled" to props.bool("disabled"),
                        "onChange" to name.ifEmpty { ALMADAR_LOCAL_ACTION }, "local" to name.isEmpty(), "sourceTrait" to almadarTrait(props)
                    ),
                    Modifier.fillMaxWidth()
                ) { event, envelope ->
                    value = envelope?.jsonObject("actionPayload")?.string("value", "") ?: ""
                    if (name.isNotEmpty()) onEvent(event, eventEnvelope(props, envelope?.jsonObject("actionPayload")))
                }
            }
        }
    }
}
