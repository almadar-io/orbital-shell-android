package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun ModalRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val title = props.string("title")
    val closeEvent = props.string("closeEvent")
    val showClose = props.bool("showCloseButton", true) && closeEvent.isNotEmpty()
    val look = props.string("look", "centered-card")
    val footer = root?.get("footer")
    val size = props.string("size", "md")
    val cap = almadarModalCap(look, size)
    val floor = almadarModalFloor(look, size)
    if (!props.bool("isOpen", true)) {
        Box(modifier = modifier)
    } else {
        AlmadarModalFrame(props, title, closeEvent, showClose, look, cap, floor, footer, modifier, onEvent) {
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
    }
}
