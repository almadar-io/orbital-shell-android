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
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role

@Composable
fun CheckboxRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val incoming = props.bool("checked", props.bool("defaultChecked"))
    var checked by remember(incoming) { mutableStateOf(incoming) }
    val label = props.string("label")
    val error = props.string("error")
    val changeEvent = props.string("onChange")
    val name = props.string("name")
    val disabled = props.bool("disabled")
    val tagId = if (name.isNotEmpty()) almadarFieldId(name, props) else if (changeEvent.isNotEmpty()) almadarActionId(changeEvent, props) else ""
    val toggle = { next: Boolean ->
        checked = next
        if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, JsonObject(mapOf("checked" to JsonPrimitive(next)))))
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .toggleable(value = checked, enabled = !disabled, role = Role.Checkbox, onValueChange = toggle)
                .almadarTag(tagId)
                .almadarLabel(props.string("aria-label")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val extent = LocalAlmadarIconography.current.defaultSize
            Box(
                modifier = Modifier
                    .size(extent)
                    .almadarAlpha(if (disabled) ALMADAR_DISABLED_ALPHA else 1f)
                    .background(if (checked) palette.primary else palette.card, almadarShape(AlmadarCorner.None))
                    .then(almadarBorder(AlmadarCorner.None, LocalAlmadarGeometry.current.borderStandard, if (error.isNotEmpty()) palette.error else if (checked) palette.primary else palette.border)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) AlmadarIcon(name = "check", tint = palette.primaryForeground, modifier = Modifier.size(extent * 0.75f))
            }
            if (label.isNotEmpty()) {
                Text(
                    text = label + (if (props.bool("required")) " *" else ""),
                    style = almadarType("sm").copy(fontWeight = FontWeight.Medium),
                    color = if (error.isNotEmpty()) palette.error else palette.foreground
                )
            }
        }
        if (error.isNotEmpty()) Text(text = error, style = almadarType("xs"), color = palette.error)
    }
}
