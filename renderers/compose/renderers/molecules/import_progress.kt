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

// tier: molecules — a grouped container styled by tokens only

@Composable
fun ImportProgressRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val step = props.string("step")
    val pipeline = listOf("fetching", "mapping", "reviewing", "committing")
    val defaults = mapOf("fetching" to "Fetching", "mapping" to "Mapping", "reviewing" to "Reviewing", "committing" to "Committing", "done" to "Done", "failed" to "Failed")
    val labels = (props as? JsonObject)?.get("labels") as? JsonObject
    val label: (String) -> String = { key -> labels?.string(key)?.takeIf { it.isNotEmpty() } ?: (defaults[key] ?: key) }
    val finished = step == "done" || step == "failed"
    val current = if (finished) pipeline.size else pipeline.indexOf(step)
    val counts = (props as? JsonObject)?.get("counts") as? JsonObject
    val palette = LocalAlmadarTheme.current.color
    val hairline = LocalAlmadarGeometry.current.borderHairline
    val rule: @Composable () -> Unit = { Box(modifier = Modifier.width(almadarSpace("space4")).height(hairline).background(palette.border)) }
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
            pipeline.forEachIndexed { index, key ->
                val complete = index < current
                val active = index == current
                if (index > 0) rule()
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                    AlmadarGlyph(if (complete) "check" else if (active) "loader" else "circle", color = if (complete) "success" else if (active) "primary" else "muted", size = "sm")
                    AlmadarText(label(key), variant = "caption", color = if (active) "primary" else "muted")
                }
            }
            if (finished) {
                rule()
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                    AlmadarGlyph(if (step == "done") "check" else "x", color = if (step == "done") "success" else "error", size = "sm")
                    AlmadarText(label(step), variant = "caption", color = if (step == "done") "success" else "error")
                }
            }
        }
        if (counts != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                if (counts["staged"] != null) AlmadarBadge("Staged ${counts.int("staged")}")
                if (counts["committed"] != null) AlmadarBadge("Committed ${counts.int("committed")}", variant = "success")
                if (counts["failed"] != null) AlmadarBadge("Failed ${counts.int("failed")}", variant = "danger")
            }
        }
    }
}
