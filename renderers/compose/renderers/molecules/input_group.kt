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
fun InputGroupRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val options = props.array("options")
    val inputProps = almadarJson(
        "placeholder" to props.string("placeholder"), "value" to props.string("value"), "disabled" to props.bool("disabled"),
        "action" to props.string("action"), "inputType" to props.string("inputType"), "label" to props.string("label"),
        "helperText" to props.string("helperText"), "error" to props.string("error"), "leftIcon" to props.string("leftIcon"),
        "rightIcon" to props.string("rightIcon"), "clearable" to props.bool("clearable"), "onClear" to props.string("onClear"),
        "rows" to root?.get("rows"), "onChange" to props.string("onChange"), "name" to props.string("name"),
        "aria-label" to props.string("aria-label"), "sourceTrait" to almadarTrait(props),
        "options" to JsonArray(options.map { if (it is JsonPrimitive) almadarJson("value" to it.contentOrNull, "label" to it.contentOrNull) else it })
    )
    val addon: @Composable (JsonElement?) -> Unit = { node ->
        if (node != null && node !is JsonNull) {
            Box(
                modifier = Modifier
                    .background(palette.muted, MaterialTheme.shapes.small)
                    .border(LocalAlmadarGeometry.current.borderHairline, palette.border, MaterialTheme.shapes.small)
                    .padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm")),
                contentAlignment = Alignment.Center
            ) { AlmadarNode(node, onEvent, variant = "small") }
        }
    }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.Top) {
        addon(root?.get("leftAddon"))
        if (options.isNotEmpty()) SelectRenderer(inputProps, Modifier.weight(1f), onEvent)
        else InputRenderer(inputProps, Modifier.weight(1f), onEvent)
        addon(root?.get("rightAddon"))
    }
}
