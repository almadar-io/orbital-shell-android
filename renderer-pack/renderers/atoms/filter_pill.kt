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
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.Role

@Composable
fun FilterPillRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val variant = props.string("variant", "default")
    val size = props.string("size", "sm")
    val label = props.string("label")
    val iconName = props.string("icon")
    val clickEvent = props.string("clickEvent")
    val removeEvent = props.string("removeEvent")
    val removable = props.bool("removable", true) && removeEvent.isNotEmpty()
    val tone = almadarTone(variant)
    val neutral = variant == "default" || variant == "neutral"
    val container = if (neutral) palette.muted else tone.solid.copy(alpha = 0.1f)
    val content = if (variant == "neutral") palette.mutedForeground else palette.foreground
    val payload = JsonObject(mapOf("label" to JsonPrimitive(label)))
    val iconSize = LocalAlmadarIconography.current.defaultSize * (if (size == "lg") 1f else 0.875f)
    var body: Modifier = Modifier
    if (clickEvent.isNotEmpty()) {
        body = body
            .clickable(role = Role.Button, onClickLabel = label) { onEvent(clickEvent, eventEnvelope(props, payload)) }
            .almadarTag(almadarActionId(clickEvent, props))
    }
    Surface(
        modifier = modifier.almadarLabel(props.string("aria-label")).then(almadarBorder(AlmadarCorner.Pill, LocalAlmadarGeometry.current.borderStandard, if (neutral) palette.border else tone.solid)),
        shape = almadarShape(AlmadarCorner.Pill),
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = body.padding(horizontal = almadarSpaceFor(if (size == "lg") "md" else "sm"), vertical = almadarSpaceFor("xs")),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
        ) {
            if (iconName.isNotEmpty()) AlmadarIcon(name = iconName, modifier = Modifier.size(iconSize))
            Text(text = label, style = almadarType(when (size) { "lg" -> "base"; "md" -> "sm"; else -> "xs" }).copy(fontWeight = FontWeight.Medium))
            if (removable) {
                AlmadarIcon(
                    name = "close",
                    description = "Remove " + label,
                    modifier = Modifier
                        .size(iconSize)
                        .clickable(role = Role.Button) { onEvent(removeEvent, eventEnvelope(props, payload)) }
                        .almadarTag(almadarActionId(removeEvent, props))
                )
            }
        }
    }
}
