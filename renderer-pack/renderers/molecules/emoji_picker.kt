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
fun EmojiPickerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val pickEvent = props.string("pickEvent")
    val position = props.string("position", "top")
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val needle = query.trim().lowercase()
    val matches = if (needle.isEmpty()) AlmadarEmoji.items else AlmadarEmoji.items.filter { item ->
        item.label.lowercase().contains(needle) || item.keywords.any { it.lowercase().contains(needle) }
    }
    val groups = remember(matches) {
        val out = mutableListOf<Pair<String, MutableList<AlmadarEmojiItem>>>()
        matches.forEach { item ->
            if (out.isNotEmpty() && out.last().first == item.category) out.last().second.add(item)
            else out.add(item.category to mutableListOf(item))
        }
        out
    }
    val searchLabel = props.string("searchPlaceholder", "Search emoji")
    val panel: @Composable () -> Unit = {
        AlmadarPanel(modifier = Modifier.width(AlmadarSizes.w80)) {
            Column(modifier = Modifier.padding(almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                AlmadarLocalInput(query, { query = it }, Modifier.fillMaxWidth(), placeholder = searchLabel, inputType = "search", ariaLabel = searchLabel)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(almadarSpaceFor("xl")),
                    modifier = Modifier.fillMaxWidth().heightIn(max = AlmadarSizes.w64).almadarFlexFill()
                ) {
                    groups.forEach { group ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AlmadarText(group.first, variant = "caption", color = "muted", weight = "semibold")
                        }
                        items(group.second.size) { cell ->
                            val entry = group.second[cell]
                            AlmadarButton(
                                props, entry.id, pickEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                                { name, _ ->
                                    open = false
                                    query = ""
                                    if (pickEvent.isNotEmpty()) onEvent(name, eventEnvelope(props, almadarJson("emoji" to entry.id)))
                                },
                                Modifier.size(almadarSpaceFor("xl")),
                                payload = almadarJson("emoji" to entry.id),
                                variant = "ghost", size = "sm", ariaLabel = entry.label, local = pickEvent.isEmpty()
                            )
                        }
                    }
                    if (groups.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AlmadarText(props.string("emptyMessage", "No emoji found"), variant = "small", color = "muted", align = "center")
                        }
                    }
                }
            }
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        if (open && position == "top") panel()
        AlmadarLocalButton(
            props, "", { open = !open },
            variant = "ghost", icon = props.string("triggerIcon", "smile"), ariaLabel = props.string("triggerLabel", "Add emoji")
        )
        if (open && position != "top") panel()
    }
}
