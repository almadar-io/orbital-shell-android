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
fun LayersPanelRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val orbitals = props.jsonObject("schema")?.array("orbitals")?.mapNotNull { it as? JsonObject } ?: emptyList()
    val selected = props.string("selectedId")
    val selectEvent = props.string("onSelect")
    Column(modifier = modifier.padding(almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        orbitals.forEach { orbital ->
            val name = orbital.string("name")
            AlmadarButton(props, name, selectEvent, onEvent, payload = almadarJson("id" to name), variant = if (selected == name) "secondary" else "ghost")
            orbital.array("traits").forEach { trait ->
                val t = trait as? JsonObject
                AlmadarText(t?.string("name")?.ifEmpty { t.string("ref") } ?: "", modifier = Modifier.padding(start = almadarSpaceFor("md")), variant = "caption", color = "muted")
            }
            orbital.array("pages").forEach { page ->
                AlmadarText((page as? JsonObject)?.string("path") ?: "", modifier = Modifier.padding(start = almadarSpaceFor("md")), variant = "caption", color = "muted")
            }
        }
    }
}
