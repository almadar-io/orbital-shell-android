package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

@Composable
fun TabbedContainerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    TabsRenderer(
        almadarJson(
            "tabs" to (props as? JsonObject)?.get("tabs"),
            "defaultActiveTab" to props.string("defaultTab"),
            "activeTab" to props.string("activeTab"),
            "orientation" to if (props.string("position", "top") == "left") "vertical" else "horizontal",
            "aria-label" to props.string("aria-label"),
            "sourceTrait" to almadarTrait(props)
        ),
        modifier,
        onEvent
    )
}
