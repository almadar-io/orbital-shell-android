package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// tier: molecules — a grouped container styled by tokens only

private fun paletteMatches(query: String, text: String): Boolean {
    if (query.isEmpty()) return true
    var index = 0
    val needle = query.lowercase()
    val haystack = text.lowercase()
    for (ch in haystack) {
        if (index < needle.length && ch == needle[index]) index++
    }
    return index == needle.length
}

private fun paletteCommandMatches(command: JsonObject, query: String): Boolean =
    paletteMatches(query, command.string("label")) || almadarStrings(command["keywords"]).any { paletteMatches(query, it) }

@Composable
fun CommandPaletteRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val openEvent = props.string("onOpenChange")
    val selectEvent = props.string("onSelect")
    var query by remember { mutableStateOf("") }
    val commands = props.array("commands").mapNotNull { it as? JsonObject }
    val filtered = commands.filter { paletteCommandMatches(it, query) }
    val close = {
        query = ""
        if (openEvent.isNotEmpty()) onEvent(openEvent, eventEnvelope(props, almadarJson("open" to false)))
    }
    if (!props.bool("open")) {
        Box(modifier = modifier)
    } else {
        val placeholder = props.string("placeholder", "Type a command...")
        AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", placeholder))) {
            Column(modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("md")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                InputRenderer(
                    almadarJson(
                        "value" to query, "placeholder" to placeholder, "inputType" to "search", "leftIcon" to "search",
                        "clearable" to true, "onChange" to ALMADAR_LOCAL_ACTION, "local" to true
                    ),
                    Modifier.fillMaxWidth()
                ) { _, envelope -> query = envelope?.jsonObject("actionPayload")?.string("value", "") ?: "" }
                if (filtered.isEmpty()) {
                    AlmadarText(props.string("emptyLabel", "No matching commands"), Modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")), color = "muted", align = "center")
                } else {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = AlmadarSizes.w80).verticalScroll(rememberScrollState())) {
                        filtered.groupBy { it.string("group") }.forEach { (group, items) ->
                            if (group.isNotEmpty()) {
                                AlmadarText(group, Modifier.padding(horizontal = almadarSpaceFor("sm")).padding(top = almadarSpaceFor("xs")), variant = "caption", color = "muted")
                            }
                            items.forEach { command ->
                                val event = command.string("event")
                                val action = command.string("action")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AlmadarButton(
                                        props, command.string("label"), action.ifEmpty { event }.ifEmpty { ALMADAR_LOCAL_ACTION },
                                        { _, _ ->
                                            if (event.isNotEmpty()) onEvent(event, eventEnvelope(props, almadarJson("commandId" to command.string("id"))))
                                            if (action.isNotEmpty()) onEvent(action, eventEnvelope(props, command.jsonObject("actionPayload") ?: JsonObject(emptyMap())))
                                            if (selectEvent.isNotEmpty()) onEvent(selectEvent, eventEnvelope(props, almadarJson("command" to command)))
                                            close()
                                        },
                                        modifier = Modifier.weight(1f),
                                        variant = "ghost", icon = command.string("icon"),
                                        disabled = command.bool("disabled"), local = action.isEmpty() && event.isEmpty()
                                    )
                                    if (command.string("shortcut").isNotEmpty()) AlmadarBadge(command.string("shortcut"), variant = "neutral")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
