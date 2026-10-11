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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip

// tier: molecules — a grouped container styled by tokens only

@Composable
fun FormSectionHeaderRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val toggleEvent = props.string("onToggle")
    val collapsed = props.bool("isCollapsed")
    val hasErrors = props.bool("hasErrors")
    val complete = props.bool("isComplete")
    val badge = props.string("badge")
    val subtitle = props.string("subtitle")
    val icon = props.string("icon")
    val badgeVariant = if (hasErrors) "danger" else if (complete) "success" else props.string("badgeVariant", "default")
    val statusIcon = if (hasErrors) "alert-circle" else if (complete) "check-circle" else ""
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(geometry.radiusContainer)
    var surface: Modifier = modifier.fillMaxWidth().clip(shape).background(palette.muted).border(geometry.borderStandard, palette.border, shape)
    if (toggleEvent.isNotEmpty()) {
        surface = surface
            .clickable(role = androidx.compose.ui.semantics.Role.Button) { onEvent(toggleEvent, eventEnvelope(props)) }
            .almadarActionTag(toggleEvent, props)
    }
    Row(modifier = surface.almadarLabel(props.string("aria-label")).height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(geometry.borderHeavy).fillMaxHeight().background(palette.primary))
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = almadarSpaceFor("lg"), vertical = almadarSpaceFor("md")),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon.isNotEmpty()) AlmadarGlyph(icon, color = "primary")
                if (statusIcon.isNotEmpty()) AlmadarGlyph(statusIcon, color = if (hasErrors) "error" else "success")
                Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    AlmadarText(props.string("title"), variant = "subheading", weight = "semibold")
                    if (subtitle.isNotEmpty()) AlmadarText(subtitle, variant = "caption", color = "muted")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                if (badge.isNotEmpty()) AlmadarBadge(badge, variant = badgeVariant)
                if (toggleEvent.isNotEmpty()) AlmadarGlyph(if (collapsed) "chevron-right" else "chevron-down", color = "muted", size = "sm")
            }
        }
    }
}
