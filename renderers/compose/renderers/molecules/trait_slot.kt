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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Popup

@Composable
fun TraitSlotRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val item = root?.get("equippedItem") as? JsonObject
    val slotNumber = props.int("slotNumber")
    val locked = props.bool("locked")
    val slotSize = props.string("size", "md")
    val feedback = props.string("feedback")
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val shape = almadarShape(AlmadarCorner.Container)
    fun named(event: String, callback: String): String = props.string(event).ifEmpty { props.string(callback) }
    val clickEvent = named("clickEvent", "onClick")
    val removeEvent = named("removeEvent", "onRemove")
    val dropEvent = named("dropEvent", "onItemDrop")
    val category = (root?.get("categoryColors") as? JsonObject)?.get(item?.string("category") ?: "") as? JsonObject
    val fill = category?.string("bg")?.takeIf { it.isNotEmpty() }?.let { almadarCssColor(it, palette.card) } ?: palette.card.copy(alpha = 0.5f)
    val categoryEdge = category?.string("border")?.takeIf { it.isNotEmpty() }?.let { almadarCssColor(it, palette.border) } ?: palette.border
    var dragOver by remember { mutableStateOf(false) }
    var tipOpen by remember { mutableStateOf(false) }
    val droppable = !locked && dropEvent.isNotEmpty()
    val side = when (slotSize) { "sm" -> almadarSpace("space10"); "lg" -> almadarSpace("space12") + almadarSpace("space6"); else -> AlmadarSizes.w14 }
    val iconSide = when (slotSize) { "sm" -> almadarSpace("space5"); "lg" -> almadarSpace("space10"); else -> almadarSpace("space7") }
    val iconVariant = when (slotSize) { "sm" -> "large"; "lg" -> "h1"; else -> "h2" }
    val edge = if (dragOver) palette.accent else if (feedback == "correct") palette.success else if (feedback == "wrong") palette.error else categoryEdge
    val ringColor = if (dragOver) palette.accent else if (feedback == "correct") palette.success else if (feedback == "wrong") palette.error else if (props.bool("selected")) palette.primary else null
    val empty = item == null
    val heavy = geometry.borderHeavy
    val gap = AlmadarSizes.w0_5
    val dash = almadarSpace("space1")
    val bare = Modifier
        .size(side)
        .graphicsLayer { scaleX = if (dragOver) 1.1f else 1f; scaleY = if (dragOver) 1.1f else 1f }
        .alpha(if (locked) 0.5f else 1f)
        .background(fill, shape)
        .drawBehind {
            val radius = CornerRadius(geometry.radiusContainer.toPx())
            val inset = heavy.toPx() / 2f
            drawRoundRect(
                edge, topLeft = Offset(inset, inset), size = Size(size.width - inset * 2f, size.height - inset * 2f), cornerRadius = radius,
                style = Stroke(width = heavy.toPx(), pathEffect = if (empty) PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx())) else null)
            )
            if (ringColor != null) {
                val out = gap.toPx() + heavy.toPx() / 2f
                drawRoundRect(ringColor, topLeft = Offset(-out, -out), size = Size(size.width + out * 2f, size.height + out * 2f), cornerRadius = radius, style = Stroke(width = heavy.toPx()))
            }
        }
        .then(if (droppable) Modifier.almadarDropTarget(true, { dragOver = it }) { raw ->
            val dropped = try { Json.parseToJsonElement(raw) as? JsonObject } catch (failure: kotlinx.serialization.SerializationException) { null }
            if (dropped != null) onEvent(dropEvent, eventEnvelope(props, almadarJson("slotNumber" to slotNumber, "itemId" to dropped.string("id"))))
        } else Modifier)
        .then(if (props.bool("draggable") && item != null) Modifier.almadarDragSource(item.toString()) else Modifier)
        .then(if (clickEvent.isNotEmpty()) Modifier.almadarActionTag(clickEvent, props) else Modifier)
        .pointerInput(clickEvent, locked) {
            detectTapGestures(
                onLongPress = { if (item != null) tipOpen = !tipOpen },
                onTap = { if (!locked && clickEvent.isNotEmpty()) onEvent(clickEvent, eventEnvelope(props, almadarJson("slotNumber" to slotNumber))) }
            )
        }
        .almadarLabel(item?.string("name") ?: "")
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = bare, contentAlignment = Alignment.Center) {
            if (locked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AlmadarText("🔒", variant = "caption", color = "muted")
                    if (props.string("lockLabel").isNotEmpty()) AlmadarText(props.string("lockLabel"), variant = "caption", color = "muted")
                }
            } else if (item != null) {
                val iconUrl = item.assetUrl("iconUrl")
                if (iconUrl.isNotEmpty()) {
                    ImageRenderer(almadarJson("src" to iconUrl, "alt" to item.string("name"), "fit" to "contain", "sourceTrait" to almadarTrait(props)), Modifier.size(iconSide), onEvent)
                } else {
                    AlmadarText(item.string("iconEmoji", "✦"), variant = iconVariant)
                }
            } else {
                AlmadarText("+", variant = "large", color = "muted")
            }
            if (item != null && removeEvent.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = AlmadarSizes.w1_5, y = -AlmadarSizes.w1_5)
                        .size(almadarSpace("space4"))
                        .background(palette.error, almadarShape(AlmadarCorner.Pill))
                        .almadarActionTag(removeEvent, props)
                        .clickable { onEvent(removeEvent, eventEnvelope(props, almadarJson("slotNumber" to slotNumber))) }
                        .almadarLabel("Remove"),
                    contentAlignment = Alignment.Center
                ) { AlmadarText("×", variant = "caption") }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = -almadarSpace("space1"), y = almadarSpace("space1"))
                    .size(almadarSpace("space4"))
                    .background(palette.card, almadarShape(AlmadarCorner.Pill))
                    .then(almadarBorder(AlmadarCorner.Pill, geometry.borderStandard, palette.border)),
                contentAlignment = Alignment.Center
            ) { AlmadarText(slotNumber.toString(), variant = "caption", color = "muted") }
        }
        if (tipOpen && item != null && props.bool("showTooltip", true)) {
            Popup(alignment = Alignment.TopCenter, offset = androidx.compose.ui.unit.IntOffset(0, -with(LocalDensity.current) { (side + almadarSpace("space2")).roundToPx() }), onDismissRequest = { tipOpen = false }) {
                Column(
                    modifier = Modifier
                        .verificationWindowRoot()
                        .widthIn(min = AlmadarSizes.w52)
                        .background(palette.background, shape)
                        .then(almadarBorder(AlmadarCorner.Container, geometry.borderStandard, palette.border))
                        .padding(almadarSpace("space3")),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
                ) {
                    AlmadarText(item.string("name"), variant = "h6", align = "center")
                    if (item.string("description").isNotEmpty()) AlmadarText(item.string("description"), variant = "caption", color = "muted", align = "center")
                    val machine = (item["stateMachine"] as? JsonObject) ?: almadarJson(
                        "name" to item.string("name"), "description" to item.string("description"), "currentState" to "idle",
                        "states" to JsonArray(listOf("idle", "active", "done").map { JsonPrimitive(it) }),
                        "transitions" to JsonArray(listOf(
                            almadarJson("from" to "idle", "to" to "active", "event" to "ACTIVATE"),
                            almadarJson("from" to "active", "to" to "done", "event" to "COMPLETE")
                        ))
                    )
                    AlmadarTraitStateViewer(machine, variant = "compact", size = "sm")
                }
            }
        }
    }
}
