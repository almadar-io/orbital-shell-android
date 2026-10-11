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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

@Composable
fun LightboxRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val images = props.array("images").mapNotNull { it as? JsonObject }
    val start = props.int("currentIndex", 0)
    var index by remember(start) { mutableStateOf(start) }
    val closeAction = props.string("closeAction")
    val local = props.bool("local")
    if (!props.bool("isOpen", false) || images.isEmpty()) {
        Box(modifier = modifier)
    } else {
        val last = images.lastIndex
        val shown = index.coerceIn(0, last)
        val image = images[shown]
        var drag by remember { mutableStateOf(0f) }
        val threshold = LocalAlmadarDensity.current.buttonHeightMd
        val pixels = with(LocalDensity.current) { threshold.toPx() }
        Box(
            modifier = modifier.fillMaxWidth()
                .heightIn(min = LocalAlmadarDensity.current.rowHeightSpacious * 8)
                .background(palette.scrim)
                .pointerInput(shown) {
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = {
                            if (drag > pixels && shown > 0) index = shown - 1
                            if (drag < -pixels && shown < last) index = shown + 1
                        }
                    ) { change, amount -> change.consume(); drag += amount }
                }
                .almadarLabel(props.string("aria-label", image.string("alt", "Image viewer")))
        ) {
            ImageRenderer(
                almadarJson("src" to image.string("src"), "alt" to image.string("alt"), "fit" to "contain"),
                Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = almadarSpaceFor("2xl"), vertical = almadarSpaceFor("xl")),
                AlmadarNoEvent
            )
            AlmadarButton(
                props, "", closeAction, onEvent,
                modifier = Modifier.align(Alignment.TopEnd).padding(almadarSpaceFor("md")),
                variant = "secondary", icon = "x", ariaLabel = "Close modal", local = local
            )
            if (shown > 0) {
                AlmadarLocalButton(
                    props, "", { index = shown - 1 },
                    modifier = Modifier.align(Alignment.CenterStart).padding(almadarSpaceFor("sm")),
                    variant = "secondary", icon = "chevron-left", ariaLabel = "Previous image"
                )
            }
            if (shown < last) {
                AlmadarLocalButton(
                    props, "", { index = shown + 1 },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(almadarSpaceFor("sm")),
                    variant = "secondary", icon = "chevron-right", ariaLabel = "Next image"
                )
            }
            CompositionLocalProvider(LocalContentColor provides palette.background) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(almadarSpaceFor("md")),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                ) {
                    if (props.bool("showCounter", true) && images.size > 1) {
                        AlmadarText((shown + 1).toString() + " / " + images.size, variant = "small", color = "inherit", align = "center")
                    }
                    if (image.string("caption").isNotEmpty()) AlmadarText(image.string("caption"), variant = "small", color = "inherit", align = "center")
                }
            }
        }
    }
}
