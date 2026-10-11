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
fun DividerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val label = props.string("label")
    val color = LocalAlmadarTheme.current.color.border
    val thickness = LocalAlmadarGeometry.current.borderHairline
    if (props.string("orientation", "horizontal") == "vertical") {
        VerticalDivider(modifier = modifier.fillMaxHeight(), thickness = thickness, color = color)
    } else if (label.isNotEmpty()) {
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), thickness = thickness, color = color)
            Text(
                text = label,
                style = almadarType("xs"),
                color = LocalAlmadarTheme.current.color.mutedForeground,
                modifier = Modifier.padding(horizontal = almadarSpaceFor("sm"))
            )
            HorizontalDivider(modifier = Modifier.weight(1f), thickness = thickness, color = color)
        }
    } else {
        HorizontalDivider(modifier = modifier.fillMaxWidth(), thickness = thickness, color = color)
    }
}
