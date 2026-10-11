package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.Role

@Composable
fun ThemeToggleRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val mode = LocalAlmadarThemeMode.current
    val dark = mode.isDark
    val showLabel = props.bool("showLabel")
    val description = props.string("aria-label", if (dark) "Switch to light theme" else "Switch to dark theme")
    val extent = LocalAlmadarIconography.current.defaultSize * (when (props.string("size", "md")) { "sm" -> 1.5f; "lg" -> 2.25f; else -> 1.875f })
    Box(modifier = modifier, propagateMinConstraints = true) {
        Row(
            modifier = Modifier
                .clip(almadarShape(AlmadarCorner.Interactive))
                .clickable(role = Role.Button, onClickLabel = description) { mode.toggle() }
                .padding(almadarSpaceFor("sm")),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
        ) {
            AlmadarIcon(name = if (dark) "sun" else "moon", tint = palette.foreground, description = description, modifier = Modifier.size(extent))
            if (showLabel) Text(text = if (dark) "Light" else "Dark", style = almadarType("sm").copy(fontWeight = FontWeight.Medium), color = palette.foreground)
        }
    }
}
