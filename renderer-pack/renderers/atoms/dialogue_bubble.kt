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
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

@Composable
fun DialogueBubbleRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val speaker = props.string("speaker")
    val full = props.string("text")
    val revealed = props.int("revealedChars", full.length).coerceIn(0, full.length)
    val top = props.string("position", "bottom") == "top"
    val ring = when (props.string("mood", "neutral")) {
        "happy" -> palette.success
        "concerned" -> palette.info
        "angry" -> palette.error
        else -> palette.warning
    }
    val shape = almadarShape(AlmadarCorner.Container, flat = if (top) AlmadarFlatCorner.BottomLeft else AlmadarFlatCorner.TopLeft)
    val portrait = LocalAlmadarIconography.current.defaultSize * 3f
    Surface(
        modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", if (speaker.isNotEmpty()) speaker + ": " + full else full)),
        shape = shape,
        color = palette.background.copy(alpha = 0.8f),
        contentColor = palette.foreground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
        ) {
            Box(
                modifier = Modifier
                    .size(portrait)
                    .border(LocalAlmadarGeometry.current.borderHeavy, ring, MaterialTheme.shapes.large)
                    .padding(LocalAlmadarGeometry.current.borderHeavy)
                    .clip(MaterialTheme.shapes.large)
                    .background(palette.muted),
                contentAlignment = Alignment.Center
            ) {
                Text(text = speaker.take(1).uppercase(), style = almadarType("lg").copy(fontWeight = FontWeight.Bold), color = palette.mutedForeground)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                if (speaker.isNotEmpty()) Text(text = speaker, style = almadarType("sm").copy(fontWeight = FontWeight.Bold), color = palette.warning)
                Text(text = full.substring(0, revealed), style = almadarType("sm"))
            }
        }
    }
}
