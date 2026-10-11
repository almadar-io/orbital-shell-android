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

@Composable
fun BadgeRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val variant = props.string("variant", "default")
    val size = props.string("size", "sm")
    val label = props.string("label")
    val amount = props.float("amount", Float.NaN)
    val amountText = if (amount.isNaN()) "" else if (amount % 1f == 0f) amount.toInt().toString() else amount.toString()
    val text = if (amountText.isNotEmpty()) (if (label.isNotEmpty()) label + " " else "") + amountText else label
    val iconName = props.string("icon")
    val removeEvent = props.string("onRemove")
    val removeLabel = props.string("removeLabel", "Remove " + text)
    val tone = almadarTone(variant)
    val solid = variant == "primary" || variant == "secondary"
    val bordered = variant != "primary" && variant != "secondary"
    val container = when {
        solid -> tone.solid
        variant == "default" || variant == "neutral" -> palette.muted
        else -> tone.solid.copy(alpha = 0.1f)
    }
    val content = when {
        solid -> tone.onSolid
        variant == "neutral" -> palette.mutedForeground
        else -> palette.foreground
    }
    val borderColor = if (variant == "default" || variant == "neutral") palette.border else tone.solid
    val textStyle = almadarType(when (size) { "lg" -> "base"; "md" -> "sm"; else -> "xs" })
    val hPad = almadarSpaceFor(when (size) { "lg" -> "md"; "md" -> "sm"; else -> "sm" })
    val vPad = almadarSpaceFor(when (size) { "lg" -> "sm"; "md" -> "xs"; else -> "xs" })
    val iconSize = LocalAlmadarIconography.current.defaultSize
    if (text.isNotEmpty() || iconName.isNotEmpty() || removeEvent.isNotEmpty()) {
        Surface(
            modifier = modifier.almadarLabel(props.string("aria-label")).then(if (bordered) almadarBorder(AlmadarCorner.Pill, LocalAlmadarGeometry.current.borderStandard, borderColor) else Modifier),
            shape = almadarShape(AlmadarCorner.Pill),
            color = container,
            contentColor = content,
            border = null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = hPad, vertical = vPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
            ) {
                if (iconName.isNotEmpty()) AlmadarIcon(name = iconName, modifier = Modifier.size(iconSize))
                if (text.isNotEmpty()) Text(text = text, style = textStyle.copy(fontWeight = FontWeight.Bold))
                else props.children().forEach { RenderChild(it, Modifier, onEvent) }
                if (removeEvent.isNotEmpty()) {
                    AlmadarIcon(
                        name = "close",
                        description = removeLabel,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onEvent(removeEvent, eventEnvelope(props)) }
                            .almadarTag(almadarActionId(removeEvent, props))
                    )
                }
            }
        }
    }
}
