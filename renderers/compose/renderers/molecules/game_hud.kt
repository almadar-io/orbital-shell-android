package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.border

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GameHudRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val position = props.string("position", "top")
    val size = props.string("size", "md")
    val variant = props.string("variant", "floating")
    val transparent = props.bool("transparent", true)
    val elementLabels = mapOf("health-bar" to "Health", "score-display" to "Score", "lives" to "Lives", "timer" to "Time")
    val stats: List<JsonObject> = (root?.get("stats") as? JsonArray ?: root?.get("items") as? JsonArray)?.mapNotNull { it as? JsonObject }
        ?: (root?.get("elements") as? JsonArray)?.mapNotNull { it as? JsonObject }?.map { element ->
            val bound = element.string("bind").split(".")
            JsonObject(
                mapOf("label" to JsonPrimitive(element.string("label").ifEmpty { elementLabels[element.string("type")] ?: element.string("type") })) +
                    (if (bound.size > 1) mapOf("source" to JsonPrimitive(bound[0]), "field" to JsonPrimitive(bound[1])) else emptyMap()) +
                    listOf("value", "icon", "assetUrl", "format", "max").mapNotNull { key -> element[key]?.let { key to it } }
            )
        }
        ?: listOf(
            almadarJson("label" to "HP", "value" to 75, "max" to 100, "format" to "bar", "variant" to "danger"),
            almadarJson("label" to "MP", "value" to 40, "max" to 60, "format" to "bar", "variant" to "primary"),
            almadarJson("label" to "Score", "value" to 4200, "format" to "number", "variant" to "warning"),
            almadarJson("label" to "Level", "value" to 4, "format" to "number", "variant" to "default")
        )
    val badge: @Composable (JsonObject) -> Unit = { stat ->
        StatBadgeRenderer(stat.merging(mapOf("size" to JsonPrimitive(size))), Modifier, onEvent)
    }
    val mid = (stats.size + 1) / 2
    val cardColor = if (transparent) palette.card.copy(alpha = 0.35f) else palette.card
    val border = androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderStandard, palette.border)
    if (position == "corners") {
        Box(modifier = modifier.fillMaxSize().almadarLabel(props.string("aria-label"))) {
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(almadarSpaceFor("md")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) { stats.take(mid).forEach { badge(it) } }
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(almadarSpaceFor("md")),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) { stats.drop(mid).forEach { badge(it) } }
        }
    } else if ((position == "top" || position == "bottom") && variant == "bar") {
        Surface(
            modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")),
            color = cardColor, contentColor = palette.foreground, border = border
        ) {
            Row(
                modifier = Modifier.padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) { stats.take(mid).forEach { badge(it) } }
                if (stats.size > mid) Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) { stats.drop(mid).forEach { badge(it) } }
            }
        }
    } else {
        Surface(
            modifier = modifier.almadarLabel(props.string("aria-label")),
            shape = MaterialTheme.shapes.medium, color = cardColor, contentColor = palette.foreground, border = border
        ) {
            FlowRow(
                modifier = Modifier.padding(almadarSpaceFor("sm")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) { stats.forEach { badge(it) } }
        }
    }
}
