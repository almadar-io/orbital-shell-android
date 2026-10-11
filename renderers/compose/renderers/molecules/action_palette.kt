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

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ActionPaletteRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val used = props.array("usedActionIds").mapNotNull { almadarScalar(it) }.toSet()
    val allowDuplicates = props.bool("allowDuplicates", true)
    val actions = props.array("actions").mapNotNull { it as? JsonObject }
    val palette = LocalAlmadarTheme.current.color
    val shape = almadarShape(AlmadarCorner.Container)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.card, shape)
            .then(almadarBorder(AlmadarCorner.Container, LocalAlmadarGeometry.current.borderStandard, palette.border))
            .padding(almadarSpace("space3"))
            .almadarLabel(props.string("aria-label")),
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
    ) {
        AlmadarText(props.string("label", "Actions"), variant = "body2", color = "muted", weight = "medium")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            actions.forEach { action ->
                ActionTileRenderer(
                    JsonObject(
                        mapOf(
                            "action" to action,
                            "size" to JsonPrimitive(props.string("size", "md")),
                            "disabled" to JsonPrimitive(!allowDuplicates && action.string("id") in used)
                        ) + listOfNotNull(root?.get("categoryColors")?.let { "categoryColors" to it }, almadarTrait(props)?.let { "sourceTrait" to it })
                    ),
                    Modifier, onEvent
                )
            }
        }
    }
}
