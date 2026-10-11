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
fun ViolationAlertRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val violation = props.jsonObject("violation")
    val actionType = violation?.string("actionType", "measure") ?: "measure"
    val severity = props.string("severity", if (actionType == "measure") "warning" else "error")
    val icon = when (actionType) { "admin" -> "alert-circle"; "penalty" -> "shield-alert"; else -> "alert-triangle" }
    val typeLabel = when (actionType) { "admin" -> "Administrative Action"; "penalty" -> "Penalty Proceedings"; else -> "Corrective Measure" }
    val message = violation?.string("message") ?: props.string("message", "Violation")
    val citation = (violation?.string("law") ?: "") + " Art. " + (violation?.string("article") ?: "")
    val line = { text: String, variant: String, color: String ->
        almadarJson("type" to "typography", "content" to text, "variant" to variant, "color" to color)
    }
    val notes = mutableListOf<JsonElement>()
    if (!props.bool("compact")) notes.add(line(typeLabel, "caption", "muted"))
    notes.add(line(message, "body2", "primary"))
    if (!props.bool("compact")) {
        violation?.string("adminAction")?.takeIf { it.isNotEmpty() }?.let { notes.add(line("Admin: " + it, "caption", "primary")) }
        violation?.string("penaltyAction")?.takeIf { it.isNotEmpty() }?.let { notes.add(line("Penalty: " + it, "caption", "primary")) }
    }
    AlertRenderer(
        almadarJson(
            "variant" to severity, "icon" to icon, "title" to citation, "dismissible" to props.bool("dismissible"),
            "children" to JsonArray(notes), "aria-label" to props.string("aria-label"), "sourceTrait" to almadarTrait(props)
        ),
        modifier,
        onEvent
    )
}
