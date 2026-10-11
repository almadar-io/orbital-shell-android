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

// tier: molecules — a grouped container styled by tokens only

@Composable
fun StatBadgeRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val raw = (props as? JsonObject)?.get("value")
    val hasValue = raw != null && raw !is JsonNull
    val number = (raw as? JsonPrimitive)?.let { it.floatOrNull ?: it.contentOrNull?.toIntOrNull()?.toFloat() } ?: 0f
    val size = props.string("size", "md")
    val format = props.string("format", "number")
    val max = props.float("max", 0f)
    val variant = props.string("variant", "default")
    val tint = when (variant) {
        "primary" -> palette.primary
        "success" -> palette.success
        "warning" -> palette.warning
        "danger" -> palette.error
        else -> null
    }
    val horizontal = almadarSpaceFor(when (size) { "xs" -> "xs"; "sm" -> "sm"; "lg" -> "lg"; else -> "md" })
    val vertical = almadarSpaceFor(when (size) { "xs", "sm" -> "xs"; else -> "sm" })
    val barSize = if (size == "lg") "md" else "sm"
    val icon = props.string("icon")
    val asset = props.assetUrl("iconUrl").ifEmpty { props.assetUrl("assetUrl") }
    val label = props.string("label")
    Surface(
        modifier = modifier.almadarLabel(props.string("aria-label")),
        shape = MaterialTheme.shapes.medium,
        color = tint?.copy(alpha = 0.15f) ?: palette.card.copy(alpha = 0.8f),
        contentColor = palette.foreground,
        border = androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderStandard, tint?.copy(alpha = 0.4f) ?: palette.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontal, vertical = vertical),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (asset.isNotEmpty()) coil.compose.SubcomposeAsyncImage(model = asset, imageLoader = rememberAlmadarImageLoader(), contentDescription = null, modifier = Modifier.size(LocalAlmadarIconography.current.defaultSize), error = { AlmadarGlyph("image", size = "sm") })
            else if (icon.isNotEmpty()) AlmadarGlyph(icon, size = "sm")
            AlmadarText(label, variant = "caption", color = "muted", weight = "medium")
            if (hasValue && (format == "hearts" || format == "bar") && max > 0f) {
                HealthBarRenderer(almadarJson("current" to number, "max" to max, "format" to format, "size" to barSize), Modifier, AlmadarNoEvent)
            }
            if (hasValue && format == "number") {
                ScoreDisplayRenderer(almadarJson("value" to number, "size" to barSize), Modifier, AlmadarNoEvent)
            }
            if (hasValue && format == "text") AlmadarText((raw as? JsonPrimitive)?.contentOrNull ?: "", weight = "bold")
        }
    }
}
