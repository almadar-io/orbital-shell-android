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
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.Role

@Composable
fun LawReferenceTooltipRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val reference = props.jsonObject("reference")
    val nodes = props.children()
    var open by remember { mutableStateOf(false) }
    val law = reference?.string("law") ?: ""
    val lawName = reference?.string("lawName") ?: ""
    val article = reference?.string("article") ?: ""
    val clause = reference?.string("clause") ?: ""
    val summary = listOf(lawName.ifEmpty { law }, article, clause).filter { it.isNotEmpty() }.joinToString(" / ")
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable(role = Role.Button, onClickLabel = summary) { open = !open }
                .almadarLabel(summary),
            verticalAlignment = Alignment.CenterVertically
        ) {
            nodes.forEach { RenderChild(it, Modifier, onEvent) }
        }
        if (open) {
            Surface(
                modifier = Modifier.padding(top = almadarSpaceFor("xs")),
                color = palette.foreground,
                contentColor = palette.background,
                shape = MaterialTheme.shapes.small,
                shadowElevation = LocalAlmadarElevation.current.popoverElevation.radius
            ) {
                Column(modifier = Modifier.padding(almadarSpaceFor("sm"))) {
                    if (lawName.isNotEmpty() || law.isNotEmpty()) {
                        Text(text = lawName.ifEmpty { law }, style = almadarType("xs").copy(fontWeight = FontWeight.Bold))
                    }
                    Text(text = listOf(article, clause).filter { it.isNotEmpty() }.joinToString(", "), style = almadarType("xs"))
                }
            }
        }
    }
}
