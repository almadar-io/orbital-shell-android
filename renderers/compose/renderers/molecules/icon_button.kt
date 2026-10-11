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
fun IconButtonRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val action = props.string("action")
    val href = props.string("href")
    val route = if (action.isNotEmpty()) action else if (href.isNotEmpty()) NAVIGATE_EVENT else ""
    val payload = props.jsonObject("actionPayload") ?: if (action.isEmpty() && href.isNotEmpty()) almadarJson("href" to href, "url" to href) else null
    ButtonRenderer(
        almadarJson(
            "leftIcon" to props.string("icon"), "aria-label" to props.string("label"),
            "variant" to props.string("variant", "ghost"), "size" to props.string("size", "sm"),
            "disabled" to props.bool("disabled"), "isLoading" to props.bool("isLoading"),
            "action" to route, "actionPayload" to payload, "sourceTrait" to almadarTrait(props)
        ),
        modifier,
        onEvent
    )
}
