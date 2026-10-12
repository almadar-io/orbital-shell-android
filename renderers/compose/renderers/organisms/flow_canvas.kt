package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

@Composable
fun FlowCanvasRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val orbitals = props.jsonObject("schema")?.array("orbitals")?.mapNotNull { it as? JsonObject } ?: emptyList()
    val openEvent = props.string("openBehaviorEvent")
    val traitsEvent = props.string("openTraitsEvent")
    Column(modifier = modifier.padding(almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
        if (orbitals.isEmpty()) AlmadarText("No program", variant = "caption", color = "muted")
        orbitals.forEach { orbital ->
            val name = orbital.string("name")
            if (openEvent.isEmpty()) AlmadarText(name, weight = "semibold")
            else AlmadarButton(props, name, openEvent, onEvent, payload = almadarJson("name" to name), variant = "ghost")
            AlmadarText(orbital.jsonObject("entity")?.string("name") ?: "", variant = "caption", color = "muted")
            AlmadarBadge("${orbital.array("traits").size} traits · ${orbital.array("pages").size} pages")
            if (traitsEvent.isNotEmpty()) AlmadarButton(props, "Open traits", traitsEvent, onEvent, payload = almadarJson("orbital" to name), variant = "ghost")
        }
    }
}
