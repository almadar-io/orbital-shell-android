package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.selection.selectableGroup

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LikertScaleRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val defaults = listOf("Strongly Disagree", "Disagree", "Neutral", "Agree", "Strongly Agree")
    val declared = props.array("options").mapNotNull { it as? JsonObject }
    val options: List<Pair<JsonElement, String>> = if (declared.isEmpty()) {
        defaults.mapIndexed { index, label -> JsonPrimitive(index + 1) to label }
    } else {
        declared.map { (it["value"] ?: JsonPrimitive("")) to it.string("label") }
    }
    val changeEvent = props.string("changeEvent")
    val disabled = props.bool("disabled")
    val incoming = almadarScalar((props as? JsonObject)?.get("value")) ?: ""
    var selected by remember(incoming) { mutableStateOf(incoming) }
    val pick = { value: JsonElement ->
        selected = almadarScalar(value) ?: ""
        if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, JsonObject(mapOf("value" to value))))
    }
    Column(modifier = modifier.fillMaxWidth().selectableGroup().almadarLabel(props.string("aria-label", props.string("question", "Likert scale"))), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
        if (props.string("question").isNotEmpty()) AlmadarText(props.string("question"), weight = "medium")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
            options.forEach { (value, label) ->
                val text = almadarScalar(value) ?: ""
                if (props.string("variant", "radios") == "buttons") {
                    AlmadarButton(
                        props, label.ifEmpty { text }, changeEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                        { _, _ -> pick(value) },
                        Modifier.weight(1f),
                        variant = if (selected == text) "primary" else "secondary", size = "sm", disabled = disabled, local = changeEvent.isEmpty()
                    )
                } else {
                    AlmadarRadio(props, label.ifEmpty { text }, selected == text, changeEvent, { pick(value) }, disabled = disabled)
                }
            }
        }
    }
}
