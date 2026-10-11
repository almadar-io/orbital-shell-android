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
fun FormFieldRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val error = props.string("error")
    val hint = props.string("hint")
    Column(modifier = modifier.almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        LabelRenderer(almadarJson("text" to props.string("label"), "required" to props.bool("required")), Modifier, AlmadarNoEvent)
        props.children().forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
        if (error.isNotEmpty()) AlmadarText(error, variant = "caption", color = "error")
        else if (hint.isNotEmpty()) AlmadarText(hint, variant = "caption", color = "muted")
    }
}
