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
fun NotificationRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val variant = props.string("variant", "info")
    val dismissEvent = props.string("dismissEvent")
    val actionEvent = props.string("actionEvent")
    val actionLabel = props.string("actionLabel")
    val duration = props.int("duration", 5000)
    val hasAction = actionLabel.isNotEmpty() && actionEvent.isNotEmpty()
    val tone = almadarTone(variant)
    val density = LocalAlmadarDensity.current
    val badge = almadarScalar((props as? JsonObject)?.get("badge")) ?: ""
    var visible by remember(props.string("message")) { mutableStateOf(true) }
    LaunchedEffect(visible, duration, hasAction, dismissEvent) {
        if (visible && duration > 0 && !hasAction && dismissEvent.isNotEmpty()) {
            kotlinx.coroutines.delay(duration.toLong())
            visible = false
            onEvent(dismissEvent, eventEnvelope(props))
        }
    }
    if (visible) {
        AlmadarPanel(modifier = modifier.fillMaxWidth().widthIn(max = AlmadarSizes.maxWMd).almadarLabel(props.string("aria-label"))) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(modifier = Modifier.width(LocalAlmadarGeometry.current.borderHeavy).fillMaxHeight().background(tone.solid))
                Row(
                    modifier = Modifier.weight(1f).padding(density.cardPaddingMd),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                    verticalAlignment = Alignment.Top
                ) {
                    AlmadarGlyph(
                        when (variant) { "success" -> "check-circle"; "error" -> "alert-circle"; "warning" -> "alert-triangle"; else -> "info" },
                        color = variant
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        if (props.string("title").isNotEmpty()) AlmadarText(props.string("title"), variant = "h6")
                        AlmadarText(props.string("message"), variant = "small")
                        if (hasAction) AlmadarButton(props, actionLabel, actionEvent, onEvent, variant = "ghost", size = "sm")
                    }
                    if (badge.isNotEmpty()) AlmadarBadge(badge)
                    if (props.bool("dismissible", true)) {
                        AlmadarButton(
                            props, "", dismissEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                            { _, _ ->
                                visible = false
                                if (dismissEvent.isNotEmpty()) onEvent(dismissEvent, eventEnvelope(props))
                            },
                            variant = "ghost", size = "sm", icon = "x", ariaLabel = "Close toast", local = dismissEvent.isEmpty()
                        )
                    }
                }
            }
        }
    }
}
