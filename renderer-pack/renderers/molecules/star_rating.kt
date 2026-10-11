package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

@Composable
fun StarRatingRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val value = props.float("value", 0f)
    val max = props.int("max", 5)
    val readOnly = props.bool("readOnly")
    val half = props.string("precision", "full") == "half"
    val action = props.string("action")
    val size = when (props.string("size", "md")) { "sm" -> "sm"; "lg" -> "xl"; else -> "lg" }
    val formatted = if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()
    val description = props.string("aria-label", props.string("label", "Rating: " + formatted + " out of " + max))
    val declared = props.jsonObject("actionPayload")
    val bare = JsonObject(((props as? JsonObject) ?: JsonObject(emptyMap())) - "actionPayload")
    Row(
        modifier = modifier.semantics { contentDescription = description; role = if (readOnly) Role.Image else Role.Button },
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until max) {
            val fill = (value - index).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.then(
                    if (readOnly) Modifier else Modifier
                        .pointerInput(index, half, action) {
                            detectTapGestures { offset ->
                                val next = if (half && offset.x < this.size.width / 2f) index + 0.5f else index + 1f
                                if (action.isNotEmpty()) {
                                    onEvent(action, eventEnvelope(bare, JsonObject((declared ?: JsonObject(emptyMap())) + mapOf("value" to JsonPrimitive(next)))))
                                }
                            }
                        }
                        .almadarActionTag(action, props)
                )
            ) {
                AlmadarGlyph(if (fill in 0.5f..0.999f) "star-half" else "star", color = if (fill >= 0.5f) "warning" else "muted", size = size)
            }
        }
    }
}
