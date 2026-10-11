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
fun FeatureGridRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val items = props.array("items").mapNotNull { it as? JsonObject }
    SimpleGridRenderer(
        almadarJson(
            "cols" to props.int("columns", 3), "gap" to props.string("gap", "md"),
            "aria-label" to props.string("aria-label"), "sourceTrait" to almadarTrait(props),
            "children" to JsonArray(items.map { it.merging(mapOf("type" to JsonPrimitive("feature-card"))) })
        ),
        modifier,
        onEvent
    )
}
