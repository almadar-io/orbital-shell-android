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
fun AlertRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val variant = props.string("variant", "info")
    val title = props.string("title")
    val message = props.string("message")
    val dismissEvent = props.string("dismissEvent")
    val palette = LocalAlmadarTheme.current.color
    val tone = almadarTone(variant)
    val iconName = props.string("icon", when (variant) {
        "success" -> "check-circle"
        "warning" -> "alert-triangle"
        "error" -> "alert-circle"
        else -> "info"
    })
    val content = props.children()
    val actions = props.array("actions")
    var dismissed by remember { mutableStateOf(false) }
    if (!dismissed) {
        Surface(
            modifier = modifier.fillMaxWidth().almadarStatus(props.string("aria-label")),
            color = palette.surface,
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderStandard, tone.solid)
        ) {
            Row(
                modifier = Modifier.padding(LocalAlmadarDensity.current.cardPaddingMd),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                verticalAlignment = Alignment.Top
            ) {
                AlmadarGlyph(iconName, color = variant)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    if (title.isNotEmpty()) AlmadarText(title, variant = "h6")
                    if (content.isNotEmpty()) content.forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
                    else if (message.isNotEmpty()) AlmadarText(message, variant = "body2")
                    if (actions.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                            actions.forEach { RenderChild(it, Modifier, onEvent) }
                        }
                    }
                }
                if (props.bool("dismissible") || dismissEvent.isNotEmpty()) {
                    AlmadarButton(
                        props, "", dismissEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                        { _, _ ->
                            dismissed = true
                            if (dismissEvent.isNotEmpty()) onEvent(dismissEvent, eventEnvelope(props))
                        },
                        variant = "ghost", icon = "x", size = "sm", ariaLabel = "Close alert", local = dismissEvent.isEmpty()
                    )
                }
            }
        }
    }
}
