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

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AvlStateMachineRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val trait = props.jsonObject("trait") ?: JsonObject(emptyMap())
    val states = trait.array("states").mapNotNull { it as? JsonObject }
    val transitions = trait.array("transitions").mapNotNull { it as? JsonObject }
    val activeState = props.string("activeState")
    val root = props as? JsonObject
    val activeTransition = (root?.get("activeTransition") as? JsonPrimitive)?.intOrNull
    val visited = almadarStrings(root?.get("visitedStates"))
    val selectedState = props.string("selectedState")
    val pendingState = props.string("pendingSourceState")
    val stateEvent = props.string("stateClickEvent")
    val transitionEvent = props.string("transitionClickEvent")
    val annotations = props.jsonObject("annotations")
    val fields = almadarStrings(root?.get("entityFields"))
    val direction = if (props.string("direction", "ltr") == "rtl") LayoutDirection.Rtl else LayoutDirection.Ltr
    val noteText: (JsonObject?) -> String = { note ->
        if (note == null) "" else listOf(note.string("title"), note.string("body")).filter { it.isNotEmpty() }.joinToString(" - ")
    }
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Column(
            modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
        ) {
            if (props.bool("showHeader", true)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                ) {
                    AlmadarText(trait.string("name"), variant = "body1", weight = "semibold")
                    AlmadarText(
                        "on " + trait.string("linkedEntity") + " · " + states.size + " states · " + transitions.size + " transitions",
                        variant = "caption", color = "muted"
                    )
                    if (fields.isNotEmpty()) AlmadarMonoText(trait.string("linkedEntity") + " { " + fields.joinToString(", ") + " }", color = palette.mutedForeground)
                    almadarStrings(trait["listenedEvents"]).forEach { AlmadarBadge("listens " + it, variant = "neutral") }
                    almadarStrings(trait["emittedEvents"]).forEach { AlmadarBadge("emits " + it, variant = "neutral") }
                }
            }
            if (states.isEmpty()) {
                AlmadarPanel(muted = true) {
                    AlmadarText("No state machine", modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("lg")), variant = "caption", color = "muted", align = "center")
                }
            } else {
                AlmadarText("States", variant = "overline", color = "muted")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
                ) {
                    states.forEach { state ->
                        val name = state.string("name")
                        val active = name == activeState
                        val selected = name == selectedState
                        val pending = name == pendingState
                        val visit = visited.indexOf(name) + 1
                        val note = noteText(annotations?.jsonObject("states")?.jsonObject(name))
                        Column(
                            modifier = Modifier.verificationContainer(VerificationIds.row(name)),
                            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                                if (state.bool("isInitial")) AlmadarGlyph("arrow-right", color = "muted", size = "xs", label = "Initial state")
                                if (stateEvent.isNotEmpty()) {
                                    AlmadarButton(
                                        props, name, stateEvent, onEvent,
                                        payload = almadarJson("stateId" to name),
                                        variant = if (active || selected || pending) "primary" else "secondary", size = "sm"
                                    )
                                } else {
                                    AlmadarBadge(name, variant = if (active || selected || pending) "primary" else "neutral", size = "md")
                                }
                                if (state.bool("isTerminal")) AlmadarGlyph("circle-check", color = "muted", size = "xs", label = "Terminal state")
                                if (active) AlmadarBadge("Now", variant = "primary")
                                else if (visit > 0) AlmadarBadge(visit.toString(), variant = "neutral")
                            }
                            if (note.isNotEmpty()) AlmadarText(note, variant = "caption", color = "muted")
                        }
                    }
                }
                AlmadarText("Transitions", variant = "overline", color = "muted")
                Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                    transitions.forEach { transition ->
                        val index = transition.int("index")
                        val event = transition.string("event")
                        val fired = activeTransition == index
                        val next = !fired && activeState.isNotEmpty() && transition.string("from") == activeState
                        val guard = transition["guard"]?.takeIf { it !is JsonNull }
                        val effects = transition.array("effects").mapNotNull { it as? JsonObject }
                        val note = noteText(annotations?.jsonObject("transitions")?.jsonObject(event))
                        Column(
                            modifier = Modifier.fillMaxWidth().verificationContainer(VerificationIds.row("transition-" + index)),
                            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                            ) {
                                AlmadarBadge(transition.string("from"), variant = "neutral")
                                AlmadarGlyph("arrow-right", color = "muted", size = "xs")
                                if (transitionEvent.isNotEmpty()) {
                                    AlmadarButton(
                                        props, event, transitionEvent, onEvent,
                                        payload = almadarJson("index" to index, "event" to event, "from" to transition.string("from"), "to" to transition.string("to")),
                                        variant = if (fired) "primary" else if (next) "secondary" else "ghost", size = "sm",
                                        icon = if (guard != null) "diamond" else ""
                                    )
                                } else {
                                    AlmadarBadge(event, variant = if (fired) "primary" else "default", icon = if (guard != null) "diamond" else "")
                                }
                                AlmadarGlyph("arrow-right", color = "muted", size = "xs")
                                AlmadarBadge(transition.string("to"), variant = "neutral")
                                effects.forEach { AlmadarBadge(it.string("type"), variant = "neutral") }
                            }
                            if (guard != null) AlmadarMonoText(almadarSExpr(guard), color = if (fired) palette.primary else palette.mutedForeground)
                            if (note.isNotEmpty()) AlmadarText(note, variant = "caption", color = "muted")
                        }
                    }
                }
            }
        }
    }
}
