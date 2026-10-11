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
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun ChoiceButtonRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val text = props.string("text")
    val action = props.string("action")
    val selected = props.bool("selected")
    val disabled = props.bool("disabled")
    val iconName = props.string("icon")
    val hasIndex = (props as? JsonObject)?.get("index") != null
    val iconSize = LocalAlmadarIconography.current.defaultSize
    Box(modifier = modifier.fillMaxWidth(), propagateMinConstraints = true) {
        OutlinedButton(
            onClick = { if (action.isNotEmpty()) onEvent(action, eventEnvelope(props, props.jsonObject("payload"))) },
            enabled = !disabled,
            shape = almadarShape(AlmadarCorner.Interactive),
            border = null,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selected) palette.accent.copy(alpha = 0.15f) else palette.muted.copy(alpha = 0.4f),
                contentColor = palette.foreground
            ),
            modifier = Modifier
                .almadarTag(almadarActionId(action, props))
                .almadarLabel(props.string("aria-label"))
                .then(almadarBorder(AlmadarCorner.Interactive, LocalAlmadarGeometry.current.borderStandard, if (selected) palette.accent else palette.border))
                .semantics { this.selected = selected }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) {
                if (hasIndex) {
                    Text(
                        text = props.int("index").toString() + ".",
                        style = LocalAlmadarTheme.current.typeScale.numeric.style.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) palette.accent else palette.mutedForeground
                    )
                }
                if (iconName.isNotEmpty()) AlmadarIcon(name = iconName, modifier = Modifier.size(iconSize))
                Text(text = text, style = almadarType("sm"))
            }
        }
    }
}
