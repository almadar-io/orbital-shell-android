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
import androidx.compose.ui.graphics.Color

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.Role

@Composable
fun TimeSlotCellRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val time = props.string("time")
    val occupied = props.bool("isOccupied")
    val clickEvent = props.string("onClick")
    var cell: Modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = LocalAlmadarDensity.current.rowHeightNormal)
        .background(if (occupied) palette.muted.copy(alpha = 0.3f) else Color.Transparent)
    if (clickEvent.isNotEmpty()) {
        cell = cell
            .clickable(role = Role.Button, onClickLabel = time) { onEvent(clickEvent, eventEnvelope(props, JsonObject(mapOf("time" to JsonPrimitive(time))))) }
            .almadarTag(almadarActionId(clickEvent, props))
    }
    Box(modifier = modifier.almadarLabel(props.string("aria-label", time)), propagateMinConstraints = true) {
        Column(modifier = cell.padding(almadarSpaceFor("xs"))) {
            Text(text = time, style = almadarType("xs"), color = if (occupied) palette.mutedForeground else palette.foreground)
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
    }
}
