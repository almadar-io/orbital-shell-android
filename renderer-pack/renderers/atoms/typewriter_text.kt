package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: atoms — a single Material 3 control

@Composable
fun TypewriterTextRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val full = props.string("text")
    val speed = props.int("speed", 40).toLong()
    val delayMs = props.int("startDelay", 0).toLong()
    val completeEvent = props.string("onComplete")
    var shown by remember(full) { mutableStateOf("") }
    LaunchedEffect(full, speed, delayMs) {
        shown = ""
        kotlinx.coroutines.delay(delayMs)
        for (count in 1..full.length) {
            shown = full.substring(0, count)
            kotlinx.coroutines.delay(speed)
        }
        if (completeEvent.isNotEmpty()) onEvent(completeEvent, eventEnvelope(props))
    }
    Text(
        text = shown,
        style = almadarType("base"),
        color = LocalAlmadarTheme.current.color.foreground,
        modifier = modifier.almadarLabel(props.string("aria-label", full))
    )
}
