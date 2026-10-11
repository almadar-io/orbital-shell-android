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
import androidx.compose.ui.graphics.Color

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.graphics.Brush

@Composable
fun GradientDividerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val color = props.string("color")
    val tone = if (color.isEmpty()) LocalAlmadarTheme.current.color.primary else almadarTone(color).solid
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LocalAlmadarGeometry.current.borderHairline)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, tone, Color.Transparent)))
            .almadarLabel(props.string("aria-label"))
    )
}
