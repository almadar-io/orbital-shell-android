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
fun ModuleCardRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val data = props.jsonObject("data")
    val density = LocalAlmadarDensity.current
    val persistence = data?.string("persistence") ?: ""
    val fields = data?.array("fields")?.mapNotNull { it as? JsonObject } ?: emptyList()
    val traits = data?.array("traits")?.mapNotNull { it as? JsonObject } ?: emptyList()
    val pages = data?.array("pages")?.mapNotNull { it as? JsonObject } ?: emptyList()
    val links = data?.array("externalLinks")?.mapNotNull { it as? JsonObject } ?: emptyList()
    AlmadarPanel(modifier = modifier.fillMaxWidth().widthIn(max = AlmadarSizes.maxWMd).almadarLabel(props.string("aria-label", data?.string("orbitalName") ?: ""))) {
        AlmadarText(data?.string("orbitalName") ?: "", modifier = Modifier.padding(horizontal = density.cardPaddingMd, vertical = almadarSpaceFor("sm")), variant = "body1", weight = "bold")
        AlmadarDivider()
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = density.cardPaddingMd, vertical = almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarGlyph(
                    when (persistence) { "persistent" -> "database"; "runtime" -> "refresh-cw"; "singleton" -> "circle-dot"; else -> "box" },
                    color = "primary", size = "sm", label = persistence
                )
                AlmadarText(data?.string("entityName") ?: "", variant = "small", weight = "semibold")
                AlmadarBadge(persistence, variant = "default")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                fields.forEach { field ->
                    AlmadarMonoText(field.string("name") + ": " + field.string("type"), color = LocalAlmadarTheme.current.color.mutedForeground)
                }
            }
        }
        traits.forEach { trait ->
            val name = trait.string("name")
            AlmadarDivider()
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = density.cardPaddingMd, vertical = almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                AlmadarText(name, variant = "caption", weight = "semibold")
                links.filter { it.string("traitName") == name }.forEach { link ->
                    AlmadarMonoText(
                        if (link.string("direction") == "in") "◀~~ " + link.string("eventName") else link.string("eventName") + " ~~▶",
                        color = LocalAlmadarTheme.current.color.info
                    )
                }
            }
        }
        if (pages.isNotEmpty()) {
            AlmadarDivider()
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = density.cardPaddingMd, vertical = almadarSpaceFor("sm")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
            ) {
                pages.forEach { page ->
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                        AlmadarGlyph("file", color = "muted", size = "xs")
                        AlmadarMonoText(page.string("route"), color = LocalAlmadarTheme.current.color.mutedForeground)
                    }
                }
            }
        }
    }
}
