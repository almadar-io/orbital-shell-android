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
import androidx.compose.ui.draw.clip

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size

@Composable
fun ReflectionBlockRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val saved = props.string("savedNote")
    val saveEvent = props.string("saveEvent", "SAVE_REFLECTION")
    var note by remember(saved) { mutableStateOf(saved) }
    var expanded by remember { mutableStateOf(false) }
    val palette = LocalAlmadarTheme.current.color
    val shape = almadarShape(AlmadarCorner.Container)
    val heavy = LocalAlmadarGeometry.current.borderHeavy
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.warning.copy(alpha = 0.1f))
            .drawBehind { drawRect(palette.warning, size = Size(heavy.toPx(), size.height)) }
            .padding(almadarSpace("space4"))
            .almadarLabel(props.string("aria-label")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
        verticalAlignment = Alignment.Top
    ) {
        AlmadarGlyph("pause-circle", color = "warning")
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            AlmadarText("Pause & Reflect", color = "warning", weight = "medium")
            AlmadarText(props.string("prompt"), variant = "body2")
            if (expanded) {
                TextareaRenderer(
                    almadarJson(
                        "value" to note, "placeholder" to "Your thoughts...", "rows" to 2,
                        "onChange" to ALMADAR_LOCAL_ACTION, "local" to true, "sourceTrait" to almadarTrait(props)
                    ),
                    Modifier.fillMaxWidth()
                ) { _, envelope -> note = envelope?.jsonObject("actionPayload")?.string("value", note) ?: note }
                AlmadarButton(
                    props, "Save & Continue", saveEvent,
                    { _, _ ->
                        onEvent(saveEvent, eventEnvelope(props, almadarJson("index" to props.int("index"), "note" to JsonPrimitive(note))))
                        expanded = false
                    },
                    variant = "secondary", size = "sm"
                )
            } else {
                AlmadarLocalButton(props, if (saved.isEmpty()) "Answer this question" else "✓ Answered · Edit", { expanded = true }, variant = "ghost", size = "sm")
            }
        }
    }
}
