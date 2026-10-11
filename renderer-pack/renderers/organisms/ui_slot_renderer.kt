package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

@Composable
fun UiSlotRendererRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    Box(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        Column(modifier = Modifier.fillMaxWidth()) {
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
    }
}
