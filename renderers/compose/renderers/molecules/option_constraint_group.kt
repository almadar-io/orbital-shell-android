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
fun OptionConstraintGroupRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val groupId = props.string("groupId")
    val options = props.array("options").mapNotNull { it as? JsonObject }
    val constraint = props.jsonObject("constraint")
    val single = (constraint?.string("type", "single") ?: "single") == "single"
    val required = constraint?.bool("required") ?: false
    val min = constraint?.int("min") ?: 0
    val max = constraint?.int("max") ?: 0
    val changeEvent = props.string("changeEvent")
    val incoming = almadarStrings((props as? JsonObject)?.get("selected"))
    var selected by remember(incoming) { mutableStateOf(incoming) }
    val hint = if (single) {
        if (required) "Required, pick 1" else "Optional, pick up to 1"
    } else if (min > 0 && max > 0) {
        if (min == max) "Pick exactly " + min else "Pick " + min + "-" + max
    } else if (min > 0) "Pick at least " + min
    else if (max > 0) "Pick up to " + max
    else "Optional"
    val error = if (single) {
        if (required && selected.isEmpty()) "Pick 1 option" else if (selected.size > 1) "Pick only 1 option" else ""
    } else if (selected.size < min) "Pick at least " + (min - selected.size) + " more"
    else if (max > 0 && selected.size > max) "Remove " + (selected.size - max) + " options"
    else ""
    val pick = { id: String, checked: Boolean ->
        val next: List<String> = if (single) {
            if (checked) listOf(id) else emptyList()
        } else if (checked) {
            if (selected.contains(id)) selected else selected + id
        } else {
            selected.filter { it != id }
        }
        selected = next
        if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, almadarJson("selected" to JsonArray(next.map { JsonPrimitive(it) }))))
    }
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", props.string("title"))), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AlmadarText(props.string("title"), modifier = Modifier.weight(1f), variant = "label", weight = "semibold")
            AlmadarText(hint, variant = "caption", color = if (single && required) "error" else "muted")
        }
        if (props.string("description").isNotEmpty()) AlmadarText(props.string("description"), variant = "caption", color = "muted")
        options.forEach { option ->
            val id = option.string("id")
            val disabled = option.bool("disabled") || option.bool("outOfStock")
            val delta = option.float("priceDelta")
            Row(
                modifier = Modifier.fillMaxWidth().verificationContainer(VerificationIds.row(groupId + "-" + id)),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (single) {
                    AlmadarRadio(props, option.string("label"), selected.contains(id), changeEvent, { pick(id, true) }, Modifier.weight(1f), disabled)
                } else {
                    AlmadarCheckbox(
                        selected.contains(id), changeEvent.ifEmpty { ALMADAR_LOCAL_ACTION }, { pick(id, it) }, Modifier.weight(1f),
                        label = option.string("label"), disabled = disabled, local = changeEvent.isEmpty()
                    )
                }
                if (delta != 0f) {
                    AlmadarText((if (delta >= 0f) "+" else "-") + "$" + "%.2f".format(Math.abs(delta)), variant = "caption", color = "muted")
                }
                if (option.bool("outOfStock")) AlmadarBadge("Out of stock", variant = "warning")
            }
        }
        if (error.isNotEmpty()) AlmadarText(error, variant = "caption", color = "error")
    }
}
