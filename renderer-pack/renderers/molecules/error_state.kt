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
fun ErrorStateRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val retryEvent = props.string("retryEvent")
    val title = props.string("title", "Something went wrong")
    val message = props.string("message", props.string("description", "An error occurred"))
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("2xl")).almadarLabel(props.string("aria-label")),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
    ) {
        Box(modifier = Modifier.background(palette.error.copy(alpha = 0.1f), MaterialTheme.shapes.large).padding(almadarSpaceFor("sm"))) {
            AlmadarGlyph("alert-circle", color = "error", size = "xl")
        }
        AlmadarText(title, variant = "h3", align = "center")
        AlmadarText(message, variant = "small", color = "muted", align = "center")
        if (retryEvent.isNotEmpty()) {
            AlmadarButton(props, "Try again", retryEvent, onEvent, modifier = Modifier.padding(top = almadarSpaceFor("md")), variant = "secondary")
        }
    }
}
