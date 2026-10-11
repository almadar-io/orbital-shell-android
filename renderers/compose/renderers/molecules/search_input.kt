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

import kotlinx.coroutines.delay

@Composable
fun SearchInputRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val event = props.string("event")
    val entity = props.string("entity")
    val query = props.string("query")
    val debounceMs = props.int("debounceMs", 300)
    val loading = props.bool("isLoading")
    val incoming = props.string("value")
    var term by remember(incoming) { mutableStateOf(incoming) }
    var armed by remember { mutableStateOf(false) }
    val payload = { text: String ->
        almadarJson("searchTerm" to text, "entity" to entity, "query" to query)
    }
    LaunchedEffect(term, armed) {
        if (armed) {
            delay(debounceMs.toLong())
            if (event.isNotEmpty() && event != "SEARCH") onEvent(event, eventEnvelope(props, payload(term)))
            if (event.isNotEmpty() || entity.isNotEmpty() || query.isNotEmpty()) onEvent("SEARCH", eventEnvelope(props, payload(term)))
        }
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        AlmadarInput(
            name = "search",
            value = term,
            onChange = { typed ->
                armed = true
                if (typed.isEmpty() && term.isNotEmpty() && (event.isNotEmpty() || query.isNotEmpty())) {
                    onEvent("CLEAR_SEARCH", eventEnvelope(props, payload("")))
                }
                term = typed
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = props.string("placeholder", "Search"),
            leftIcon = "search",
            clearable = props.bool("clearable", true) && !loading,
            ariaLabel = props.string("aria-label", "Search")
        )
        if (loading) AlmadarSpinner(Modifier.padding(end = almadarSpaceFor("md")), "sm", "Searching")
    }
}
