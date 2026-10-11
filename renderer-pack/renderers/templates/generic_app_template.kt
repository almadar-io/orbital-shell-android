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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// tier: templates — CONTENT ONLY

@Composable
fun GenericAppTemplateRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val entity = props.jsonObject("entity")
    val subtitle = props.string("subtitle")
    val footer = root?.get("footer")
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("md")),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                AlmadarText(props.string("title"), variant = "h3")
                if (subtitle.isNotEmpty()) AlmadarText(subtitle, variant = "body2", color = "secondary")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarNode(root?.get("headerActions"), onEvent)
            }
        }
        AlmadarDivider()
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(almadarSpaceFor("lg"))
        ) {
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
        if (footer != null && footer !is JsonNull) {
            AlmadarDivider()
            Box(modifier = Modifier.fillMaxWidth().background(palette.muted).padding(almadarSpaceFor("md"))) {
                AlmadarNode(footer, onEvent, Modifier.fillMaxWidth())
            }
        }
    }
}
