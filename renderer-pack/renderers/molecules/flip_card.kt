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
fun FlipCardRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val height = when (props.string("height", "h-64")) {
        "h-14" -> AlmadarSizes.w14
        "h-16" -> AlmadarSizes.w16
        "h-20" -> AlmadarSizes.w20
        "h-24" -> AlmadarSizes.w24
        "h-28" -> AlmadarSizes.w28
        "h-32" -> AlmadarSizes.w32
        "h-36" -> AlmadarSizes.w36
        "h-40" -> AlmadarSizes.w40
        "h-44" -> AlmadarSizes.w44
        "h-48" -> AlmadarSizes.w48
        "h-52" -> AlmadarSizes.w52
        "h-56" -> AlmadarSizes.w56
        "h-60" -> AlmadarSizes.w60
        "h-72" -> AlmadarSizes.w72
        "h-80" -> AlmadarSizes.w80
        "h-96" -> AlmadarSizes.w96
        else -> AlmadarSizes.w64
    }
    val face = { content: JsonElement? ->
        almadarJson(
            "type" to "box", "padding" to "lg", "rounded" to "lg", "shadow" to "lg", "bg" to "surface",
            "fullWidth" to true, "fullHeight" to true,
            "children" to JsonArray(listOf(almadarJson("type" to "center", "fullHeight" to true, "children" to JsonArray(listOfNotNull(content)))))
        )
    }
    Box(modifier = modifier.fillMaxWidth().height(height)) {
        FlipContainerRenderer(
            almadarJson(
                "flipped" to props.bool("flipped"), "onClick" to props.string("onFlip"),
                "aria-label" to props.string("aria-label"), "sourceTrait" to almadarTrait(props),
                "children" to JsonArray(listOf(face(root?.get("front")), face(root?.get("back"))))
            ),
            Modifier,
            onEvent
        )
    }
}
