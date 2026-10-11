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

// tier: atoms — a single Material 3 control

@Composable
fun SpinnerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val base = LocalAlmadarIconography.current.defaultSize
    val extent = when (props.string("size", "md")) {
        "xs" -> base * 0.75f
        "sm" -> base
        "lg" -> base * 2f
        else -> base * 1.5f
    }
    val description = props.string("aria-label", "Loading")
    val indicator: @Composable () -> Unit = {
        CircularProgressIndicator(
            modifier = Modifier.size(extent),
            color = palette.foreground,
            strokeWidth = LocalAlmadarGeometry.current.borderHeavy
        )
    }
    if (props.bool("overlay")) {
        Box(
            modifier = modifier.fillMaxSize().background(palette.background.copy(alpha = 0.6f)).almadarLabel(description),
            contentAlignment = Alignment.Center
        ) { indicator() }
    } else {
        Box(modifier = modifier.almadarLabel(description), contentAlignment = Alignment.Center) { indicator() }
    }
}
