package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: atoms — a single Material 3 control

@Composable
fun CenterRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val horizontal = props.bool("horizontal", true)
    val vertical = props.bool("vertical", true)
    var sized: Modifier = Modifier
    if (props.bool("fullWidth") || !props.bool("inline")) sized = sized.fillMaxWidth()
    if (props.bool("fullHeight")) sized = sized.fillMaxHeight()
    val alignment = when {
        horizontal && vertical -> Alignment.Center
        horizontal -> Alignment.TopCenter
        vertical -> Alignment.CenterStart
        else -> Alignment.TopStart
    }
    val nodes = props.children()
    Box(modifier = modifier.then(sized).almadarLabel(props.string("aria-label")), contentAlignment = alignment) {
        nodes.forEach { RenderChild(it, Modifier, onEvent) }
    }
}
