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
fun UiSlotComponentRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val name = almadarScalar(root?.get("slot")).orEmpty()
    val nodes = props.children()
    val fallback = root?.get("fallback")?.takeIf { it !is JsonNull }
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().then(if (name.isEmpty()) Modifier else Modifier.verificationContainer(VerificationIds.slot(name)))) {
            if (nodes.isNotEmpty()) nodes.forEach { RenderChild(it, Modifier.fillMaxWidth(), onEvent) }
            else AlmadarNode(fallback, onEvent, Modifier.fillMaxWidth())
        }
    }
}
