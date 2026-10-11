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
import androidx.compose.ui.draw.clip

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size

@Composable
fun ConnectionBlockRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val shape = almadarShape(AlmadarCorner.Container)
    val heavy = LocalAlmadarGeometry.current.borderHeavy
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.success.copy(alpha = 0.1f))
            .drawBehind { drawRect(palette.success, size = Size(heavy.toPx(), size.height)) }
            .padding(almadarSpace("space5"))
            .almadarLabel(props.string("aria-label")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
        verticalAlignment = Alignment.Top
    ) {
        AlmadarGlyph("link-2", color = "success")
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            AlmadarText("Building On What You Know", variant = "h6")
            MarkdownContentRenderer(almadarJson("content" to props.string("content"), "sourceTrait" to almadarTrait(props)), Modifier.fillMaxWidth(), onEvent)
        }
    }
}
