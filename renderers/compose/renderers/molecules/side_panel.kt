package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// tier: molecules — a grouped container styled by tokens only

@Composable
fun SidePanelRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val closeEvent = props.string("closeEvent", props.string("onClose"))
    val right = props.string("position", "right") == "right"
    if (!props.bool("isOpen")) {
        Box(modifier = modifier)
    } else {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = if (right) Alignment.TopEnd else Alignment.TopStart) {
            AlmadarPanel(material = true, modifier = Modifier.widthIn(max = AlmadarSizes.w96).fillMaxWidth().almadarLabel(props.string("aria-label", props.string("title")))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlmadarText(props.string("title"), modifier = Modifier.weight(1f), variant = "h6")
                    AlmadarButton(props, "", closeEvent.ifEmpty { ALMADAR_LOCAL_ACTION }, onEvent, variant = "ghost", size = "sm", icon = "x", ariaLabel = "Close panel", local = closeEvent.isEmpty())
                }
                AlmadarDivider()
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(almadarSpaceFor("md")),
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
                ) {
                    props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
                }
            }
        }
    }
}
