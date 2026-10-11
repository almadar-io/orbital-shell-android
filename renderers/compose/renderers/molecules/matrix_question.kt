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
fun MatrixQuestionRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val rows = props.array("rows").mapNotNull { it as? JsonObject }
    val defaults = listOf("Strongly Disagree", "Disagree", "Neutral", "Agree", "Strongly Agree")
    val declared = props.array("columns").mapNotNull { it as? JsonObject }
    val columns: List<Pair<JsonElement, String>> = if (declared.isEmpty()) {
        defaults.mapIndexed { index, label -> JsonPrimitive(index + 1) to label }
    } else {
        declared.map { (it["value"] ?: JsonPrimitive("")) to it.string("label") }
    }
    val changeEvent = props.string("changeEvent")
    val disabled = props.bool("disabled")
    val incoming = (props as? JsonObject)?.get("values") as? JsonObject
    var values by remember(incoming) { mutableStateOf(incoming?.mapValues { almadarScalar(it.value) ?: "" } ?: emptyMap()) }
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", props.string("title"))), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
        if (props.string("title").isNotEmpty()) AlmadarText(props.string("title"), variant = "h4")
        rows.forEachIndexed { index, row ->
            val rowId = row.string("id")
            Column(
                modifier = Modifier.fillMaxWidth().verificationContainer(VerificationIds.row(rowId)),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
            ) {
                AlmadarText(row.string("label"), variant = "small", weight = "medium")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    columns.forEach { (value, label) ->
                        val text = almadarScalar(value) ?: ""
                        AlmadarRadio(
                            props, label.ifEmpty { text }, values[rowId] == text, changeEvent,
                            {
                                values = values + (rowId to text)
                                if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, JsonObject(mapOf("rowId" to JsonPrimitive(rowId), "value" to value))))
                            },
                            disabled = disabled
                        )
                    }
                }
                if (index < rows.lastIndex) AlmadarDivider()
            }
        }
    }
}
