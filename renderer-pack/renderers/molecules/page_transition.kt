package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: molecules — a grouped container styled by tokens only

@Composable
fun PageTransitionRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")).almadarSurfaceEnter("pageEnter", AlmadarEnterTiming.Page, replayKey = props.string("locationKey"))) {
        props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
    }
}
