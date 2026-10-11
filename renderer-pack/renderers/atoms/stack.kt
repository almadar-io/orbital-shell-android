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

// tier: atoms — a single Material 3 control

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StackRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val gap = almadarSpaceFor(props.string("gap", "md"))
    val align = props.string("align", "stretch")
    val justify = props.string("justify", "start")
    val horizontal = props.string("direction", "vertical") == "horizontal"
    val wrap = props.bool("wrap")
    val flex = props.bool("flex")
    val action = props.string("action")
    val nodes = props.children().let { if (props.bool("reverse")) it.asReversed() else it }
    var styled: Modifier = if (flex) Modifier.fillMaxSize() else Modifier
    if (action.isNotEmpty()) styled = styled.clickable { onEvent(action, eventEnvelope(props)) }.almadarTag(almadarActionId(action, props))
    Box(modifier = modifier.almadarLabel(props.string("aria-label")), propagateMinConstraints = true) {
        if (horizontal) {
            val arrangement = when (justify) {
                "center" -> Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
                "end" -> Arrangement.spacedBy(gap, Alignment.End)
                "between" -> Arrangement.SpaceBetween
                "around" -> Arrangement.SpaceAround
                "evenly" -> Arrangement.SpaceEvenly
                else -> Arrangement.spacedBy(gap)
            }
            val vertical = when (align) {
                "center" -> Alignment.CenterVertically
                "end" -> Alignment.Bottom
                else -> Alignment.Top
            }
            if (wrap) {
                FlowRow(modifier = styled, horizontalArrangement = arrangement, verticalArrangement = Arrangement.spacedBy(gap)) {
                    nodes.forEach { RenderChild(it, Modifier, onEvent) }
                }
            } else {
                if (flex || justify != "start") {
                    Row(modifier = styled, horizontalArrangement = arrangement, verticalAlignment = vertical) {
                        nodes.forEach { RenderChild(it, if (flex) Modifier.weight(1f) else Modifier, onEvent) }
                    }
                } else {
                    AlmadarFlexRow(gap, vertical, styled) {
                        nodes.forEach { RenderChild(it, Modifier, onEvent) }
                    }
                }
            }
        } else {
            val arrangement = when (justify) {
                "center" -> Arrangement.spacedBy(gap, Alignment.CenterVertically)
                "end" -> Arrangement.spacedBy(gap, Alignment.Bottom)
                "between" -> Arrangement.SpaceBetween
                "around" -> Arrangement.SpaceAround
                "evenly" -> Arrangement.SpaceEvenly
                else -> Arrangement.spacedBy(gap)
            }
            val horizontalAlign = when (align) {
                "center" -> Alignment.CenterHorizontally
                "end" -> Alignment.End
                else -> Alignment.Start
            }
            Column(modifier = styled, verticalArrangement = arrangement, horizontalAlignment = horizontalAlign) {
                nodes.forEach {
                    val share = if (flex) Modifier.weight(1f) else Modifier
                    RenderChild(it, if (align == "stretch") share.fillMaxWidth() else share, onEvent)
                }
            }
        }
    }
}
