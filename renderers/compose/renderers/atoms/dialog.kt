package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.border

// tier: atoms — a single Material 3 control

@Composable
fun DialogRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    Surface(
        modifier = modifier.almadarLabel(props.string("aria-label")).then(almadarBorder(AlmadarCorner.Container, LocalAlmadarGeometry.current.borderStandard, palette.border)),
        color = palette.card,
        contentColor = palette.cardForeground,
        shape = almadarShape(AlmadarCorner.Container),
        shadowElevation = LocalAlmadarElevation.current.dialogElevation.radius
    ) {
        Column(
            modifier = Modifier.padding(LocalAlmadarDensity.current.dialogPadding),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
        ) {
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
    }
}
