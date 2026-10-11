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

// tier: molecules — a grouped container styled by tokens only

@Composable
fun EmptyStateRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val variant = props.string("variant", "default")
    val destructive = props.bool("destructive") || variant == "error"
    val success = variant == "success"
    val look = props.string("look", "icon-only")
    val rawIcon = props.string("icon")
    val iconName = when (rawIcon) {
        "check" -> "check-circle"
        "error" -> "x-circle"
        "warning" -> "alert-circle"
        else -> rawIcon
    }
    val title = props.string("title", props.string("message", "No items"))
    val description = props.string("description")
    val actionLabel = props.string("actionLabel")
    val actionEvent = props.string("actionEvent")
    val tone = if (destructive) "error" else if (success) "success" else "muted"
    val badgeFill = if (destructive) palette.error.copy(alpha = 0.1f) else if (success) palette.success.copy(alpha = 0.1f) else palette.muted
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("2xl")).almadarLabel(props.string("aria-label")),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
    ) {
        if (iconName.isNotEmpty() && look != "text-only") {
            Box(modifier = Modifier.background(badgeFill, MaterialTheme.shapes.large).padding(almadarSpaceFor("sm"))) {
                AlmadarGlyph(iconName, color = tone, size = if (look == "illustrated" || look == "mascot") "xl" else "lg")
            }
        }
        AlmadarText(title, variant = "h5", color = if (destructive) "error" else if (success) "success" else "primary", align = "center")
        if (description.isNotEmpty()) AlmadarText(description, variant = "small", color = "muted", align = "center")
        if (actionLabel.isNotEmpty() && actionEvent.isNotEmpty()) {
            AlmadarButton(props, actionLabel, actionEvent, onEvent, variant = if (destructive) "danger" else "primary")
        }
    }
}
