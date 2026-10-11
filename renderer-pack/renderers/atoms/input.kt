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
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow

// tier: atoms — a single Material 3 control

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun InputRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val incoming = props.string("value")
    var value by remember(incoming) { mutableStateOf(incoming) }
    val label = props.string("label")
    val placeholder = props.string("placeholder")
    val helper = props.string("helperText")
    val error = props.string("error")
    val disabled = props.bool("disabled")
    val inputType = props.string("inputType", "text")
    val name = props.string("name")
    val changeEvent = props.string("onChange")
    val action = props.string("action")
    val clearEvent = props.string("onClear")
    val leftIcon = props.string("leftIcon", props.string("icon"))
    val rightIcon = props.string("rightIcon")
    val clearable = props.bool("clearable")
    val multiline = inputType == "textarea"
    val keyboard = when (inputType) {
        "email" -> KeyboardType.Email
        "number" -> KeyboardType.Decimal
        "password" -> KeyboardType.Password
        "tel" -> KeyboardType.Phone
        "url" -> KeyboardType.Uri
        else -> KeyboardType.Text
    }
    val submit = {
        if (action.isNotEmpty()) {
            val numeric = value.toDoubleOrNull()
            if (inputType == "number") {
                if (numeric != null) onEvent(action, eventEnvelope(props, JsonObject(mapOf("value" to JsonPrimitive(numeric)))))
            } else {
                onEvent(action, eventEnvelope(props, JsonObject(mapOf("value" to JsonPrimitive(value)))))
            }
        }
    }
    val tagId = if (name.isNotEmpty()) almadarFieldId(name, props) else if (changeEvent.isNotEmpty()) almadarActionId(changeEvent, props) else ""
    val iconSize = LocalAlmadarIconography.current.defaultSize
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ownBorder = LocalAlmadarGeometry.current.borderStyleInteractive != CssBorderStyle.Solid
    val edge = if (error.isNotEmpty()) palette.error else if (focused) palette.ring else if (disabled) palette.border else palette.input
    val basis = almadarFieldBasis(leadingIcon = leftIcon.isNotEmpty(), trailingIcon = rightIcon.isNotEmpty() || (clearable && value.isNotEmpty()))
    Box(modifier = modifier.almadarIntrinsicWidth(0, basis), propagateMinConstraints = true) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, JsonObject(mapOf("value" to JsonPrimitive(it)))))
            },
            interactionSource = source,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LocalAlmadarDensity.current.inputHeightMd)
                .almadarTag(tagId)
                .almadarLabel(props.string("aria-label"))
                .then(almadarBorder(AlmadarCorner.Interactive, LocalAlmadarGeometry.current.borderStandard, edge, hostDrawsSolid = true)),
            enabled = !disabled,
            isError = error.isNotEmpty(),
            singleLine = !multiline,
            minLines = if (multiline) props.int("rows", 3) else 1,
            shape = almadarShape(AlmadarCorner.Interactive),
            colors = almadarFieldColors(ownBorder),
            textStyle = almadarType("sm"),
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip) } } else null,
            supportingText = if (error.isNotEmpty() || helper.isNotEmpty()) { { Text(if (error.isNotEmpty()) error else helper) } } else null,
            leadingIcon = if (leftIcon.isNotEmpty()) { { AlmadarIcon(name = leftIcon, modifier = Modifier.size(iconSize)) } } else null,
            trailingIcon = if (clearable && value.isNotEmpty() && !disabled) {
                {
                    AlmadarIcon(
                        name = "close",
                        description = "Clear",
                        modifier = Modifier.size(iconSize).clickable {
                            value = ""
                            if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, JsonObject(mapOf("value" to JsonPrimitive("")))))
                            if (clearEvent.isNotEmpty()) onEvent(clearEvent, eventEnvelope(props))
                        }
                    )
                }
            } else if (rightIcon.isNotEmpty()) {
                { AlmadarIcon(name = rightIcon, modifier = Modifier.size(iconSize)) }
            } else null,
            visualTransformation = if (inputType == "password") PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = if (action.isNotEmpty()) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
    }
}
