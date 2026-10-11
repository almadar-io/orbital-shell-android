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
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role

@Composable
fun SwitchRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val incoming = props.bool("checked", props.bool("defaultChecked"))
    var checked by remember(incoming) { mutableStateOf(incoming) }
    val label = props.string("label")
    val disabled = props.bool("disabled")
    val changeEvent = props.string("onChange", "change")
    val name = props.string("name")
    val toggle = { next: Boolean ->
        checked = next
        onEvent(changeEvent, eventEnvelope(props, JsonObject(mapOf("checked" to JsonPrimitive(next)))))
    }
    Row(
        modifier = modifier
            .toggleable(value = checked, enabled = !disabled, role = Role.Switch, onValueChange = toggle)
            .almadarTag(if (name.isNotEmpty()) almadarFieldId(name, props) else almadarActionId(changeEvent, props))
            .almadarLabel(props.string("aria-label")),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
    ) {
        val unit = LocalAlmadarIconography.current.defaultSize
        val trackHeight = unit * 1.5f
        val thumb = unit * 1.25f
        val inset = (trackHeight - thumb) / 2f
        val travel = unit * 1.25f
        val thumbOffset by animateDpAsState(if (checked) travel else inset * 0f, label = "switch-thumb")
        val circle = RoundedCornerShape(percent = 50)
        val thumbShadow = LocalAlmadarElevation.current.interactiveElevation
        Box(
            modifier = Modifier
                .almadarAlpha(if (disabled) ALMADAR_DISABLED_ALPHA else 1f)
                .size(width = unit * 2.75f, height = trackHeight)
                .background(if (checked) palette.primary else palette.muted, circle)
                .padding(inset),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumb)
                    .shadow(thumbShadow.radius, circle, ambientColor = thumbShadow.color, spotColor = thumbShadow.color)
                    .background(palette.background, circle)
            )
        }
        if (label.isNotEmpty()) Text(text = label, style = almadarType("sm").copy(fontWeight = FontWeight.Medium), color = palette.foreground)
    }
}
