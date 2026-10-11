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
import androidx.compose.ui.draw.clip

// tier: molecules — a grouped container styled by tokens only

@Composable
fun ChangeListRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val inspectEvent = props.string("inspectEvent")
    val items = ((props as? JsonObject)?.get("changes") as? JsonArray)
        ?.mapNotNull { it as? JsonObject }
        ?.filter { it.string("title").isNotEmpty() && it.string("type") in listOf("added", "modified", "removed") }
        ?: emptyList()
    if (items.isEmpty()) {
        AlmadarText("No changes in this version.", modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")), variant = "body2", color = "muted", align = "center")
    } else {
        Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            items.forEach { change ->
                val type = change.string("type")
                val icon = when (type) { "added" -> "plus-circle"; "modified" -> "edit-3"; else -> "minus-circle" }
                val tone = when (type) { "added" -> "success"; "modified" -> "warning"; else -> "error" }
                val before = change.string("before")
                val after = change.string("after")
                val description = change.string("description")
                AlmadarPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                            AlmadarGlyph(icon, color = tone, size = "sm")
                            AlmadarText(change.string("title"), modifier = Modifier.weight(1f, fill = false), variant = "body2", weight = "semibold", truncate = true)
                            AlmadarBadge(type.replaceFirstChar { it.uppercase() }, variant = tone)
                        }
                        if (description.isNotEmpty()) AlmadarText(description, variant = "caption", color = "muted")
                        if (type == "modified" && (before.isNotEmpty() || after.isNotEmpty())) {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                                listOf(Triple("Before", before, palette.error), Triple("After", after, palette.success)).forEach { (heading, text, tint) ->
                                    if (text.isNotEmpty()) {
                                        val shape = androidx.compose.foundation.shape.RoundedCornerShape(geometry.radiusContainer)
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                                .clip(shape)
                                                .background(tint.copy(alpha = 0.05f))
                                                .border(geometry.borderStandard, tint.copy(alpha = 0.2f), shape)
                                                .padding(almadarSpaceFor("sm"))
                                        ) {
                                            AlmadarText(heading, variant = "caption", color = "muted", weight = "semibold")
                                            AlmadarMonoText(text)
                                        }
                                    }
                                }
                            }
                        }
                        if (inspectEvent.isNotEmpty()) {
                            AlmadarButton(
                                props, "Inspect", inspectEvent, onEvent,
                                payload = almadarJson("title" to change.string("title"), "type" to type),
                                variant = "ghost", icon = "external-link", size = "sm"
                            )
                        }
                    }
                }
            }
        }
    }
}
