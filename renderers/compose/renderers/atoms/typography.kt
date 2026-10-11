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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

@Composable
fun TypographyRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val variantProp = props.string("variant")
    val level = props.int("level", 0)
    val variant = if (variantProp.isNotEmpty()) variantProp else if (level in 1..6) "h" + level else "body1"
    val sizeKey = when (props.string("size", "none")) {
        "xs" -> "xs"
        "sm" -> "sm"
        "md" -> "base"
        "lg" -> "lg"
        "xl" -> "xl"
        "2xl" -> "2xl"
        "3xl" -> "3xl"
        else -> ""
    }
    val isHeading = variant in listOf("h1", "h2", "h3", "h4", "h5", "h6", "heading", "subheading")
    val variantKey = when (variant) {
        "h1" -> "4xl"
        "h2" -> "3xl"
        "h3", "heading" -> "2xl"
        "h4" -> "xl"
        "h5", "subheading", "large" -> "lg"
        "body2", "small", "label" -> "sm"
        "caption", "overline" -> "xs"
        else -> "base"
    }
    var style = almadarType(if (sizeKey.isNotEmpty()) sizeKey else variantKey)
    if (isHeading) style = almadarHeadingStyle(style)
    if (variant == "large" || variant == "label") style = style.copy(fontWeight = FontWeight.Medium)
    if (variant == "overline") style = style.copy(fontWeight = FontWeight.Bold)
    val weight = when (props.string("weight", "none")) {
        "light" -> FontWeight.Light
        "normal" -> FontWeight.Normal
        "medium" -> FontWeight.Medium
        "semibold" -> FontWeight.SemiBold
        "bold" -> FontWeight.Bold
        else -> style.fontWeight
    }
    val color = when (props.string("color", "primary")) {
        "secondary", "muted" -> palette.mutedForeground
        "error" -> palette.error
        "success" -> palette.success
        "warning" -> palette.warning
        "inherit" -> LocalContentColor.current
        else -> palette.foreground
    }
    val align = when (props.string("align")) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.End
        "left" -> TextAlign.Start
        else -> null
    }
    val overflow = props.string("overflow")
    val maxLines = when {
        props.bool("truncate") -> 1
        overflow == "clamp-2" -> 2
        overflow == "clamp-3" -> 3
        else -> Int.MAX_VALUE
    }
    val childText = ((props as? JsonObject)?.get("children") as? JsonPrimitive)?.contentOrNull
    val raw = props.string("content", childText ?: "")
    val number = raw.toDoubleOrNull()
    val shown = when {
        number == null -> raw
        props.string("format") == "number" -> java.text.NumberFormat.getInstance().format(number)
        props.string("format") == "currency" -> java.text.NumberFormat.getCurrencyInstance().format(number)
        props.string("format") == "percent" -> java.text.NumberFormat.getPercentInstance().format(number / 100.0)
        else -> raw
    }
    val text = if (variant == "overline") shown.uppercase() else if (isHeading) almadarHeadingText(shown) else shown
    var semantic: Modifier = modifier
    if (align != null) semantic = semantic.fillMaxWidth()
    if (isHeading) semantic = semantic.semantics { heading() }
    if (text.isEmpty() && childText == null && props.children().isNotEmpty()) {
        Column(modifier = semantic) { props.children().forEach { RenderChild(it, Modifier, onEvent) } }
    } else {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = weight,
            textAlign = align,
            maxLines = maxLines,
            overflow = if (maxLines != Int.MAX_VALUE) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = semantic.almadarLabel(props.string("aria-label"))
        )
    }
}
