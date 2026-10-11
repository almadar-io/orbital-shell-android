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

// tier: atoms — a single Material 3 control

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun ButtonRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val density = LocalAlmadarDensity.current
    val label = props.string("label")
    val action = props.string("action")
    val disabled = props.bool("disabled")
    val loading = props.bool("isLoading")
    val variant = props.string("variant", "primary")
    val leftIcon = props.string("leftIcon", props.string("icon"))
    val rightIcon = props.string("rightIcon", props.string("iconRight"))
    val height = when (props.string("size", "md")) {
        "sm" -> density.buttonHeightSm
        "lg" -> density.buttonHeightLg
        else -> density.buttonHeightMd
    }
    val iconSize = LocalAlmadarIconography.current.defaultSize
    val gap = almadarSpaceFor("sm")
    val shape = almadarShape(AlmadarCorner.Interactive)
    val borderWidth = LocalAlmadarGeometry.current.borderStandard
    val source = remember { MutableInteractionSource() }
    val raised = almadarInteractiveShadow(source, AlmadarCorner.Interactive)
    val href = props.string("href")
    val uriHandler = LocalUriHandler.current
    val onClick = {
        if (!disabled && !loading) {
            if (action.isNotEmpty()) onEvent(action, eventEnvelope(props))
            if (href.isNotEmpty() && !href.startsWith("#")) {
                if (almadarIsAbsoluteHref(href)) uriHandler.openUri(href) else onEvent(NAVIGATE_EVENT, eventEnvelope(JsonObject(emptyMap()), almadarJson("url" to href)))
            }
        }
    }
    val content: @Composable RowScope.() -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.almadarAlpha(if (loading) 0f else 1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                if (leftIcon.isNotEmpty()) AlmadarIcon(name = leftIcon, modifier = Modifier.size(iconSize))
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = almadarType(if (props.string("size", "md") == "lg") "base" else "sm"),
                        textDecoration = if (variant == "link") TextDecoration.Underline else null
                    )
                }
                if (rightIcon.isNotEmpty()) AlmadarIcon(name = rightIcon, modifier = Modifier.size(iconSize))
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(iconSize),
                    strokeWidth = LocalAlmadarGeometry.current.borderHeavy,
                    color = LocalContentColor.current
                )
            }
        }
    }
    val padding = if (label.isEmpty()) PaddingValues(horizontal = almadarSpaceFor("sm")) else ButtonDefaults.ContentPadding
    val tone = almadarTone(variant)
    val control = Modifier
        .heightIn(min = height)
        .almadarTag(almadarActionId(action, props))
        .almadarLabel(props.string("aria-label"))
    val framed = control.then(raised)
    Box(modifier = modifier, propagateMinConstraints = true) {
        when (variant) {
            "secondary" -> OutlinedButton(
                onClick = onClick, enabled = !disabled, shape = shape, contentPadding = padding, interactionSource = source,
                modifier = framed.then(almadarBorder(AlmadarCorner.Interactive, borderWidth, palette.accent)), border = null,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.foreground),
                content = content
            )
            "ghost" -> TextButton(
                onClick = onClick, enabled = !disabled, shape = shape, contentPadding = padding, modifier = control, interactionSource = source,
                colors = ButtonDefaults.textButtonColors(contentColor = palette.mutedForeground),
                content = content
            )
            "link" -> TextButton(
                onClick = onClick, enabled = !disabled, shape = shape, modifier = control, interactionSource = source,
                contentPadding = PaddingValues(),
                colors = ButtonDefaults.textButtonColors(contentColor = palette.mutedForeground),
                content = content
            )
            "danger", "destructive", "success", "warning" -> OutlinedButton(
                onClick = onClick, enabled = !disabled, shape = shape, contentPadding = padding, interactionSource = source,
                modifier = framed.then(almadarBorder(AlmadarCorner.Interactive, borderWidth, tone.solid)), border = null,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.surface, contentColor = palette.foreground),
                content = content
            )
            "default" -> FilledTonalButton(
                onClick = onClick, enabled = !disabled, shape = shape, contentPadding = padding, interactionSource = source,
                modifier = framed.then(almadarBorder(AlmadarCorner.Interactive, borderWidth, palette.border)),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = palette.secondary, contentColor = palette.secondaryForeground),
                content = content
            )
            else -> Button(
                onClick = onClick, enabled = !disabled, shape = shape, contentPadding = padding, modifier = framed, interactionSource = source,
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.primaryForeground),
                content = content
            )
        }
    }
}
