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
import androidx.compose.foundation.border

// tier: molecules — a grouped container styled by tokens only

@Composable
fun SectionRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val title = props.string("title")
    val description = props.string("description")
    val variant = props.string("variant", "default")
    val padding = when (props.string("padding", "md")) {
        "none" -> almadarSpace("space0")
        "sm" -> almadarSpace("space3")
        "lg" -> almadarSpace("space6")
        "xl" -> almadarSpace("space8")
        else -> almadarSpace("space4")
    }
    val actionNode = (props as? JsonObject)?.get("action")
    val surface = when (variant) {
        "card" -> Modifier.background(palette.card, MaterialTheme.shapes.medium).border(LocalAlmadarGeometry.current.borderStandard, palette.border, MaterialTheme.shapes.medium)
        "bordered" -> Modifier.border(LocalAlmadarGeometry.current.borderStandard, palette.border, MaterialTheme.shapes.medium)
        "filled" -> Modifier.background(palette.muted, MaterialTheme.shapes.medium)
        else -> Modifier
    }
    Column(
        modifier = modifier.fillMaxWidth().then(surface).padding(padding).almadarLabel(props.string("aria-label")),
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
    ) {
        if (title.isNotEmpty() || description.isNotEmpty() || actionNode != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    if (title.isNotEmpty()) AlmadarText(title, variant = "h4")
                    if (description.isNotEmpty()) AlmadarText(description, variant = "body1", color = "muted")
                }
                when (actionNode) {
                    is JsonObject -> RenderChild(actionNode, Modifier, onEvent)
                    is JsonArray -> Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) { actionNode.forEach { RenderChild(it, Modifier, onEvent) } }
                    else -> Unit
                }
            }
            if (props.bool("divider")) AlmadarDivider()
        }
        if (props.bool("isLoading")) {
            AlmadarSkeletonSlot(props, "text")
        } else if (props.jsonObject("error") != null) {
            AlmadarErrorText(props)
        } else {
            props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        }
    }
}
