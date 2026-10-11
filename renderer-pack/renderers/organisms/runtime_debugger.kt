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
import androidx.compose.ui.text.font.FontWeight

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

@Composable
fun RuntimeDebuggerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val schema = props.jsonObject("schema")
    val mode = props.string("mode", "floating")
    var collapsed by remember { mutableStateOf(if (mode == "verify") true else props.bool("defaultCollapsed", true)) }
    val orbitals = schema?.array("orbitals")?.mapNotNull { it as? JsonObject } ?: emptyList()
    AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", "Runtime debugger"))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarGlyph("bug", color = "muted", size = "sm")
                AlmadarText("Runtime debugger", variant = "label", weight = "bold")
                AlmadarBadge(mode, variant = "info")
            }
            AlmadarLocalButton(
                props, "", { collapsed = !collapsed }, variant = "ghost", size = "sm",
                icon = if (collapsed) "chevron-down" else "chevron-up", ariaLabel = if (collapsed) "Expand debugger" else "Collapse debugger"
            )
        }
        if (!collapsed) {
            AlmadarDivider()
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = AlmadarSizes.maxWXl).verticalScroll(rememberScrollState()).padding(almadarSpaceFor("md")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
            ) {
                if (orbitals.isEmpty()) {
                    AlmadarText("Waiting for transitions", variant = "small", color = "muted")
                }
                orbitals.forEach { orbital ->
                    AlmadarMonoText(orbital.string("name"), weight = FontWeight.Bold)
                    orbital.array("traits").forEach { trait ->
                        AlmadarMonoText(almadarScalar(trait) ?: (trait as? JsonObject)?.string("name") ?: "", color = palette.mutedForeground)
                    }
                }
            }
        }
    }
}
