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
fun LoadingStateRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val title = props.string("title")
    val frame = if (props.bool("fullPage")) Modifier.fillMaxSize().background(palette.background.copy(alpha = 0.8f)) else Modifier.fillMaxWidth()
    Column(
        modifier = modifier.then(frame).padding(vertical = almadarSpaceFor("2xl")).almadarLabel(props.string("aria-label")),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AlmadarSpinner(size = "lg", label = props.string("message", "Loading..."))
        if (title.isNotEmpty()) AlmadarText(title, modifier = Modifier.padding(top = almadarSpaceFor("md")), variant = "h3", align = "center")
        AlmadarText(
            props.string("message", "Loading..."),
            modifier = Modifier.padding(top = almadarSpaceFor(if (title.isNotEmpty()) "sm" else "md")),
            variant = "small", color = "muted", align = "center"
        )
    }
}
