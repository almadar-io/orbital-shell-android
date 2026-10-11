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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

// tier: molecules — a grouped container styled by tokens only

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CommitRowRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val sha = props.string("sha")
    val selectEvent = props.string("selectEvent")
    val restoreEvent = props.string("restoreEvent")
    val selected = props.bool("selected")
    val current = props.bool("current")
    val kind = props.string("kind")
    val author = props.string("author")
    val timestamp = props.string("timestamp")
    val additions = (props as? JsonObject)?.get("additions") as? JsonPrimitive
    val deletions = (props as? JsonObject)?.get("deletions") as? JsonPrimitive
    val kindIcon = when (kind) {
        "user" -> "user"
        "agent" -> "sparkles"
        "restore" -> "rotate-ccw"
        "undo" -> "undo-2"
        "redo" -> "redo-2"
        "snapshot" -> "camera"
        "merge" -> "git-merge"
        else -> ""
    }
    var select: Modifier = Modifier
    if (selectEvent.isNotEmpty()) {
        select = select
            .clickable(role = androidx.compose.ui.semantics.Role.Button) { onEvent(selectEvent, eventEnvelope(props, almadarJson("sha" to sha))) }
            .almadarActionTag(selectEvent, props)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(geometry.radiusInteractive))
            .background(if (selected) palette.muted else Color.Transparent)
            .padding(horizontal = almadarSpaceFor("sm"), vertical = almadarSpaceFor("xs"))
            .almadarLabel(props.string("aria-label")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).then(select)) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                if (kindIcon.isNotEmpty()) AlmadarGlyph(kindIcon, color = "muted", size = "xs", label = kind)
                AlmadarText(props.string("message"), variant = "caption", truncate = true)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalArrangement = Arrangement.Center) {
                AlmadarMonoText(if (sha.length > 7) sha.substring(0, 7) else sha, size = "xs", color = palette.mutedForeground)
                if (author.isNotEmpty()) AlmadarText(author, variant = "caption", color = "muted")
                if (timestamp.isNotEmpty()) AlmadarText(timestamp, variant = "caption", color = "muted")
                if (additions != null) AlmadarBadge("+" + additions.content, variant = "success")
                if (deletions != null) AlmadarBadge("-" + deletions.content, variant = "error")
                if (current) AlmadarBadge("Current")
            }
        }
        if (restoreEvent.isNotEmpty() && !current) {
            AlmadarButton(props, "Restore", restoreEvent, onEvent, payload = almadarJson("sha" to sha), variant = "ghost", icon = "rotate-ccw", size = "sm")
        }
    }
}
