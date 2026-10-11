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
fun RichTextEditorRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val changeEvent = props.string("changeEvent")
    val incoming = props.string("value")
    var value by remember(incoming) { mutableStateOf(incoming) }
    val commit = { next: String ->
        value = next
        if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, JsonObject(mapOf("value" to JsonPrimitive(next)))))
    }
    val tools = listOf(
        Triple("bold", "Bold", "**text**"), Triple("italic", "Italic", "*text*"), Triple("code", "Code", "`code`"),
        Triple("heading-1", "Heading 1", "\n# "), Triple("heading-2", "Heading 2", "\n## "), Triple("list", "Bullet list", "\n- "),
        Triple("list-ordered", "Numbered list", "\n1. "), Triple("quote", "Quote", "\n> "), Triple("minus", "Divider", "\n---\n")
    )
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
        if (props.bool("readOnly")) {
            MarkdownContentRenderer(almadarJson("content" to value), Modifier.fillMaxWidth(), onEvent)
        } else {
            if (props.bool("showToolbar", true)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    tools.forEach { (icon, label, snippet) ->
                        AlmadarLocalButton(props, "", { commit(value + snippet) }, variant = "ghost", size = "sm", icon = icon, ariaLabel = label)
                    }
                }
                AlmadarDivider()
            }
            InputRenderer(
                almadarJson(
                    "value" to value, "placeholder" to props.string("placeholder"), "inputType" to "textarea",
                    "onChange" to changeEvent.ifEmpty { ALMADAR_LOCAL_ACTION }, "local" to changeEvent.isEmpty(), "sourceTrait" to almadarTrait(props)
                ),
                Modifier.fillMaxWidth()
            ) { _, envelope -> commit(envelope?.jsonObject("actionPayload")?.string("value", "") ?: "") }
        }
    }
}
