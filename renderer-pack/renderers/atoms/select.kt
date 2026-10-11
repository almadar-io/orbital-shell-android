package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SelectRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val changeEvent = props.string("onChange")
    val valueEvent = props.string("onValueChange")
    val action = props.string("action")
    val name = props.string("name")
    val label = props.string("label")
    val placeholder = props.string("placeholder")
    val error = props.string("error")
    val multiple = props.bool("multiple")
    val clearable = props.bool("clearable")
    val options = props.array("options").map { Triple(it.string("value"), it.string("label", it.string("value")), !it.bool("disabled")) }
    val groups = props.array("groups").map { group -> group.string("label") to group.array("options").map { Triple(it.string("value"), it.string("label", it.string("value")), !it.bool("disabled")) } }
    val all = options + groups.flatMap { it.second }
    val incoming = (props as? JsonObject)?.get("value")
    val initial = when (incoming) {
        is JsonArray -> incoming.map { it.jsonPrimitive.content }
        is JsonPrimitive -> if (incoming.content.isEmpty()) emptyList() else listOf(incoming.content)
        else -> emptyList()
    }
    var selected by remember(incoming) { mutableStateOf(initial) }
    var expanded by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ownBorder = LocalAlmadarGeometry.current.borderStyleInteractive != CssBorderStyle.Solid
    val edge = if (error.isNotEmpty()) palette.error else if (focused) palette.ring else palette.input
    val display = selected.map { value -> all.firstOrNull { it.first == value }?.second ?: value }.joinToString(", ")
    val tagId = if (name.isNotEmpty()) almadarFieldId(name, props) else if (changeEvent.isNotEmpty()) almadarActionId(changeEvent, props) else if (action.isNotEmpty()) almadarActionId(action, props) else ""
    fun commit(next: List<String>) {
        selected = next
        val payload: JsonElement = if (multiple) JsonArray(next.map { JsonPrimitive(it) }) else JsonPrimitive(next.firstOrNull() ?: "")
        listOf(changeEvent, valueEvent, action).filter { it.isNotEmpty() }.forEach { event ->
            onEvent(event, eventEnvelope(props, JsonObject(mapOf("value" to payload))))
        }
    }
    @Composable
    fun Item(option: Triple<String, String, Boolean>) {
        val isSelected = selected.contains(option.first)
        DropdownMenuItem(
            text = { Text(option.second, style = almadarType("sm")) },
            enabled = option.third,
            leadingIcon = if (multiple) { { Checkbox(checked = isSelected, onCheckedChange = null) } } else null,
            trailingIcon = if (!multiple && isSelected) { { AlmadarIcon(name = "check") } } else null,
            onClick = {
                if (multiple) commit(if (isSelected) selected - option.first else selected + option.first)
                else { expanded = false; commit(listOf(option.first)) }
            }
        )
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            isError = error.isNotEmpty(),
            shape = almadarShape(AlmadarCorner.Interactive),
            colors = almadarFieldColors(ownBorder),
            interactionSource = source,
            textStyle = almadarType("sm"),
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
            supportingText = if (error.isNotEmpty()) { { Text(error) } } else null,
            trailingIcon = {
                if (clearable && selected.isNotEmpty()) {
                    AlmadarIcon(name = "close", description = "Clear", modifier = Modifier.clickable { commit(emptyList()) })
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .then(almadarInteractiveShadow(source, AlmadarCorner.Interactive))
                .then(almadarBorder(AlmadarCorner.Interactive, LocalAlmadarGeometry.current.borderStandard, edge, hostDrawsSolid = true))
                .heightIn(min = LocalAlmadarDensity.current.inputHeightMd)
                .almadarTag(tagId)
                .almadarLabel(props.string("aria-label"))
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.verificationWindowRoot()) {
            options.forEach { Item(it) }
            groups.forEach { group ->
                Text(
                    text = group.first,
                    style = almadarType("xs").copy(fontWeight = FontWeight.Bold),
                    color = palette.mutedForeground,
                    modifier = Modifier.padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("xs"))
                )
                group.second.forEach { Item(it) }
            }
        }
    }
}
