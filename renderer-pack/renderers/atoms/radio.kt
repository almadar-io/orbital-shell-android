package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role

@Composable
fun RadioRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val action = props.string("action")
    val name = props.string("name")
    val label = props.string("label")
    val helper = props.string("helperText")
    val error = props.string("error")
    val disabled = props.bool("disabled")
    val options = props.array("options").map { it.jsonPrimitive.content }
    val incoming = props.string("value")
    var selected by remember(incoming) { mutableStateOf(incoming) }
    val tagId = if (name.isNotEmpty()) almadarFieldId(name, props) else if (action.isNotEmpty()) almadarActionId(action, props) else ""
    val pick = { next: String ->
        selected = next
        if (action.isNotEmpty()) onEvent(action, eventEnvelope(props, JsonObject(mapOf("value" to JsonPrimitive(next)))))
    }
    val colors = RadioButtonDefaults.colors(
        selectedColor = if (error.isNotEmpty()) palette.error else palette.primary,
        unselectedColor = if (error.isNotEmpty()) palette.error else palette.border
    )
    val entries = if (options.isNotEmpty()) options else listOf(incoming.ifEmpty { label })
    Column(modifier = modifier.selectableGroup().almadarTag(tagId).almadarLabel(props.string("aria-label", label))) {
        entries.forEach { option ->
            Row(
                modifier = Modifier.selectable(
                    selected = selected == option,
                    enabled = !disabled,
                    role = Role.RadioButton,
                    onClick = { pick(option) }
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == option, onClick = null, enabled = !disabled, colors = colors)
                Text(
                    text = if (options.isNotEmpty()) option else label,
                    style = almadarType("sm").copy(fontWeight = FontWeight.Medium),
                    color = if (error.isNotEmpty()) palette.error else palette.foreground,
                    modifier = Modifier.padding(start = almadarSpaceFor("sm"))
                )
            }
        }
        if (error.isNotEmpty()) Text(text = error, style = almadarType("xs"), color = palette.error)
        else if (helper.isNotEmpty()) Text(text = helper, style = almadarType("xs"), color = palette.mutedForeground)
    }
}
