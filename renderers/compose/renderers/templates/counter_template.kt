package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.ui.text.style.TextAlign

// tier: templates — CONTENT ONLY

@Composable
private fun CounterControl(
    parent: JsonElement,
    event: String,
    callback: String,
    label: String,
    icon: String,
    variant: String,
    size: String,
    disabled: Boolean,
    onEvent: (String, JsonElement?) -> Unit
) {
    AlmadarButton(
        parent, label, event.ifEmpty { callback }.ifEmpty { ALMADAR_LOCAL_ACTION },
        { _, _ ->
            if (event.isNotEmpty()) onEvent(event, eventEnvelope(parent))
            if (callback.isNotEmpty()) onEvent(callback, eventEnvelope(parent))
        },
        variant = variant, icon = icon, size = size, disabled = disabled, local = event.isEmpty() && callback.isEmpty()
    )
}

@Composable
fun CounterTemplateRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val entity = props.jsonObject("entity")
    if (entity != null) {
        val variant = props.string("variant", "standard")
        val size = props.string("size", "md")
        val count = formatFieldValue(entity["count"], "")
        val displayKey = when (size) { "sm" -> "4xl"; "lg" -> "display-2"; else -> "display-1" }
        val buttonSize = if (size == "sm" || size == "lg") size else "md"
        val incrementEvent = props.string("incrementEvent")
        val decrementEvent = props.string("decrementEvent")
        val resetEvent = props.string("resetEvent")
        val incrementCallback = props.string("onIncrement")
        val decrementCallback = props.string("onDecrement")
        val resetCallback = props.string("onReset")
        val title = props.string("title", "Counter")
        val showReset = props.bool("showReset", true)
        val decrement: @Composable (Boolean, String) -> Unit = { labelled, tone ->
            CounterControl(
                props, decrementEvent, decrementCallback, if (labelled) entity.string("decrementLabel") else "", "minus",
                tone, buttonSize, entity.bool("decrementDisabled"), onEvent
            )
        }
        val increment: @Composable (Boolean, String) -> Unit = { labelled, tone ->
            CounterControl(
                props, incrementEvent, incrementCallback, if (labelled) entity.string("incrementLabel") else "", "plus",
                tone, buttonSize, entity.bool("incrementDisabled"), onEvent
            )
        }
        val reset: @Composable (String) -> Unit = { label ->
            if (showReset) CounterControl(props, resetEvent, resetCallback, label, "rotate-ccw", "ghost", "sm", false, onEvent)
        }
        if (variant == "minimal") {
            Row(
                modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("lg"), Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                decrement(true, "secondary")
                AlmadarColoredText(count, palette.foreground, sizeKey = displayKey, heading = true, align = TextAlign.Center)
                increment(true, "secondary")
            }
        } else {
            val full = variant == "full"
            Box(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier.widthIn(max = AlmadarBreakpoints.sm).fillMaxWidth().padding(almadarSpaceFor("lg")),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor(if (full) "xl" else "lg"))
                ) {
                    AlmadarText(title, variant = "h2", color = "muted")
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                        AlmadarColoredText(count, palette.primary, sizeKey = displayKey, heading = true, align = TextAlign.Center)
                        if (full && entity.string("rangeText").isNotEmpty()) AlmadarText(entity.string("rangeText"), variant = "small", color = "muted")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")), verticalAlignment = Alignment.CenterVertically) {
                        decrement(full, "secondary")
                        increment(full, "primary")
                    }
                    reset(if (full) "Reset to 0" else "Reset")
                }
            }
        }
    }
}
