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
fun TagInputRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val variant = props.string("variant", "default")
    val unique = props.bool("unique", true)
    val disabled = props.bool("disabled")
    val addEvent = props.string("addEvent")
    val removeEvent = props.string("removeEvent")
    val helper = props.string("helperText")
    val incoming = props.array("value").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    var tags by remember(incoming) { mutableStateOf(incoming) }
    var draft by remember { mutableStateOf("") }
    val tagsPayload = { list: List<String> -> JsonArray(list.map { JsonPrimitive(it) }) }
    val commit = { text: String ->
        val tag = text.trim()
        if (tag.isNotEmpty() && !(unique && tags.contains(tag))) {
            val next = tags + tag
            tags = next
            if (addEvent.isNotEmpty()) onEvent(addEvent, eventEnvelope(props, almadarJson("tag" to tag, "value" to tagsPayload(next))))
        }
        draft = ""
    }
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                tags.forEachIndexed { index, tag ->
                    AlmadarBadge(
                        tag, variant = variant,
                        removeEvent = if (disabled) "" else removeEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                        local = removeEvent.isEmpty(),
                        removeLabel = "Remove " + tag,
                        onEvent = { _, _ ->
                            val next = tags.filterIndexed { position, _ -> position != index }
                            tags = next
                            if (removeEvent.isNotEmpty()) {
                                onEvent(removeEvent, eventEnvelope(props, almadarJson("tag" to tag, "index" to index, "value" to tagsPayload(next))))
                            }
                        }
                    )
                }
            }
        }
        AlmadarInput(
            name = "tag",
            value = draft,
            onChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = props.string("placeholder", "Add a tag"),
            disabled = disabled,
            submitEvent = "add-tag",
            onSubmit = { commit(it) }
        )
        if (helper.isNotEmpty()) AlmadarText(helper, variant = "caption", color = "muted")
    }
}
