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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ReplyTreeRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val roots = almadarRows((props as? JsonObject)?.get("nodes"))
    val maxDepth = props.int("maxDepth", 6)
    val showActions = props.bool("showActions", true)
    val voteEvent = props.string("voteEvent")
    val replyEvent = props.string("replyEvent")
    val flagEvent = props.string("flagEvent")
    val continueEvent = props.string("continueThreadEvent")
    val initialCollapsed = mutableSetOf<String>()
    val collect: (List<JsonObject>) -> Unit = { start ->
        val pending = ArrayDeque(start)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (node.bool("collapsed")) initialCollapsed.add(node.string("id"))
            pending.addAll(almadarRows(node["replies"]))
        }
    }
    collect(roots)
    var collapsed by remember { mutableStateOf(initialCollapsed.toSet()) }
    var composing by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    @Composable
    fun Node(node: JsonObject, depth: Int) {
        val id = node.string("id")
        val author = node.string("authorName")
        val replies = almadarRows(node["replies"])
        val isCollapsed = collapsed.contains(id)
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).verificationContainer(VerificationIds.row(id)), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            Column(modifier = Modifier.fillMaxHeight().width(almadarSpaceFor("xl")), horizontalAlignment = Alignment.CenterHorizontally) {
                if (replies.isNotEmpty()) {
                    AlmadarLocalButton(
                        props, "", { collapsed = if (isCollapsed) collapsed - id else collapsed + id },
                        variant = "ghost", size = "sm", icon = if (isCollapsed) "chevron-right" else "chevron-down",
                        ariaLabel = if (isCollapsed) "Expand replies" else "Collapse replies"
                    )
                    if (!isCollapsed) Box(modifier = Modifier.weight(1f).width(LocalAlmadarGeometry.current.borderHairline).background(palette.border))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(bottom = almadarSpaceFor("md")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                    AvatarRenderer(almadarJson("name" to author, "src" to node.string("authorAvatarUrl"), "size" to "sm"), Modifier, AlmadarNoEvent)
                    AlmadarText(author, variant = "body1", weight = "semibold")
                    AlmadarText(node.string("postedAt"), variant = "caption", color = "secondary")
                }
                AlmadarText(node.string("content"), variant = "body1")
                if (showActions) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        VoteStackRenderer(
                            almadarJson(
                                "count" to node.int("voteCount"), "userVote" to node.string("userVote"), "voteEvent" to voteEvent,
                                "size" to "sm", "variant" to "horizontal", "label" to "Vote on reply by " + author, "sourceTrait" to almadarTrait(props)
                            ),
                            Modifier,
                            { name, envelope ->
                                val next = envelope?.jsonObject("actionPayload")?.get("next") ?: JsonNull
                                onEvent(name, eventEnvelope(props, almadarJson("nodeId" to id, "vote" to next)))
                            }
                        )
                        AlmadarLocalButton(
                            props, "Reply", { composing = if (composing == id) null else id; draft = "" },
                            variant = "ghost", size = "sm", icon = "message-square", ariaLabel = "Reply to " + author
                        )
                        AlmadarButton(
                            props, "Flag", flagEvent, onEvent,
                            payload = almadarJson("nodeId" to id), variant = "ghost", size = "sm", icon = "flag", ariaLabel = "Flag reply by " + author
                        )
                    }
                }
                if (composing == id) {
                    Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                        AlmadarInput(
                            name = "reply-" + id, value = draft, onChange = { draft = it },
                            placeholder = "Reply to " + author + "…", inputType = "textarea", ariaLabel = "Reply to " + author
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                            AlmadarButton(
                                props, "Send", replyEvent,
                                { name, envelope ->
                                    onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload")))
                                    draft = ""
                                    composing = null
                                },
                                payload = almadarJson("parentNodeId" to id, "content" to draft.trim()),
                                variant = "primary", size = "sm", icon = "send", disabled = draft.isBlank()
                            )
                            AlmadarLocalButton(props, "Cancel", { draft = ""; composing = null }, variant = "ghost", size = "sm")
                        }
                    }
                }
                if (replies.isNotEmpty() && !isCollapsed) {
                    if (depth >= maxDepth) {
                        AlmadarButton(props, "Continue thread", continueEvent, onEvent, payload = almadarJson("nodeId" to id), variant = "ghost", size = "sm", icon = "chevron-right")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                            replies.forEach { Node(it, depth + 1) }
                        }
                    }
                }
            }
        }
    }
    if (roots.isEmpty()) {
        AlmadarText("No replies yet.", modifier = modifier.almadarLabel(props.string("aria-label")), variant = "small", color = "muted")
    } else {
        Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            roots.forEach { Node(it, 0) }
        }
    }
}
