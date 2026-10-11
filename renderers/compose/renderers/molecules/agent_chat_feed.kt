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

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun AgentChatFeedRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val activities = props.array("activities").mapNotNull { it as? JsonObject }
    val working = props.bool("working")
    val idPrefix = props.string("idPrefix", "agent-chat")
    val rawKey = props.string("rawLlmPreferenceKey")
    val clipboard = LocalClipboardManager.current
    var showRaw by remember(rawKey) { mutableStateOf(false) }
    fun isUser(a: JsonObject) = a.string("type") == "message" && a.string("role") == "user"
    fun isReply(a: JsonObject) = (a.string("type") == "message" && a.string("role") != "user") || a.string("type") == "error"
    class Turn(val request: JsonObject?, val steps: MutableList<JsonObject>, val replies: MutableList<JsonObject>, val startedAt: Long)
    val turns = mutableListOf<Turn>()
    activities.forEach { activity ->
        val current = turns.lastOrNull()
        val stamp = activity.string("timestamp").toLongOrNull() ?: 0L
        if (isUser(activity) || current == null) {
            turns.add(Turn(if (isUser(activity)) activity else null, if (isUser(activity) || isReply(activity)) mutableListOf() else mutableListOf(activity), if (isReply(activity)) mutableListOf(activity) else mutableListOf(), stamp))
        } else if (isReply(activity)) {
            current.replies.add(activity)
        } else {
            current.steps.add(activity)
        }
    }
    val zone = java.time.ZoneId.systemDefault()
    fun dayOf(stamp: Long) = java.time.Instant.ofEpochMilli(stamp).atZone(zone).toLocalDate()
    fun clock(stamp: Long) = java.time.Instant.ofEpochMilli(stamp).atZone(zone).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    fun dayLabel(stamp: Long): String {
        val day = dayOf(stamp)
        val today = java.time.LocalDate.now(zone)
        return when (day) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> day.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }
    val listState = rememberLazyListState()
    val itemCount = turns.size + (if (working) 1 else 0)
    LaunchedEffect(activities.size, working) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }
    if (activities.isEmpty() && !working) {
        EmptyStateRenderer(
            almadarJson(
                "icon" to "sparkles", "title" to props.string("emptyTitle"),
                "description" to "Ask in your own words — the assistant can look things up and make changes for you.",
                "sourceTrait" to almadarTrait(props)
            ),
            modifier, onEvent
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth().padding(almadarSpaceFor("sm")).verificationContainer(idPrefix + "-scroller").almadarFlexFill(),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
        ) {
            items(turns.size) { index ->
                val turn = turns[index]
                val previous = turns.getOrNull(index - 1)
                val live = index == turns.lastIndex && working && turn.replies.isEmpty()
                val visibleSteps = if (showRaw) turn.steps else turn.steps.filter { it.string("type") != "llm_response" }
                val toolSteps = turn.steps.count { it.string("type") != "llm_response" }
                val hasRaw = turn.steps.any { it.string("type") == "llm_response" }
                var open by remember(turn.startedAt, index) { mutableStateOf(false) }
                val stepFacts = almadarActivityFacts(visibleSteps)
                Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    if (previous == null || dayOf(previous.startedAt) != dayOf(turn.startedAt)) AlmadarDivider(label = dayLabel(turn.startedAt))
                    if (turn.request != null) {
                        AgentActivityRowRenderer(almadarJson("activity" to turn.request), Modifier, onEvent)
                        AlmadarText(clock(turn.startedAt), variant = "caption", color = "muted", align = "right")
                    }
                    if (live) {
                        visibleSteps.forEachIndexed { stepIndex, step ->
                            AgentActivityRowRenderer(
                                almadarJson("activity" to step, "facts" to almadarJson("settled" to stepFacts[stepIndex].first, "readyCount" to stepFacts[stepIndex].second)),
                                Modifier, onEvent
                            )
                        }
                    } else if (toolSteps > 0 || hasRaw) {
                        AlmadarLocalButton(
                            props, "Worked through " + toolSteps + " steps", { open = !open },
                            variant = "ghost", size = "sm", icon = if (open) "chevron-down" else "chevron-right"
                        )
                        if (open) {
                            Column(modifier = Modifier.padding(start = almadarSpaceFor("md")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                if (hasRaw) {
                                    SwitchRenderer(
                                        almadarJson("checked" to showRaw, "label" to "Raw LLM", "name" to idPrefix + "-show-raw-llm-" + index, "onChange" to idPrefix + "-raw-llm", "local" to true),
                                        Modifier
                                    ) { _, envelope -> showRaw = envelope?.jsonObject("actionPayload")?.bool("checked", !showRaw) ?: !showRaw }
                                }
                                visibleSteps.forEachIndexed { stepIndex, step ->
                                    AgentActivityRowRenderer(
                                        almadarJson("activity" to step, "facts" to almadarJson("settled" to stepFacts[stepIndex].first, "readyCount" to stepFacts[stepIndex].second)),
                                        Modifier, onEvent
                                    )
                                }
                            }
                        }
                    }
                    turn.replies.forEach { reply ->
                        AgentActivityRowRenderer(almadarJson("activity" to reply), Modifier, onEvent)
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                            AlmadarText(clock(reply.string("timestamp").toLongOrNull() ?: turn.startedAt), variant = "caption", color = "muted")
                            if (reply.string("type") == "message") {
                                AlmadarLocalButton(
                                    props, "", { clipboard.setText(AnnotatedString(reply.string("content"))) },
                                    variant = "ghost", icon = "copy", size = "sm", ariaLabel = "Copy reply"
                                )
                            }
                        }
                    }
                }
            }
            if (working) item { AlmadarThinkingRow() }
        }
    }
}
