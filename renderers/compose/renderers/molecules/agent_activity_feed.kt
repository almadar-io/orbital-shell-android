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
fun AgentActivityFeedRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val activities = props.array("activities").mapNotNull { it as? JsonObject }
    val working = props.bool("working")
    val followLatest = props.bool("followLatest", true)
    val facts = almadarActivityFacts(activities)
    val listState = rememberLazyListState()
    LaunchedEffect(activities.size, working) {
        if (followLatest && activities.isNotEmpty()) listState.animateScrollToItem(activities.size)
    }
    if (activities.isEmpty() && !working) {
        EmptyStateRenderer(
            almadarJson(
                "icon" to "sparkles", "title" to props.string("emptyTitle", "No activity yet"),
                "description" to props.string("emptyDescription", "Send a message and the agent's work shows up here."),
                "sourceTrait" to almadarTrait(props)
            ),
            modifier, onEvent
        )
    } else {
        LazyColumn(state = listState, modifier = modifier.fillMaxWidth().almadarFlexFill(), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            items(activities.size) { index ->
                AgentActivityRowRenderer(
                    almadarJson("activity" to activities[index], "facts" to almadarJson("settled" to facts[index].first, "readyCount" to facts[index].second)),
                    Modifier.verificationContainer(VerificationIds.row(index.toString())),
                    onEvent
                )
            }
            if (working) item { AlmadarThinkingRow() }
        }
    }
}
