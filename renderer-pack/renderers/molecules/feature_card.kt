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

// tier: molecules — a grouped container styled by tokens only

@Composable
fun FeatureCardRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val icon = props.string("icon")
    val href = props.string("href")
    val variant = props.string("variant", "bordered")
    val size = props.string("size", "md")
    val density = LocalAlmadarDensity.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val pad = when (size) {
        "sm" -> density.cardPaddingSm
        "lg" -> density.cardPaddingLg
        else -> density.cardPaddingMd
    }
    val gap = almadarSpaceFor(size)
    var surface: Modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))
    if (variant == "interactive" && href.isNotEmpty()) {
        surface = surface.clickable(role = androidx.compose.ui.semantics.Role.Button) { uriHandler.openUri(href) }
    }
    AlmadarPanel(modifier = surface, muted = variant == "default") {
        Column(modifier = Modifier.fillMaxWidth().padding(pad), verticalArrangement = Arrangement.spacedBy(gap)) {
            if (icon.isNotEmpty()) AlmadarGlyph(icon, color = "primary", size = if (size == "sm") "lg" else "xl")
            AlmadarText(props.string("title"), variant = "h4")
            AlmadarText(props.string("description"), variant = "body2", color = "muted")
            if (href.isNotEmpty() && variant != "interactive") {
                AlmadarLocalButton(props, props.string("linkLabel", "Learn more"), { uriHandler.openUri(href) }, variant = "ghost", size = "sm")
            }
        }
    }
}
