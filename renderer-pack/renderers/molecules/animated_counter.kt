package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.animation.core.*

// tier: molecules — a grouped container styled by tokens only

@Composable
fun AnimatedCounterRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val raw = props.string("value")
    val match = Regex("^([^0-9-]*)(-?[0-9]+(?:\\.[0-9]+)?)(.*)$").find(raw)
    val number = match?.groupValues?.get(2)?.toFloatOrNull()
    val decimals = match?.groupValues?.get(2)?.substringAfter('.', "")?.length ?: 0
    val animated by animateFloatAsState(
        targetValue = number ?: 0f,
        animationSpec = tween(durationMillis = props.int("duration", 1500)),
        label = "counter"
    )
    val body = when (props.string("format")) {
        "currency" -> formatFieldValue(JsonPrimitive(animated.toDouble()), "currency")
        "percent" -> Math.round(animated).toString() + "%"
        "number" -> java.text.NumberFormat.getInstance().format(Math.round(animated))
        else -> "%.${decimals}f".format(animated)
    }
    val text = if (match == null) raw else props.string("prefix") + match.groupValues[1] + body + match.groupValues[3] + props.string("suffix")
    val label = props.string("label")
    Column(
        modifier = modifier.almadarLabel(props.string("aria-label", label.ifEmpty { text })),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
    ) {
        AlmadarText(text, variant = "h2", align = "center")
        if (label.isNotEmpty()) AlmadarText(label, variant = "body2", color = "muted", align = "center")
    }
}
