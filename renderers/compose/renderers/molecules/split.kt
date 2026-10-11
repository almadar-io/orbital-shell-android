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

@Composable
fun SplitRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val parts = props.string("ratio", "1:1").split(":").mapNotNull { it.toFloatOrNull() }
    val first = parts.getOrElse(0) { 1f }
    val second = parts.getOrElse(1) { 1f }
    val reverse = props.bool("reverse")
    val gap = almadarSpaceFor(props.string("gap", "md"))
    val breakpoint = when (props.string("stackBreakpoint", "md")) { "sm" -> AlmadarBreakpoints.sm; "lg" -> AlmadarBreakpoints.lg; "xl" -> AlmadarBreakpoints.xl; else -> AlmadarBreakpoints.md }
    val rowAlign = when (props.string("align", "stretch")) { "start" -> Alignment.Top; "center" -> Alignment.CenterVertically; "end" -> Alignment.Bottom; else -> Alignment.Top }
    val children = props.children()
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "card", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier)
    } else {
        AlmadarBoxWithConstraints(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
            if (props.bool("stackOnMobile", true) && maxWidth < breakpoint) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    (if (reverse) children.reversed() else children).forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().let { if (props.string("align", "stretch") == "stretch") it.height(IntrinsicSize.Min) else it },
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = rowAlign
                ) {
                    val ordered = if (reverse) listOf(second to children.getOrNull(1), first to children.getOrNull(0)) else listOf(first to children.getOrNull(0), second to children.getOrNull(1))
                    ordered.forEach { (weight, child) ->
                        if (child != null) {
                            RenderChild(child, Modifier.weight(weight).let { if (props.string("align", "stretch") == "stretch") it.fillMaxHeight() else it }, onEvent)
                        }
                    }
                }
            }
        }
    }
}
