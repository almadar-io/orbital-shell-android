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
fun AsideRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    Surface(
        modifier = modifier.almadarLabel(props.string("aria-label")),
        color = palette.muted,
        contentColor = palette.foreground,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(almadarSpaceFor("md")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
    }
}
