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
fun ActivityRailRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    // ActivityRail.tsx compact form (phones): one labelled row per item; the active item is primary; picking emits UI:{onChange} { id }.
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val active = props.string("active")
    val changeEvent = props.string("onChange")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        items.forEach { item ->
            val id = item.string("id")
            AlmadarButton(props, item.string("label"), changeEvent, onEvent, Modifier.fillMaxWidth(), payload = almadarJson("id" to id), variant = if (active == id) "primary" else "ghost", icon = item.string("icon"))
        }
        AlmadarNode((props as? JsonObject)?.get("footer"), onEvent)
    }
}
