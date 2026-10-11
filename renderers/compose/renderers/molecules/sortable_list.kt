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
fun SortableListRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val items = almadarRows((props as? JsonObject)?.get("items"))
    val reorderEvent = props.string("reorderEvent")
    val handleLeft = props.string("dragHandlePosition", "left") != "right"
    val template = (props as? JsonObject)?.get("renderItem") as? JsonObject
    val declared = props.jsonObject("reorderPayload")
    var order by remember(items) { mutableStateOf(items) }
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        order.forEachIndexed { index, row ->
            val id = row.string("id", index.toString())
            val fields = almadarFields(row.entries.filter { it.key != "id" && it.value is JsonPrimitive }.map { JsonPrimitive(it.key) })
            val move = { to: Int ->
                val next = order.toMutableList()
                next.add(to, next.removeAt(index))
                order = next
                onEvent(
                    reorderEvent,
                    eventEnvelope(props, JsonObject((declared ?: JsonObject(emptyMap())) + mapOf("fromIndex" to JsonPrimitive(index), "toIndex" to JsonPrimitive(to), "item" to row)))
                )
            }
            val controls: @Composable () -> Unit = {
                Column {
                    AlmadarButton(
                        props, "", reorderEvent, { _, _ -> move(index - 1) },
                        payload = almadarJson("fromIndex" to index, "toIndex" to index - 1, "item" to row),
                        variant = "ghost", icon = "chevron-up", size = "sm", ariaLabel = "Move up", disabled = index == 0
                    )
                    AlmadarButton(
                        props, "", reorderEvent, { _, _ -> move(index + 1) },
                        payload = almadarJson("fromIndex" to index, "toIndex" to index + 1, "item" to row),
                        variant = "ghost", icon = "chevron-down", size = "sm", ariaLabel = "Move down", disabled = index == order.lastIndex
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().verificationContainer(VerificationIds.row(id)),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (handleLeft) controls()
                if (template != null) RenderChild(template, Modifier.weight(1f), onEvent)
                else AlmadarRecordFields(fields, row, Modifier.weight(1f))
                if (!handleLeft) controls()
            }
        }
    }
}
