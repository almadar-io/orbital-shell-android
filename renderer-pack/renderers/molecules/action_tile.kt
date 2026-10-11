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
import androidx.compose.foundation.border

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.draw.alpha

@Composable
fun ActionTileRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val action = (props as? JsonObject)?.get("action") as? JsonObject
    val size = props.string("size", "md")
    val disabled = props.bool("disabled")
    val palette = LocalAlmadarTheme.current.color
    val shape = almadarShape(AlmadarCorner.Container)
    val category = (props as? JsonObject)?.get("categoryColors").let { it as? JsonObject }?.get(action?.string("category") ?: "") as? JsonObject
    val bg = category?.string("bg")?.takeIf { it.isNotEmpty() }?.let { almadarCssColor(it, palette.card) }
    val edge = category?.string("border")?.takeIf { it.isNotEmpty() }?.let { almadarCssColor(it, palette.border) }
    val (horizontal, vertical) = when (size) {
        "sm" -> almadarSpace("space2") to almadarSpace("space1")
        "lg" -> almadarSpace("space4") to almadarSpace("space3")
        else -> almadarSpace("space3") to almadarSpace("space2")
    }
    val iconVariant = when (size) { "sm" -> "large"; "lg" -> "h2"; else -> "h3" }
    val labelVariant = when (size) { "sm" -> "caption"; "lg" -> "body1"; else -> "small" }
    val iconUrl = action?.assetUrl("iconUrl") ?: ""
    val payload = action?.toString() ?: ""
    Column(
        modifier = modifier
            .alpha(if (disabled) 0.4f else 1f)
            .background(if (disabled) palette.muted else (bg ?: palette.card), shape)
            .then(almadarBorder(AlmadarCorner.Container, LocalAlmadarGeometry.current.borderHeavy, if (disabled) palette.border else (edge ?: palette.border)))
            .then(if (disabled) Modifier else Modifier.almadarDragSource(payload))
            .padding(horizontal = horizontal, vertical = vertical)
            .almadarLabel(action?.string("name") ?: ""),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))
    ) {
        if (iconUrl.isNotEmpty()) {
            ImageRenderer(almadarJson("src" to iconUrl, "alt" to "", "fit" to "contain", "sourceTrait" to almadarTrait(props)), Modifier.size(almadarSpace("space8")), onEvent)
        } else {
            AlmadarText(action?.string("iconEmoji", "✦") ?: "✦", variant = iconVariant)
        }
        AlmadarText(action?.string("name") ?: "", variant = labelVariant, weight = "medium")
    }
}
