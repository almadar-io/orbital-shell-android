package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

@Composable
fun SwipeableRowRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val itemData = (props as? JsonObject)?.get("itemData") as? JsonObject
    val left = props.array("leftActions").mapNotNull { it as? JsonObject }
    val right = props.array("rightActions").mapNotNull { it as? JsonObject }
    val revealWidth = props.float("threshold", 80f).dp
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    val maxLeft = if (left.isNotEmpty()) revealPx else 0f
    val maxRight = if (right.isNotEmpty()) revealPx else 0f
    var offset by remember { mutableStateOf(0f) }
    val rowId = itemData?.string("id") ?: ""
    val payloadOf = { action: JsonObject ->
        JsonObject((action.jsonObject("eventPayload") ?: JsonObject(emptyMap())) + mapOf("row" to (itemData ?: JsonObject(emptyMap()))))
    }
    val trayButton: @Composable (JsonObject, Modifier) -> Unit = { action, buttonModifier ->
        val event = action.string("event")
        AlmadarButton(
            props, action.string("label"), event.ifEmpty { ALMADAR_LOCAL_ACTION },
            { name, envelope ->
                offset = 0f
                if (event.isNotEmpty()) onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload")))
            },
            buttonModifier,
            payload = payloadOf(action), variant = action.string("variant", "secondary"), icon = action.string("icon"),
            size = "sm", disabled = action.bool("disabled"), local = event.isEmpty()
        )
    }
    Box(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        if (offset > 0f && left.isNotEmpty()) {
            Row(modifier = Modifier.align(Alignment.CenterStart).width(revealWidth), verticalAlignment = Alignment.CenterVertically) {
                left.forEach { trayButton(it, Modifier.weight(1f)) }
            }
        }
        if (offset < 0f && right.isNotEmpty()) {
            Row(modifier = Modifier.align(Alignment.CenterEnd).width(revealWidth), verticalAlignment = Alignment.CenterVertically) {
                right.forEach { trayButton(it, Modifier.weight(1f)) }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.toInt(), 0) }
                .then(if (rowId.isEmpty()) Modifier else Modifier.verificationContainer(VerificationIds.row(rowId)))
                .pointerInput(maxLeft, maxRight) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offset = if (offset > revealPx / 2f && maxLeft > 0f) maxLeft else if (offset < -revealPx / 2f && maxRight > 0f) -maxRight else 0f
                        }
                    ) { change, delta ->
                        change.consume()
                        offset = (offset + delta).coerceIn(-maxRight, maxLeft)
                    }
                },
            color = palette.surface
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                Column(modifier = Modifier.weight(1f)) {
                    props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                    (left + right).forEach { action ->
                        val event = action.string("event")
                        val icon = action.string("icon")
                        AlmadarButton(
                            props, if (icon.isEmpty()) action.string("label") else "", event.ifEmpty { ALMADAR_LOCAL_ACTION },
                            { name, envelope -> if (event.isNotEmpty()) onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload"))) },
                            payload = payloadOf(action), variant = "ghost", icon = icon, size = "sm",
                            disabled = action.bool("disabled"), ariaLabel = action.string("label"), local = event.isEmpty()
                        )
                    }
                }
            }
        }
    }
}
