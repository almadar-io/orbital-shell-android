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

// tier: molecules — a grouped container styled by tokens only

@Composable
fun FormLayoutRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val dividers = props.bool("dividers", true)
    val palette = LocalAlmadarTheme.current.color
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("lg"))) {
        props.children().forEachIndexed { index, child ->
            if (index > 0 && dividers) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(LocalAlmadarGeometry.current.borderStandard).background(palette.border))
                    RenderChild(child, Modifier.padding(top = almadarSpace("space8")), onEvent)
                }
            } else {
                RenderChild(child, Modifier, onEvent)
            }
        }
    }
}
