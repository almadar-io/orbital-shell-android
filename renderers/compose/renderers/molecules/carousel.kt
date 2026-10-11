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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

@Composable
fun CarouselRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val items = props.array("items")
    val total = items.size
    val loop = props.bool("loop")
    val slideEvent = props.string("slideChangeEvent")
    val vertical = props.string("orientation") == "vertical"
    val loadMoreEvent = props.string("loadMoreEvent")
    var requestedAt by remember { mutableStateOf(-1) }
    val template = root?.get("renderItem") ?: root?.get("children")
    var active by remember { mutableStateOf(0) }
    var dragX by remember { mutableStateOf(0f) }
    val swipe = with(LocalDensity.current) { almadarSpaceFor("2xl").toPx() }
    val goTo = { requested: Int ->
        val target = if (loop) (if (requested < 0) total - 1 else if (requested >= total) 0 else requested) else requested.coerceIn(0, total - 1)
        active = target
        if (slideEvent.isNotEmpty()) {
            onEvent(slideEvent, eventEnvelope(props, JsonObject(mapOf("index" to JsonPrimitive(target)) + (props.jsonObject("slideChangePayload") ?: JsonObject(emptyMap())))))
        }
        // One request per page size once the viewer is `loadMoreAhead` slides from the end.
        if (loadMoreEvent.isNotEmpty() && props.bool("hasMore") && !props.bool("isLoadingMore") &&
            target >= total - 1 - props.int("loadMoreAhead", 2) && requestedAt != total) {
            requestedAt = total
            onEvent(loadMoreEvent, eventEnvelope(props, JsonObject(mapOf("index" to JsonPrimitive(target)))))
        }
    }
    LaunchedEffect(props.bool("autoPlay"), props.int("autoPlayInterval", 5000), loop, total) {
        if (props.bool("autoPlay") && total > 1) {
            while (true) {
                kotlinx.coroutines.delay(props.int("autoPlayInterval", 5000).toLong())
                val next = if (loop) (active + 1) % total else minOf(active + 1, total - 1)
                if (next != active) goTo(next)
            }
        }
    }
    if (total > 0) {
        val current = active.coerceIn(0, total - 1)
        val hasPrev = loop || current > 0
        val hasNext = loop || current < total - 1
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (props.bool("fullHeight")) Modifier.fillMaxHeight() else Modifier)
                .almadarLabel(props.string("aria-label"))
                .pointerInput(total, loop, vertical) {
                    if (vertical) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragX <= -swipe) goTo(current + 1)
                                if (dragX >= swipe) goTo(current - 1)
                                dragX = 0f
                            }
                        ) { change, delta -> change.consume(); dragX += delta }
                    } else {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragX <= -swipe) goTo(current + 1)
                                if (dragX >= swipe) goTo(current - 1)
                                dragX = 0f
                            }
                        ) { change, delta -> change.consume(); dragX += delta }
                    }
                }
        ) {
            val slide = when (template) {
                is JsonArray -> template.getOrNull(current)
                is JsonObject -> template
                else -> null
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (slide != null) RenderChild(slide, Modifier.fillMaxWidth(), onEvent)
            }
            if (props.bool("showArrows", true) && total > 1) {
                val arrow: @Composable (Boolean, String, String, Int, Modifier) -> Unit = { enabled, icon, label, step, place ->
                    if (enabled) {
                        Box(modifier = place.padding(almadarSpaceFor("sm")).background(palette.surface.copy(alpha = 0.8f), MaterialTheme.shapes.large)) {
                            AlmadarButton(
                                props, "", slideEvent.ifEmpty { ALMADAR_LOCAL_ACTION }, { _, _ -> goTo(current + step) },
                                variant = "ghost", icon = icon, size = "sm", ariaLabel = label, local = slideEvent.isEmpty()
                            )
                        }
                    }
                }
                if (vertical) {
                    arrow(hasPrev, "chevron-up", "Previous slide", -1, Modifier.align(Alignment.TopEnd))
                    arrow(hasNext, "chevron-down", "Next slide", 1, Modifier.align(Alignment.BottomEnd))
                } else {
                    arrow(hasPrev, "chevron-left", "Previous slide", -1, Modifier.align(Alignment.CenterStart))
                    arrow(hasNext, "chevron-right", "Next slide", 1, Modifier.align(Alignment.CenterEnd))
                }
            }
            if (props.bool("showDots", true) && total > 1) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = almadarSpaceFor("md")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(total) { index ->
                        val on = index == current
                        Box(
                            modifier = Modifier
                                .size(almadarSpace("space2") * (if (on) 1.25f else 1f))
                                .almadarAlpha(if (on) 1f else 0.6f)
                                .background(if (on) palette.primary else palette.muted, MaterialTheme.shapes.large)
                                .clickable(role = androidx.compose.ui.semantics.Role.Tab) { goTo(index) }
                                .almadarActionTag(slideEvent, props)
                                .almadarLabel("Go to slide " + (index + 1))
                        )
                    }
                }
            }
        }
    }
}
