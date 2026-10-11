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
fun ContainerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val size = props.string("maxWidth", props.string("size", "lg"))
    val cap = when (size) {
        "sm" -> AlmadarBreakpoints.sm
        "md" -> AlmadarBreakpoints.md
        "xl" -> AlmadarBreakpoints.xl
        "2xl" -> AlmadarBreakpoints.xxl
        "xs" -> AlmadarSizes.maxWXs
        "full" -> Dp.Infinity
        else -> AlmadarBreakpoints.lg
    }
    val padding = almadarSpaceFor(when (val token = props.string("padding", "md")) { "xl", "lg", "sm", "none" -> token; else -> "md" })
    Box(
        modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")),
        contentAlignment = if (props.bool("center", true)) Alignment.TopCenter else Alignment.TopStart
    ) {
        Column(modifier = Modifier.widthIn(max = cap).fillMaxWidth().padding(horizontal = padding)) {
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
    }
}
