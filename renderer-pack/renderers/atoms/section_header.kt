package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.ui.text.style.TextAlign

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

@Composable
fun SectionHeaderRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val subtitle = props.string("subtitle")
    val level = props.int("level", 2).coerceIn(1, 6)
    val align = props.string("align", "center")
    val horizontal = when (align) {
        "left" -> Alignment.Start
        "right" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val textAlign = when (align) {
        "left" -> TextAlign.Start
        "right" -> TextAlign.End
        else -> TextAlign.Center
    }
    val titleKey = when (level) { 1 -> "4xl"; 2 -> "3xl"; 3 -> "2xl"; 4 -> "xl"; 5 -> "lg"; else -> "base" }
    Column(
        modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")),
        horizontalAlignment = horizontal,
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
    ) {
        Text(
            text = almadarHeadingText(props.string("title")),
            style = almadarHeadingStyle(almadarType(titleKey)),
            color = palette.foreground,
            textAlign = textAlign,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle.isNotEmpty()) Text(text = subtitle, style = almadarType("base"), color = palette.mutedForeground, textAlign = textAlign)
    }
}
