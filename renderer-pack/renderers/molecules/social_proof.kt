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

import androidx.compose.ui.platform.LocalConfiguration

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SocialProofRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val variant = props.string("variant", "logos")
    val base = LocalAlmadarIconography.current.defaultSize
    Box(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        if (variant == "quotes") {
            val wide = LocalConfiguration.current.screenWidthDp.dp >= AlmadarBreakpoints.sm
            val columns = if (!wide) 1 else if (items.size >= 3) 3 else if (items.size >= 2) 2 else 1
            Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
                items.chunked(columns).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
                        rowItems.forEach { item ->
                            AlmadarPanel(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                Column(modifier = Modifier.padding(almadarSpaceFor("md")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                                    if (item.string("quote").isNotEmpty()) AlmadarText("“" + item.string("quote") + "”", variant = "body1")
                                    AlmadarText(item.string("name"), variant = "caption", color = "muted")
                                }
                            }
                        }
                        repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        } else if (variant == "badges") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"), Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) {
                items.forEach { AlmadarBadge(it.string("name"), size = "md") }
            }
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xl"), Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xl"))
            ) {
                items.forEach { item ->
                    Column(modifier = Modifier.almadarAlpha(0.6f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        if (item.string("logo").isNotEmpty()) {
                            ImageRenderer(almadarJson("src" to item.string("logo"), "alt" to item.string("name"), "fit" to "contain"), Modifier.size(base * 3f), AlmadarNoEvent)
                        }
                        AlmadarText(item.string("name"), variant = "caption", color = "muted")
                    }
                }
            }
        }
    }
}
