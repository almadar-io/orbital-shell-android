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
fun SpacerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val size = props.string("size", "auto")
    val extent = almadarSpaceFor(size)
    val vertical = props.string("axis", "horizontal") == "vertical"
    if (size == "auto") {
        Spacer(modifier = modifier)
    } else if (vertical) {
        Spacer(modifier = modifier.height(extent))
    } else {
        Spacer(modifier = modifier.width(extent))
    }
}
