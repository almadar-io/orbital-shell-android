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
import androidx.compose.ui.graphics.Color

// tier: molecules — a grouped container styled by tokens only

@Composable
fun DocTocRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val activeId = props.string("activeId")
    Column(modifier = modifier.fillMaxWidth().almadarLandmark(props.string("aria-label", "Table of contents"))) {
        items.forEach { item ->
            val id = item.string("id")
            val active = id == activeId
            val href = "#" + id
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = if (item.int("level") >= 3) almadarSpaceFor("md") else Dp.Hairline),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(LocalAlmadarGeometry.current.borderHeavy).height(LocalAlmadarDensity.current.rowHeightCompact).background(if (active) palette.accent else Color.Transparent))
                AlmadarButton(
                    props, item.string("label"), NAVIGATE_EVENT, onEvent,
                    modifier = Modifier.weight(1f),
                    payload = almadarJson("href" to href, "url" to href),
                    variant = if (active) "secondary" else "ghost", size = "sm"
                )
            }
        }
    }
}
