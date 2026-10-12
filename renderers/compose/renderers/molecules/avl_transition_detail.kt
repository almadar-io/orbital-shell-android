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
fun AvlTransitionDetailRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    // AvlTransitionDetail.tsx on a phone: the from → event → to header, the guard and each effect as text; with stepEvent, a Step button
    // asking the host to play this transition (UI:{stepEvent} { orbital, trait, from, event, payload }). Circuit boards and the payload editor are web-only.
    val transition = props.jsonObject("transition") ?: JsonObject(emptyMap())
    val orbital = props.string("orbital")
    val trait = props.string("trait")
    val stepEvent = props.string("stepEvent")
    val from = transition.string("from")
    val event = transition.string("event")
    val effects = transition.array("effects").mapNotNull { it as? JsonObject }
    val guardExpr = transition["guard"]
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
            AlmadarBadge(from, size = "lg")
            AlmadarText(event, weight = "semibold")
            AlmadarText("→", variant = "h5", color = "muted")
            AlmadarBadge(transition.string("to"), size = "lg")
        }
        if (stepEvent.isNotEmpty()) {
            AlmadarButton(props, "Step", stepEvent, onEvent, payload = JsonObject(mapOf("orbital" to JsonPrimitive(orbital), "trait" to JsonPrimitive(trait), "from" to JsonPrimitive(from), "event" to JsonPrimitive(event), "payload" to JsonObject(emptyMap()))), size = "sm", icon = "skip-forward")
        }
        if (guardExpr != null && guardExpr !is JsonNull) {
            AlmadarText("Guard", variant = "overline", color = "muted")
            AlmadarText(almadarSexpr(guardExpr), variant = "caption")
        }
        if (effects.isNotEmpty()) AlmadarText("Effects (${effects.size})", variant = "overline", color = "muted")
        effects.forEach { effect ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                AlmadarBadge(effect.string("type"), variant = "primary")
                AlmadarText(almadarSexpr(effect["args"]), variant = "caption", color = "muted")
            }
        }
    }
}
