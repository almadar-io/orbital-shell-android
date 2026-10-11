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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip

// tier: molecules — a grouped container styled by tokens only

@Composable
fun StateJsonViewRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    var expanded by remember { mutableStateOf(props.bool("defaultExpanded")) }
    val palette = LocalAlmadarTheme.current.color
    val shape = almadarShape(AlmadarCorner.Container)
    fun quoted(text: String): String = JsonPrimitive(text).toString()
    fun block(key: String, rows: List<String>): String =
        if (rows.isEmpty()) "  ${quoted(key)}: []" else "  ${quoted(key)}: [\n" + rows.joinToString(",\n") + "\n  ]"
    val stateRows = props.array("states").map { "    " + quoted((it as? JsonPrimitive)?.contentOrNull ?: "") }
    val transitionRows = props.array("transitions").map { transition ->
        "    {\n      \"from\": ${quoted(transition.string("from"))},\n      \"to\": ${quoted(transition.string("to"))},\n      \"event\": ${quoted(transition.string("event"))}\n    }"
    }
    val json = "{\n  \"name\": ${quoted(props.string("name"))},\n  \"initialState\": ${quoted(props.string("initialState"))},\n" +
        block("states", stateRows) + ",\n" + block("transitions", transitionRows) + "\n}"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(almadarBorder(AlmadarCorner.Container, LocalAlmadarGeometry.current.borderStandard, palette.border))
            .almadarLabel(props.string("aria-label"))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(palette.muted).padding(almadarSpace("space2")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlmadarText(props.string("label", "View Code"), variant = "caption", color = "muted", weight = "medium")
            Spacer(Modifier.weight(1f))
            AlmadarLocalButton(props, if (expanded) "Hide JSON" else "Show JSON", { expanded = !expanded }, variant = "ghost", size = "sm")
        }
        if (expanded) {
            Box(modifier = Modifier.fillMaxWidth().background(palette.background).horizontalScroll(rememberScrollState()).padding(almadarSpace("space3"))) {
                AlmadarMonoText(json, size = "xs")
            }
        }
    }
}
