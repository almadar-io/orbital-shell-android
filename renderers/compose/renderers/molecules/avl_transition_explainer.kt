package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.background

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AvlTransitionExplainerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val transition = props.jsonObject("transition") ?: JsonObject(emptyMap())
    val annotations = props.jsonObject("annotations")
    val event = transition.string("event")
    val guard = transition["guard"]?.takeIf { it !is JsonNull }
    val effects = transition.array("effects").mapNotNull { it as? JsonObject }
    val eventNote = annotations?.jsonObject("transitions")?.jsonObject(event)
    val effectNotes = annotations?.jsonObject("effects")
    val noteText: (JsonObject?) -> String = { note ->
        if (note == null) "" else listOf(note.string("title"), note.string("body")).filter { it.isNotEmpty() }.joinToString(" - ")
    }
    val effectItems = JsonArray(effects.mapIndexed { index, effect ->
        val type = effect.string("type")
        val args = effect.array("args")
        val note = noteText(effectNotes?.jsonObject(type))
        almadarJson(
            "id" to "effect-" + index,
            "header" to almadarJson("type" to "badge", "label" to type, "variant" to "primary", "size" to "sm"),
            "content" to JsonArray(
                listOf(almadarJson("type" to "typography", "content" to almadarSExpr(JsonArray(listOf(JsonPrimitive(type)) + args)), "variant" to "caption")) +
                    (if (note.isEmpty()) emptyList() else listOf(almadarJson("type" to "typography", "content" to note, "variant" to "caption", "color" to "muted")))
            )
        )
    })
    AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        Column(modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("lg")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
            AlmadarText(props.string("orbitalName") + " / " + props.string("traitName"), variant = "caption", color = "muted")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
            ) {
                AlmadarBadge(transition.string("from"), variant = "neutral", size = "lg")
                AlmadarText(event, variant = "body1", weight = "semibold")
                AlmadarGlyph("arrow-right", color = "muted", size = "md")
                AlmadarBadge(transition.string("to"), variant = "neutral", size = "lg")
            }
            if (eventNote != null) AlmadarText(noteText(eventNote), variant = "caption", color = "muted")
            if (guard != null) {
                Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    AlmadarText("Guard", variant = "overline", color = "muted")
                    AlmadarText("The transition only fires while this holds.", variant = "caption", color = "muted")
                    AlmadarMonoText(almadarSExpr(guard), Modifier.fillMaxWidth().background(palette.muted).padding(almadarSpaceFor("sm")))
                }
            }
            if (effects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    AlmadarText("Effects (" + effects.size + ")", variant = "overline", color = "muted")
                    AlmadarText("Run in this order when the transition fires.", variant = "caption", color = "muted")
                    AccordionRenderer(
                        almadarJson("items" to effectItems, "multiple" to true, "defaultOpenItems" to JsonArray(effects.indices.map { JsonPrimitive("effect-" + it) })),
                        Modifier.fillMaxWidth(), AlmadarNoEvent
                    )
                }
            }
        }
    }
}
