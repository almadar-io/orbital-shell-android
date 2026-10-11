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
fun OrbInspectorRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val node = props.jsonObject("node")
    val program = props.jsonObject("schema")?.string("name") ?: ""
    val closeEvent = props.string("onClose")
    Column(modifier = modifier.padding(almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AlmadarText(node?.string("orbitalName")?.ifEmpty { null } ?: "Nothing selected", weight = "semibold")
            if (closeEvent.isNotEmpty()) AlmadarButton(props, "Close", closeEvent, onEvent, variant = "ghost", size = "sm")
        }
        AlmadarText(program, variant = "caption", color = "muted")
        listOf("Trait" to "traitName", "State" to "stateName", "Event" to "transitionEvent").forEach { (label, key) ->
            val value = node?.string(key) ?: ""
            if (value.isNotEmpty()) AlmadarText("$label: $value", variant = "caption")
        }
    }
}
