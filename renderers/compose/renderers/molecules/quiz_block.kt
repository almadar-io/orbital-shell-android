package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: molecules — a grouped container styled by tokens only

@Composable
fun QuizBlockRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    val density = LocalAlmadarDensity.current
    AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        Column(modifier = Modifier.padding(density.cardPaddingMd), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.Top) {
                AlmadarGlyph("help-circle", color = "primary", size = "sm")
                AlmadarText(props.string("question"), modifier = Modifier.weight(1f), weight = "medium")
            }
            if (revealed) {
                AlmadarDivider()
                AlmadarText(props.string("answer"))
            }
            AlmadarLocalButton(
                props, if (revealed) "Hide answer" else "Show answer", { revealed = !revealed },
                variant = "ghost", size = "sm", icon = if (revealed) "chevron-up" else "chevron-down"
            )
        }
    }
}
