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

@Composable
fun ActivationBlockRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val saved = props.string("savedResponse")
    val saveEvent = props.string("saveEvent", "SAVE_ACTIVATION")
    var response by remember(saved) { mutableStateOf(saved) }
    var expanded by remember(saved) { mutableStateOf(saved.isEmpty()) }
    val palette = LocalAlmadarTheme.current.color
    val shape = almadarShape(AlmadarCorner.Container)
    val save: (String) -> Unit = { text ->
        onEvent(saveEvent, eventEnvelope(props, almadarJson("response" to JsonPrimitive(text))))
        expanded = false
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.primary.copy(alpha = 0.1f), shape)
            .then(almadarBorder(AlmadarCorner.Container, LocalAlmadarGeometry.current.borderHeavy, palette.primary))
            .padding(almadarSpace("space5"))
            .almadarLabel(props.string("aria-label")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
        verticalAlignment = Alignment.Top
    ) {
        AlmadarGlyph("lightbulb", color = "primary", size = "lg")
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            AlmadarText("Before You Begin...", variant = "h4", color = "primary")
            AlmadarText(props.string("question"), color = "primary")
            if (expanded) {
                TextareaRenderer(
                    almadarJson(
                        "value" to response, "placeholder" to "Jot down your thoughts...", "rows" to 3,
                        "onChange" to ALMADAR_LOCAL_ACTION, "local" to true, "sourceTrait" to almadarTrait(props)
                    ),
                    Modifier.fillMaxWidth()
                ) { _, envelope -> response = envelope?.jsonObject("actionPayload")?.string("value", response) ?: response }
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                    AlmadarButton(props, "Continue to Lesson →", saveEvent, { _, _ -> save(response) }, size = "sm")
                    AlmadarButton(props, "Skip for now", saveEvent, { _, _ -> save("") }, variant = "ghost", size = "sm")
                }
            } else {
                AlmadarLocalButton(props, "✓ Answered · Edit response", { expanded = true }, variant = "ghost", size = "sm")
            }
        }
    }
}
