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
fun AgentActivityRowRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val activity = props.jsonObject("activity") ?: JsonObject(emptyMap())
    val facts = props.jsonObject("facts")
    Box(modifier = modifier) {
        AlmadarActivityItem(activity, facts?.bool("settled") ?: false, facts?.int("readyCount") ?: 0)
    }
}
