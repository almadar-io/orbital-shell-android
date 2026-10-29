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
import androidx.compose.foundation.clickable

// tier: molecules — a grouped container styled by tokens only

@Composable
fun DocPaginationRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val prev = props.jsonObject("prev")
    val next = props.jsonObject("next")
    val link: @Composable (JsonObject?, Boolean, Modifier) -> Unit = { target, forward, slot ->
        if (target == null) {
            Spacer(modifier = slot)
        } else {
            val href = target.string("href")
            Surface(
                modifier = slot.clickable(role = androidx.compose.ui.semantics.Role.Button) {
                    onEvent(NAVIGATE_EVENT, eventEnvelope(props, almadarJson("href" to href, "url" to href)))
                }.almadarActionTag(NAVIGATE_EVENT, props),
                shape = MaterialTheme.shapes.medium,
                color = palette.card,
                contentColor = palette.foreground,
                border = androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderStandard, palette.border)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("md")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"), if (forward) Alignment.End else Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!forward) AlmadarGlyph("arrow-left", color = "muted", size = "md")
                    Column(modifier = Modifier.weight(1f, fill = false), horizontalAlignment = if (forward) Alignment.End else Alignment.Start) {
                        if (target.string("category").isNotEmpty()) AlmadarText(target.string("category"), variant = "caption", color = "muted")
                        AlmadarText(target.string("label"), variant = "body1", align = if (forward) "right" else "left")
                    }
                    if (forward) AlmadarGlyph("arrow-right", color = "muted", size = "md")
                }
            }
        }
    }
    if (prev == null && next == null) {
        Box(modifier = modifier)
    } else {
        Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min).almadarLandmark(props.string("aria-label")), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
            link(prev, false, Modifier.weight(1f).fillMaxHeight())
            link(next, true, Modifier.weight(1f).fillMaxHeight())
        }
    }
}
