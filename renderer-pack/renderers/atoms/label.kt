package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

@Composable
fun LabelRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val childText = ((props as? JsonObject)?.get("children") as? JsonPrimitive)?.contentOrNull ?: ""
    val text = props.string("text", childText)
    Row(modifier = modifier.almadarLabel(props.string("aria-label"))) {
        Text(text = text, style = almadarType("sm").copy(fontWeight = FontWeight.Bold), color = palette.foreground)
        if (props.bool("required")) Text(text = " *", style = almadarType("sm"), color = palette.error)
    }
}
