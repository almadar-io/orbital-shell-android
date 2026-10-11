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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChartLegendRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val dot = almadarSpace("space2") * 1.25f
    val entries: @Composable () -> Unit = {
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(dot).background(parseCssColor(item.string("color"), palette.mutedForeground), MaterialTheme.shapes.large))
                AlmadarText(item.string("label"), variant = "small", color = "muted")
            }
        }
    }
    if (props.string("direction", "horizontal") == "horizontal") {
        FlowRow(
            modifier = modifier.almadarLabel(props.string("aria-label")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
        ) { entries() }
    } else {
        Column(modifier = modifier.almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) { entries() }
    }
}
