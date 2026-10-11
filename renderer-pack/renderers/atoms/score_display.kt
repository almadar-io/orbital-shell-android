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
fun ScoreDisplayRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val value = props.float("value", props.float("score", 0f))
    val locale = props.string("locale").let { if (it.isEmpty()) java.util.Locale.getDefault() else java.util.Locale.forLanguageTag(it) }
    val formatted = java.text.NumberFormat.getInstance(locale).format(value.toDouble())
    val label = props.string("label")
    val iconName = props.string("icon")
    val style = almadarType(when (props.string("size", "md")) { "sm" -> "sm"; "lg" -> "2xl"; "xl" -> "4xl"; else -> "lg" })
    Row(
        modifier = modifier.almadarLabel(props.string("aria-label", if (label.isNotEmpty()) label + " " + formatted else formatted)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
    ) {
        if (iconName.isNotEmpty()) AlmadarIcon(name = iconName, tint = palette.foreground, modifier = Modifier.size(LocalAlmadarIconography.current.defaultSize * 1.25f))
        if (label.isNotEmpty()) Text(text = label, style = style.copy(fontWeight = FontWeight.Bold), color = palette.mutedForeground)
        Text(text = formatted, style = style.copy(fontWeight = FontWeight.Bold, fontFamily = LocalAlmadarTheme.current.typeScale.monoFamily), color = palette.foreground)
    }
}
