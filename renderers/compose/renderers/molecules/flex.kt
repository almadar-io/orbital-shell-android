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
fun FlexRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val direction = props.string("direction", "row")
    val wrap = props.string("wrap", "nowrap")
    val align = props.string("align", "stretch")
    StackRenderer(
        almadarJson(
            "direction" to if (direction.startsWith("col")) "vertical" else "horizontal",
            "reverse" to direction.endsWith("reverse"),
            "wrap" to (wrap != "nowrap"),
            "align" to if (align == "baseline") "start" else align,
            "justify" to props.string("justify", "start"),
            "gap" to props.string("gap", "none"),
            "aria-label" to props.string("aria-label"),
            "sourceTrait" to almadarTrait(props),
            "children" to (props as? JsonObject)?.get("children")
        ),
        if (props.bool("inline")) modifier else modifier.fillMaxWidth(),
        onEvent
    )
}
