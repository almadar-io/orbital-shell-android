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
fun DateRangeSelectorRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val defaults = listOf("1W" to "week", "1M" to "month", "3M" to "3months", "1Y" to "year")
    val options = props.array("options").mapNotNull { it as? JsonObject }.map { it.string("label") to it.string("value") }.ifEmpty { defaults }
    val initial = props.string("selected", "month")
    var selected by remember(initial) { mutableStateOf(initial) }
    FlowRow(
        modifier = modifier.almadarLabel(props.string("aria-label")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
    ) {
        options.forEach { (label, value) ->
            AlmadarLocalButton(props, label, { selected = value }, variant = if (selected == value) "primary" else "ghost", size = "sm")
        }
    }
}
