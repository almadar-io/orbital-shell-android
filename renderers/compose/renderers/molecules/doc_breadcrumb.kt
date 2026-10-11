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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DocBreadcrumbRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val items = props.array("items").mapNotNull { it as? JsonObject }
    FlowRow(
        modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", "Breadcrumb")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            val route = almadarNavigate(item)
            if (index > 0) AlmadarGlyph("chevron-right", color = "muted", size = "xs")
            if (isLast || route.event.isEmpty()) {
                AlmadarText(item.string("label"), variant = "caption", color = if (isLast) "primary" else "muted", weight = if (isLast) "medium" else "none")
            } else {
                AlmadarButton(props, item.string("label"), route.event, onEvent, payload = route.payload, variant = "link", size = "sm")
            }
        }
    }
}
