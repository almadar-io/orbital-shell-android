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
import androidx.compose.ui.input.pointer.pointerInput

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun ControlGridRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val size = props.string("size", "md")
    val disabled = props.bool("disabled")
    val dpad = props.string("kind") == "dpad"
    val held = remember { mutableStateListOf<String>() }
    val announce: (String, Boolean) -> Unit = { id, pressed ->
        val actionEvent = props.string("actionEvent")
        if (actionEvent.isNotEmpty()) onEvent(actionEvent, eventEnvelope(props, almadarJson("id" to id, "pressed" to pressed)))
        val directionEvent = props.string("directionEvent")
        if (dpad && directionEvent.isNotEmpty()) onEvent(directionEvent, eventEnvelope(props, almadarJson("direction" to id, "pressed" to pressed)))
    }
    val pressable: @Composable (String, Map<String, JsonElement>, String, String, Modifier) -> Unit = { id, values, press, release, cell ->
        Box(
            modifier = cell.pointerInput(id, disabled) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (!disabled && !held.contains(id)) { held.add(id); announce(id, true) }
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (held.remove(id)) announce(id, false)
                }
            },
            contentAlignment = Alignment.Center
        ) {
            ControlButtonRenderer(
                props.merging(values + mapOf(
                    "pressEvent" to JsonPrimitive(press),
                    "releaseEvent" to JsonPrimitive(release),
                    "size" to JsonPrimitive(size),
                    "disabled" to JsonPrimitive(disabled)
                )),
                Modifier,
                onEvent
            )
        }
    }
    if (dpad) {
        val width = when (size) { "sm" -> AlmadarSizes.w28; "lg" -> AlmadarSizes.w52; else -> AlmadarSizes.w40 }
        val gap = when (size) { "sm" -> AlmadarSizes.w0_5; "lg" -> AlmadarSizes.w1_5; else -> almadarSpace("space1") }
        val cellSide = (width - gap * 2) / 3
        val events = root?.get("directionEvents") as? JsonObject
        val releases = root?.get("directionReleaseEvents") as? JsonObject
        val assets = root?.get("directionAssets") as? JsonObject
        val glyphs = mapOf("up" to "▲\uFE0E", "down" to "▼\uFE0E", "left" to "◀\uFE0E", "right" to "▶\uFE0E")
        val names = mapOf("up" to "Up", "down" to "Down", "left" to "Left", "right" to "Right")
        val blank: @Composable () -> Unit = { Spacer(Modifier.size(cellSide)) }
        val direction: @Composable (String) -> Unit = { d ->
            val press = events?.string(d) ?: ""
            if (events == null || press.isNotEmpty()) {
                val values = mutableMapOf<String, JsonElement>(
                    "variant" to JsonPrimitive("secondary"),
                    "label" to JsonPrimitive(glyphs[d] ?: ""),
                    "aria-label" to JsonPrimitive(names[d] ?: d)
                )
                assets?.get(d)?.let { values["assetUrl"] = it }
                pressable(d, values, press, releases?.string(d) ?: "", Modifier.size(cellSide))
            } else blank()
        }
        val hubSide = almadarSpace("space6")
        Column(modifier = modifier.width(width), verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) { blank(); direction("up"); blank() }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                direction("left")
                Box(modifier = Modifier.size(cellSide), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(hubSide)
                            .background(palette.muted, almadarShape(AlmadarCorner.Interactive))
                            .then(almadarBorder(AlmadarCorner.Interactive, LocalAlmadarGeometry.current.borderHeavy, palette.mutedForeground, lineFrom = AlmadarCorner.Container))
                    )
                }
                direction("right")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) { blank(); direction("down"); blank() }
        }
    } else {
        val buttons: List<JsonObject> = (root?.get("buttons") as? JsonArray)?.mapNotNull { it as? JsonObject } ?: listOf(
            almadarJson("id" to "jump", "label" to "Jump", "icon" to "chevron-up", "variant" to "primary"),
            almadarJson("id" to "attack", "label" to "Attack", "icon" to "sword", "variant" to "secondary"),
            almadarJson("id" to "dodge", "label" to "Dodge", "icon" to "wind", "variant" to "ghost")
        )
        val action: @Composable (JsonObject) -> Unit = { button ->
            val id = button.string("id")
            val values = mutableMapOf<String, JsonElement>(
                "variant" to JsonPrimitive(button.string("variant", "secondary")),
                "aria-label" to JsonPrimitive(button.string("label", id))
            )
            listOf("label", "icon", "assetUrl").forEach { key -> button[key]?.let { values[key] = it } }
            pressable(id, values, "", "", Modifier)
        }
        val small = almadarSpace("space1")
        val layout = props.string("layout", "horizontal")
        when {
            layout == "diamond" && buttons.size == 4 -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(small), horizontalAlignment = Alignment.CenterHorizontally) {
                action(buttons[0])
                Row(horizontalArrangement = Arrangement.spacedBy(small), verticalAlignment = Alignment.CenterVertically) { action(buttons[3]); action(buttons[1]) }
                action(buttons[2])
            }
            layout == "diamond" -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(small)) {
                buttons.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(small)) { row.forEach { action(it) } } }
            }
            layout == "vertical" -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(almadarSpace("space2"))) { buttons.forEach { action(it) } }
            else -> Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2"))) { buttons.forEach { action(it) } }
        }
    }
}
