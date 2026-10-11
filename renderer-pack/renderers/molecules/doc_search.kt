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
fun DocSearchRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchEvent = props.string("onSearch")
    LaunchedEffect(query) {
        if (searchEvent.isNotEmpty() && query.isNotBlank()) {
            kotlinx.coroutines.delay(300)
            onEvent(searchEvent, eventEnvelope(props, almadarJson("query" to query.trim())))
        }
    }
    InputRenderer(
        almadarJson(
            "value" to query, "placeholder" to props.string("placeholder", "Search documentation..."), "inputType" to "search",
            "leftIcon" to "search", "clearable" to true, "aria-label" to props.string("aria-label"),
            "onChange" to ALMADAR_LOCAL_ACTION, "local" to true
        ),
        modifier.fillMaxWidth()
    ) { _, envelope -> query = envelope?.jsonObject("actionPayload")?.string("value", "") ?: "" }
}
