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
fun DateRangePickerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val event = props.string("event")
    val initialFrom = props.string("from")
    val initialTo = props.string("to")
    var from by remember(initialFrom) { mutableStateOf(initialFrom) }
    var to by remember(initialTo) { mutableStateOf(initialTo) }
    var active by remember { mutableStateOf("") }
    val today = java.time.LocalDate.now()
    val tokens: Map<String, Pair<String, String>> = mapOf(
        "7d" to (today.minusDays(7).toString() to today.toString()),
        "30d" to (today.minusDays(30).toString() to today.toString()),
        "month" to (today.withDayOfMonth(1).toString() to today.toString()),
        "quarter" to (today.withMonth((today.monthValue - 1) / 3 * 3 + 1).withDayOfMonth(1).toString() to today.toString()),
        "ytd" to (today.withDayOfYear(1).toString() to today.toString())
    )
    val declared = props.array("presets").mapNotNull { it as? JsonObject }
    val presets = declared.map { it.string("value") to it.string("label") }.ifEmpty {
        listOf("7d" to "Last 7 days", "30d" to "Last 30 days", "month" to "This Month", "quarter" to "This Quarter", "ytd" to "YTD")
    }
    val ranges = declared.associate { preset ->
        preset.string("value") to (preset.jsonObject("range")?.let { it.string("from") to it.string("to") })
    }
    val commit = { nextFrom: String, nextTo: String ->
        from = nextFrom
        to = nextTo
        if (event.isNotEmpty()) onEvent(event, eventEnvelope(props, almadarJson("from" to nextFrom, "to" to nextTo)))
    }
    Column(modifier = modifier.almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
            AlmadarLocalInput(
                from, { active = ""; commit(it, to) }, Modifier.weight(1f),
                label = props.string("fromLabel", "From"), placeholder = "YYYY-MM-DD"
            )
            AlmadarLocalInput(
                to, { active = ""; commit(from, it) }, Modifier.weight(1f),
                label = props.string("toLabel", "To"), placeholder = "YYYY-MM-DD"
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
            presets.forEach { (value, label) ->
                val range = ranges[value] ?: tokens[value]
                if (range != null) {
                    AlmadarButton(
                        props, label, event.ifEmpty { ALMADAR_LOCAL_ACTION },
                        { _, _ -> active = value; commit(range.first, range.second) },
                        payload = almadarJson("from" to range.first, "to" to range.second),
                        variant = if (active == value) "primary" else "ghost", size = "sm", local = event.isEmpty()
                    )
                }
            }
        }
    }
}
