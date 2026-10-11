package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: atoms — a single Material 3 control

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@Composable
fun TextareaRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val incoming = props.string("value")
    var value by remember(incoming) { mutableStateOf(incoming) }
    val label = props.string("label")
    val placeholder = props.string("placeholder")
    val helper = props.string("helperText")
    val error = props.string("error")
    val name = props.string("name")
    val changeEvent = props.string("onChange")
    val action = props.string("action")
    val tagId = if (name.isNotEmpty()) almadarFieldId(name, props) else if (changeEvent.isNotEmpty()) almadarActionId(changeEvent, props) else ""
    val palette = LocalAlmadarTheme.current.color
    val source = remember { MutableInteractionSource() }
    val focusToken = LocalAlmadarFieldFocusToken.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusToken) { if (focusToken != null) focusRequester.requestFocus() }
    val focused by source.collectIsFocusedAsState()
    val ownBorder = LocalAlmadarGeometry.current.borderStyleInteractive != CssBorderStyle.Solid
    val edge = if (error.isNotEmpty()) palette.error else if (focused) palette.ring else palette.input
    Box(modifier = modifier, propagateMinConstraints = true) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                if (changeEvent.isNotEmpty()) onEvent(changeEvent, eventEnvelope(props, JsonObject(mapOf("value" to JsonPrimitive(it)))))
            },
            interactionSource = source,
            modifier = Modifier
                .fillMaxWidth()
                .then(almadarInteractiveShadow(source, AlmadarCorner.Interactive))
                .then(almadarBorder(AlmadarCorner.Interactive, LocalAlmadarGeometry.current.borderStandard, edge, hostDrawsSolid = true))
                .focusRequester(focusRequester)
                .almadarTag(tagId)
                .almadarLabel(props.string("aria-label"))
                .onPreviewKeyEvent { event ->
                    val submitKey = action.isNotEmpty() && event.type == KeyEventType.KeyDown &&
                        event.key == Key.Enter && (event.isCtrlPressed || event.isMetaPressed)
                    if (submitKey) onEvent(action, eventEnvelope(props, JsonObject(mapOf("value" to JsonPrimitive(value)))))
                    submitKey
                },
            isError = error.isNotEmpty(),
            minLines = props.int("rows", 3),
            shape = almadarShape(AlmadarCorner.Interactive),
            colors = almadarFieldColors(ownBorder),
            textStyle = almadarType("sm"),
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
            supportingText = if (error.isNotEmpty() || helper.isNotEmpty()) { { Text(if (error.isNotEmpty()) error else helper) } } else null
        )
    }
}
