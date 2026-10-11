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

import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun VoteStackRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val vote = props.string("userVote")
    val isUp = vote == "up"
    val isDown = vote == "down"
    val voteEvent = props.string("voteEvent")
    val disabled = props.bool("disabled")
    val size = props.string("size", "md")
    val horizontal = props.string("variant", "vertical") == "horizontal"
    val label = props.string("aria-label", props.string("label", "Vote"))
    val arrow: @Composable (String, Boolean, String) -> Unit = { icon, chosen, description ->
        AlmadarButton(
            props, "", voteEvent, onEvent,
            modifier = Modifier.semantics { selected = chosen },
            payload = almadarJson("next" to (if (chosen) JsonNull else JsonPrimitive(if (icon == "chevron-up") "up" else "down"))),
            variant = if (chosen) "secondary" else "ghost", icon = icon, size = if (size == "lg") "md" else "sm",
            disabled = disabled, ariaLabel = description
        )
    }
    val count: @Composable () -> Unit = {
        AlmadarText(props.int("count").toString(), modifier = Modifier.padding(horizontal = almadarSpaceFor("xs")), variant = if (size == "sm") "small" else "body1", weight = "bold", align = "center")
    }
    Surface(
        modifier = modifier.almadarAlpha(if (disabled) ALMADAR_DISABLED_ALPHA else 1f).almadarLabel(label),
        shape = MaterialTheme.shapes.small,
        color = palette.surface,
        contentColor = palette.foreground,
        border = androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderStandard, palette.border)
    ) {
        if (horizontal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                arrow("chevron-up", isUp, "Upvote")
                count()
                arrow("chevron-down", isDown, "Downvote")
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                arrow("chevron-up", isUp, "Upvote")
                count()
                arrow("chevron-down", isDown, "Downvote")
            }
        }
    }
}
