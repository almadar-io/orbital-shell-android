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

// tier: templates — CONTENT ONLY

@Composable
fun GameShellRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val showTopBar = props.bool("showTopBar", true)
    val slot: @Composable (String, Modifier) -> Unit = { key, slotModifier ->
        when (val node = (props as? JsonObject)?.get(key)) {
            is JsonObject -> RenderChild(node, slotModifier, onEvent)
            is JsonArray -> Column(modifier = slotModifier, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                node.forEach { RenderChild(it, Modifier, onEvent) }
            }
            else -> Unit
        }
    }
    val hasHud = (props as? JsonObject)?.get("hud").let { it is JsonObject || it is JsonArray }
    Box(modifier = modifier.fillMaxSize().background(palette.background).almadarLabel(props.string("aria-label"))) {
        Box(modifier = Modifier.fillMaxSize()) {
            props.children().forEach { RenderChild(it, Modifier.fillMaxSize(), onEvent) }
        }
        if (showTopBar || hasHud) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(almadarSpaceFor("sm")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                verticalAlignment = Alignment.Top
            ) {
                if (showTopBar) {
                    AlmadarPanel {
                        AlmadarText(
                            props.string("appName", "Game"),
                            modifier = Modifier.padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("xs")),
                            variant = "h5", weight = "bold", truncate = true
                        )
                    }
                }
                if (hasHud) slot("hud", Modifier.weight(1f))
            }
        }
        slot("controls", Modifier.align(Alignment.BottomStart).padding(almadarSpaceFor("md")))
        slot("addons", Modifier.align(Alignment.BottomEnd).padding(almadarSpaceFor("md")))
        CompositionLocalProvider(LocalAlmadarGameOverlay provides true) { slot("overlay", Modifier.align(Alignment.Center)) }
    }
}
