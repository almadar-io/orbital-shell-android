package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

// tier: molecules — a grouped container styled by tokens only

@Composable
fun SequenceBarRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val slots = props.array("slots")
    val feedback = props.array("slotFeedback")
    val playing = props.bool("playing")
    val step = props.int("currentStep", -1)
    fun named(event: String, callback: String): String = props.string(event).ifEmpty { props.string(callback) }
    val dropEvent = named("slotDropEvent", "onSlotDrop")
    val removeEvent = named("slotRemoveEvent", "onSlotRemove")
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(almadarSpace("space2")).almadarLabel(props.string("aria-label")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxOf(props.int("maxSlots"), 0)) { index ->
            val item = slots.getOrNull(index) as? JsonObject
            if (index > 0) AlmadarText("→", variant = "large", color = if (step >= 0 && index <= step) "primary" else "muted")
            TraitSlotRenderer(
                JsonObject(
                    buildMap {
                        put("slotNumber", JsonPrimitive(index + 1))
                        put("size", JsonPrimitive(props.string("size", "lg")))
                        put("locked", JsonPrimitive(playing))
                        put("selected", JsonPrimitive(step == index))
                        put("draggable", JsonPrimitive(!playing && item != null))
                        put("feedback", JsonPrimitive((feedback.getOrNull(index) as? JsonPrimitive)?.contentOrNull ?: ""))
                        put("dropEvent", JsonPrimitive(dropEvent))
                        put("removeEvent", JsonPrimitive(if (item == null) "" else removeEvent))
                        if (item != null) put("equippedItem", item)
                        root?.get("categoryColors")?.let { put("categoryColors", it) }
                        almadarTrait(props)?.let { put("sourceTrait", it) }
                    }
                ),
                Modifier
            ) { name, envelope ->
                if (!playing) {
                    if (name == dropEvent) onEvent(name, eventEnvelope(props, almadarJson("slotNumber" to index, "itemId" to (envelope?.jsonObject("actionPayload")?.string("itemId") ?: ""))))
                    else if (name == removeEvent) onEvent(name, eventEnvelope(props, almadarJson("slotNumber" to index)))
                }
            }
        }
    }
}
