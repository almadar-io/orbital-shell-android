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
fun BehaviorViewRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val data = props.jsonObject("data")
    val details = data?.array("traits")
        ?.mapNotNull { trait -> (trait as? JsonObject)?.string("name")?.let { name -> data.jsonObject("traitDetails")?.jsonObject(name) } }
        ?: emptyList()
    if (details.isEmpty()) {
        EmptyStateRenderer(
            almadarJson("icon" to "workflow", "title" to "No trait data", "sourceTrait" to almadarTrait(props)),
            modifier, onEvent
        )
    } else {
        Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
            details.forEach { trait ->
                AlmadarPanel(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("lg"))) {
                        AvlStateMachineRenderer(
                            almadarJson("trait" to trait, "sourceTrait" to almadarTrait(props)),
                            Modifier.fillMaxWidth(), onEvent
                        )
                    }
                }
            }
        }
    }
}
