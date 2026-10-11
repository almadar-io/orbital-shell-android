package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role

private val traceActivityLabels = mapOf(
    "tool_call" to "tool call", "tool_result" to "tool result", "message" to "message", "error" to "error",
    "file_operation" to "file operation", "schema_diff" to "schema diff", "milestone" to "milestone", "analysis" to "analysis",
    "cancelled" to "cancelled", "clarification_question" to "clarification question", "coordinator_decision" to "coordinator decision",
    "done" to "done", "llm_response" to "model response", "orbital_done" to "orbital done", "orbital_started" to "orbital started",
    "pending_question" to "pending question", "plan_committed" to "plan committed", "schema_change" to "schema change",
)

private val traceFileOperations = mapOf("ls" to "list", "read_file" to "read", "write_file" to "write", "edit_file" to "edit")

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
private fun tracePrettyFormat(): Json = Json { prettyPrint = true; prettyPrintIndent = "  " }

private val tracePrettyJson = tracePrettyFormat()

private class TraceEntry(val source: String, val data: JsonObject)

private fun traceNumber(element: JsonElement, key: String): Double = ((element as? JsonObject)?.get(key) as? JsonPrimitive)?.doubleOrNull ?: 0.0

private fun traceTime(timestamp: Double): String =
    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp.toLong()))

private fun tracePretty(raw: String): String? =
    runCatching { tracePrettyJson.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(raw)) }.getOrNull()

private fun tracePreview(text: String, limit: Int = 120): String = if (text.length > limit) text.take(limit - 1) + "…" else text

private fun traceCompact(element: JsonElement): String {
    val text = Json.encodeToString(JsonElement.serializer(), element)
    return if (text.length <= 240) text else text.take(239) + "…"
}

private fun traceSummarize(args: JsonElement?): String {
    val keys = (args as? JsonObject)?.keys?.toList() ?: emptyList()
    if (keys.isEmpty()) return ""
    return keys.take(3).joinToString(", ") + (if (keys.size > 3) ", …" else "")
}

private fun traceStatusIcon(status: String): String = when (status) {
    "running" -> "loader"
    "complete" -> "check"
    "error" -> "x"
    else -> "circle"
}

private fun traceRoleVariant(role: String): String = when (role) {
    "user" -> "info"
    "tool" -> "warning"
    "assistant" -> "default"
    else -> "neutral"
}

private fun traceLastMessage(subagent: JsonElement): String? = subagent.array("messages").lastOrNull()?.string("message")

private fun traceSubagentActivities(subagent: JsonElement): List<JsonObject> = subagent.array("messages").map { entry ->
    val timestamp = JsonPrimitive(traceNumber(entry, "timestamp"))
    if (entry.string("tool").isNotEmpty()) {
        almadarJson("type" to "tool_call", "tool" to entry.string("tool"), "argsText" to entry.string("message"), "timestamp" to timestamp)
    } else {
        almadarJson("type" to "message", "role" to "system", "content" to entry.string("message"), "timestamp" to timestamp)
    }
}

private fun traceCoordinatorItems(activities: List<JsonObject>): List<JsonObject> = activities.flatMap { activity ->
    val timestamp = JsonPrimitive(traceNumber(activity, "timestamp"))
    when (activity.string("type")) {
        "tool_call" -> listOf(almadarJson("type" to "tool_call", "tool" to activity.string("tool"), "argsText" to activity.string("argsText"), "timestamp" to timestamp))
        "tool_result" -> listOf(almadarJson("type" to "tool_result", "tool" to activity.string("tool"), "resultText" to activity.string("resultText"), "success" to activity.bool("success", true), "timestamp" to timestamp))
        "message" -> listOf(almadarJson("type" to "message", "role" to activity.string("role"), "content" to activity.string("content"), "timestamp" to timestamp))
        "error" -> listOf(almadarJson("type" to "message", "role" to "system", "content" to "Error: ${activity.string("message")}", "timestamp" to timestamp))
        "llm_response" -> listOf(almadarJson("type" to "message", "role" to "assistant", "content" to activity.string("content"), "timestamp" to timestamp))
        "file_operation" -> listOf(almadarJson("type" to "file_operation", "operation" to activity.string("operation"), "path" to activity.string("path"), "success" to activity.bool("success", true), "timestamp" to timestamp))
        "schema_diff" -> listOf(almadarJson("type" to "schema_diff", "filePath" to activity.string("filePath"), "hunks" to (activity["hunks"] ?: JsonArray(emptyList())), "timestamp" to timestamp))
        else -> emptyList()
    }
}

private fun traceIcon(entry: TraceEntry): Pair<String, String> {
    val data = entry.data
    return when (entry.source) {
        "message" -> when {
            data.string("role") == "tool" -> "terminal" to "primary"
            data.string("reasoningContent").isNotEmpty() -> "sparkles" to "primary"
            data.string("role") == "user" -> "user" to "primary"
            else -> "message-square" to "muted"
        }
        "activity" -> when (data.string("type")) {
            "tool_call" -> "terminal" to "primary"
            "tool_result" -> if (data.bool("success", true)) "check" to "success" else "x" to "error"
            "error" -> "alert-triangle" to "error"
            "file_operation" -> "file" to "muted"
            "schema_diff" -> "git-commit" to "muted"
            else -> "info" to "muted"
        }
        else -> "bot" to "primary"
    }
}

private fun traceLabel(entry: TraceEntry): String {
    val data = entry.data
    return when (entry.source) {
        "message" -> when {
            data.string("role") == "tool" -> "tool"
            data.string("reasoningContent").isNotEmpty() -> "thinking"
            else -> data.string("role")
        }
        "activity" -> traceActivityLabels[data.string("type")] ?: data.string("type")
        else -> "subagent"
    }
}

private fun tracePreviewOf(entry: TraceEntry): String {
    val data = entry.data
    return when (entry.source) {
        "message" -> when {
            data.string("role") == "tool" -> "${data.string("toolName").ifEmpty { "tool" }} → ${data.string("toolCallId").take(8)}"
            data.string("reasoningContent").isNotEmpty() -> tracePreview(data.string("reasoningContent"))
            else -> tracePreview(data.string("content"))
        }
        "activity" -> when (data.string("type")) {
            "tool_call" -> "${data.string("tool")}(${tracePreview(data.string("argsText"))})"
            "tool_result" -> "${data.string("tool")}: ${tracePreview(data.string("resultText"))}"
            "message" -> tracePreview(data.string("content"))
            "error" -> tracePreview(data.string("message"))
            "file_operation" -> "${traceFileOperations[data.string("operation")] ?: data.string("operation")} ${data.string("path")}"
            "schema_diff" -> "diff ${data.string("filePath")}"
            else -> data.string("type")
        }
        else -> "${data.string("name")} · ${data.string("task")}"
    }
}

private fun traceTimeOf(entry: TraceEntry): String? {
    val data = entry.data
    if (entry.source == "activity") return traceTime(traceNumber(data, "timestamp"))
    if (entry.source == "subagent") {
        val first = data.array("messages").firstOrNull() as? JsonObject
        if (first != null && first["timestamp"] != null) return traceTime(traceNumber(first, "timestamp"))
    }
    return null
}

private fun traceCode(code: String, language: String, height: String): JsonObject =
    almadarJson("type" to "code-block", "code" to code, "language" to language, "maxHeight" to height)

private fun traceToolCallItems(calls: List<JsonElement>, prefix: String): JsonArray = JsonArray(calls.mapIndexed { offset, call ->
    almadarJson(
        "id" to "$prefix-$offset",
        "title" to "${call.string("name")}(${traceSummarize((call as? JsonObject)?.get("args"))})",
        "content" to traceCode(tracePrettyJson.encodeToString(JsonElement.serializer(), (call as? JsonObject)?.get("args") ?: JsonObject(emptyMap())), "json", "24rem")
    )
})

@Composable
private fun TraceScroll(cap: Dp, count: Int, autoScroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberScrollState()
    LaunchedEffect(count, state.maxValue) { if (autoScroll) state.scrollTo(state.maxValue) }
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = cap).verticalScroll(state), content = content)
}

@Composable
private fun TraceDot(color: Color, pulse: Boolean) {
    val context = LocalContext.current
    val reducedMotion = remember { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val transition = rememberInfiniteTransition(label = "trace-dot")
    val level by transition.animateFloat(
        initialValue = if (pulse && !reducedMotion) 0.4f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = (LocalAlmadarMotion.current.durationsMs["dramatic"] ?: 0) * 2), repeatMode = RepeatMode.Reverse),
        label = "trace-dot-level"
    )
    Box(Modifier.size(almadarSpace("space2")).alpha(level).background(color, CircleShape))
}

@Composable
private fun TraceRow(activity: JsonObject) {
    val palette = LocalAlmadarTheme.current.color
    val type = activity.string("type")
    val success = activity.bool("success", true)
    val role = activity.string("role")
    val icon = when (type) {
        "tool_call" -> "terminal" to "primary"
        "tool_result" -> if (success) "check" to "success" else "x" to "error"
        "message" -> (if (role == "user") "user" else if (role == "system") "info" else "sparkles") to (if (role == "user") "primary" else if (role == "system") "muted" else "inherit")
        "error" -> "alert-triangle" to "error"
        else -> "file" to "muted"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space1")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2"))
    ) {
        AlmadarGlyph(icon.first, color = icon.second, size = "xs")
        Box(Modifier.weight(1f)) {
            when (type) {
                "tool_call" -> {
                    val args = activity.string("argsText")
                    AlmadarMonoRuns(
                        listOfNotNull(
                            AlmadarMonoRun(activity.string("tool"), null, true),
                            if (args != "" && args != "{}") AlmadarMonoRun(" $args", palette.mutedForeground) else null
                        )
                    )
                }
                "tool_result" -> AlmadarMonoRuns(listOf(AlmadarMonoRun(activity.string("tool") + ": ", palette.mutedForeground), AlmadarMonoRun(activity.string("resultText"))))
                "message" -> AlmadarText(activity.string("content"), variant = "caption")
                "error" -> AlmadarText(activity.string("message"), variant = "caption", color = "error")
                else -> AlmadarMonoRuns(listOf(AlmadarMonoRun("[$type] ${traceCompact(activity)}", palette.mutedForeground)))
            }
        }
        AlmadarText(traceTime(traceNumber(activity, "timestamp")), variant = "caption", color = "muted")
    }
}

@Composable
private fun TraceStream(activities: List<JsonObject>) {
    Column(Modifier.fillMaxWidth()) { activities.forEach { TraceRow(it) } }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TraceBadges(labels: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1")), verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
        labels.forEach { AlmadarBadge(it, variant = "neutral") }
    }
}

@Composable
private fun TraceCoordinatorCard(activities: List<JsonObject>) {
    val palette = LocalAlmadarTheme.current.color
    val decision = activities.lastOrNull { it.string("type") == "coordinator_decision" }
    val plan = activities.lastOrNull { it.string("type") == "plan_committed" }
    val orbitals = plan?.array("orbitals")?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
    val questions = activities.filter { it.string("type") == "pending_question" }
    if (decision == null && plan == null && questions.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth().background(palette.muted.copy(alpha = 0.3f)).padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space2")),
        verticalArrangement = Arrangement.spacedBy(almadarSpace("space2"))
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1")), verticalAlignment = Alignment.CenterVertically) {
            AlmadarGlyph("compass", size = "xs")
            AlmadarText("Coordinator", variant = "caption", weight = "semibold")
        }
        if (decision != null) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1")), verticalAlignment = Alignment.CenterVertically) {
                    AlmadarText("ORGANISM", variant = "caption", color = "muted")
                    AlmadarBadge(decision.string("organism"), variant = "default")
                    val prior = decision.string("priorOrganism")
                    if (prior.isNotEmpty() && prior != decision.string("organism")) AlmadarText("was $prior", variant = "caption", color = "muted")
                }
                if (decision.string("reason").isNotEmpty()) AlmadarText(decision.string("reason"), variant = "caption", color = "muted")
            }
        }
        if (orbitals.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                AlmadarText("PLAN · ${orbitals.size} ${if (orbitals.size == 1) "ORBITAL" else "ORBITALS"}", variant = "caption", color = "muted")
                TraceBadges(orbitals)
            }
        }
        if (questions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                AlmadarText("${questions.size} PENDING ${if (questions.size == 1) "QUESTION" else "QUESTIONS"}", variant = "caption", color = "muted")
                questions.forEach { question ->
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                        AlmadarGlyph("help-circle", color = "warning", size = "xs")
                        AlmadarText(question.string("question") + (question.string("orbitalName").let { if (it.isEmpty()) "" else " ($it)" }), modifier = Modifier.weight(1f), variant = "caption")
                    }
                }
            }
        }
    }
    AlmadarDivider()
}

@Composable
private fun TraceSubagentRow(subagent: JsonObject) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space2")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2"))
    ) {
        AlmadarGlyph(traceStatusIcon(subagent.string("status")), size = "xs")
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarText(subagent.string("name"), variant = "caption", weight = "medium", truncate = true)
                AlmadarBadge(subagent.string("role"), variant = "default")
            }
            AlmadarText(traceLastMessage(subagent) ?: subagent.string("task"), variant = "caption", color = "muted", truncate = true)
        }
    }
    AlmadarDivider()
}

@Composable
private fun TraceOrbitalGroup(name: String, subagents: List<JsonObject>) {
    val palette = LocalAlmadarTheme.current.color
    val running = subagents.count { it.string("status") == "running" }
    val complete = subagents.count { it.string("status") == "complete" }
    Row(
        modifier = Modifier.fillMaxWidth().background(palette.surface).padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space2")),
        horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2")),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlmadarGlyph("circle", size = "xs")
        AlmadarText(name, modifier = Modifier.weight(1f), variant = "caption", weight = "semibold", truncate = true)
        AlmadarBadge(if (running > 0) "$running running" else "$complete done", variant = "default")
    }
    AlmadarDivider()
    subagents.forEach { TraceSubagentRow(it) }
}

@Composable
private fun TraceRichCard(subagent: JsonObject) {
    val palette = LocalAlmadarTheme.current.color
    val activities = traceSubagentActivities(subagent)
    val dot = when (subagent.string("status")) {
        "complete" -> palette.success
        "error" -> palette.error
        else -> palette.primary
    }
    val ms = traceNumber(subagent, "durationMs")
    Column(Modifier.fillMaxWidth().padding(almadarSpace("space3")), verticalArrangement = Arrangement.spacedBy(almadarSpace("space2"))) {
        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2")), verticalAlignment = Alignment.CenterVertically) {
            TraceDot(dot, subagent.string("status") == "running")
            AlmadarText(subagent.string("name"), modifier = Modifier.weight(1f), variant = "caption", weight = "semibold", truncate = true)
            AlmadarBadge(subagent.string("role"), variant = when (subagent.string("role")) { "deterministic" -> "default"; "llm" -> "info"; else -> "warning" })
            if (ms > 0) AlmadarText(if (ms < 1000) "${ms.toInt()}ms" else String.format(java.util.Locale.ROOT, "%.1fs", ms / 1000), variant = "caption", color = "muted")
        }
        AlmadarText(subagent.string("task"), variant = "caption", color = "muted")
        if (activities.isNotEmpty()) {
            AlmadarDivider()
            TraceScroll(AlmadarSizes.w64, activities.size) { TraceStream(activities) }
        } else {
            AlmadarHintText(if (subagent.string("status") == "complete") "No conversation log" else "Waiting for process data…")
        }
    }
    AlmadarDivider()
}

@Composable
private fun TraceChatRow(message: JsonObject, index: Int, onEvent: (String, JsonElement?) -> Unit) {
    val role = message.string("role")
    val content = message.string("content")
    val reasoning = message.string("reasoningContent")
    val calls = message.array("toolCalls")
    if (role == "tool") {
        val pretty = tracePretty(content)
        val id = message.string("toolCallId")
        val title = "${message.string("toolName").ifEmpty { "tool" }} →${if (id.isEmpty()) "" else " " + id.take(8)}"
        val body = if (pretty != null) traceCode(pretty, "json", "24rem") else almadarJson("type" to "typography", "content" to content, "variant" to "caption")
        Box(Modifier.fillMaxWidth().padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space2"))) {
            AccordionRenderer(almadarJson("items" to JsonArray(listOf(almadarJson("id" to "tool-$index", "title" to title, "content" to body)))), Modifier, onEvent)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space2")),
            verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarBadge(role.uppercase(), variant = traceRoleVariant(role))
                if (reasoning.isNotEmpty()) AlmadarText("· thinking", variant = "caption", color = "muted")
                if (calls.isNotEmpty()) AlmadarText("· ${calls.size} tool call${if (calls.size == 1) "" else "s"}", variant = "caption", color = "muted")
            }
            if (reasoning.isNotEmpty()) {
                AccordionRenderer(almadarJson("items" to JsonArray(listOf(almadarJson("id" to "reasoning-$index", "title" to "Reasoning", "content" to almadarJson("type" to "markdown-content", "content" to reasoning))))), Modifier, onEvent)
            }
            if (content.isNotEmpty()) MarkdownContentRenderer(almadarJson("content" to content), Modifier, onEvent)
            if (calls.isNotEmpty()) {
                AccordionRenderer(almadarJson("multiple" to true, "items" to traceToolCallItems(calls, "tc-$index")), Modifier, onEvent)
            }
        }
    }
    AlmadarDivider()
}

@Composable
private fun TraceDetail(entry: TraceEntry, onEvent: (String, JsonElement?) -> Unit) {
    val data = entry.data
    val time = traceTimeOf(entry)
    @Composable
    fun head(badge: String, variant: String, stamp: String?) {
        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1")), verticalAlignment = Alignment.CenterVertically) {
            AlmadarBadge(badge, variant = variant)
            if (stamp != null) AlmadarText(stamp, variant = "caption", color = "muted")
        }
    }
    @Composable
    fun code(text: String, height: String) {
        CodeBlockRenderer(traceCode(text, "json", height), Modifier, onEvent)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(almadarSpace("space4"))) {
        when (entry.source) {
            "message" -> {
                val role = data.string("role")
                val content = data.string("content")
                val calls = data.array("toolCalls")
                head(role.uppercase(), traceRoleVariant(role), null)
                if (data.string("reasoningContent").isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                        AlmadarText("Reasoning", variant = "caption", color = "muted")
                        MarkdownContentRenderer(almadarJson("content" to data.string("reasoningContent")), Modifier, onEvent)
                    }
                }
                if (content.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                        AlmadarText("Content", variant = "caption", color = "muted")
                        if (role == "tool") {
                            val pretty = tracePretty(content)
                            if (pretty != null) code(pretty, "24rem") else AlmadarMonoText(content)
                        } else {
                            MarkdownContentRenderer(almadarJson("content" to content), Modifier, onEvent)
                        }
                    }
                }
                if (calls.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                        AlmadarText("Tool calls", variant = "caption", color = "muted")
                        AccordionRenderer(almadarJson("multiple" to true, "items" to traceToolCallItems(calls, "tc-detail")), Modifier, onEvent)
                    }
                }
            }
            "activity" -> when (data.string("type")) {
                "tool_call" -> {
                    head("tool call", "default", time)
                    AlmadarText(data.string("tool"), variant = "body2")
                    code(data.string("argsText"), "60vh")
                }
                "tool_result" -> {
                    val ok = data.bool("success", true)
                    head(if (ok) "success" else "fail", if (ok) "default" else "warning", time)
                    AlmadarText(data.string("tool"), variant = "body2")
                    code(data.string("resultText"), "60vh")
                }
                "message" -> {
                    head(data.string("role"), "neutral", time)
                    AlmadarText(data.string("content"), variant = "body2")
                }
                "error" -> {
                    head("error", "warning", time)
                    AlmadarText(data.string("message"), variant = "body2", color = "error")
                }
                "file_operation" -> {
                    head("file", "neutral", time)
                    AlmadarText("${traceFileOperations[data.string("operation")] ?: data.string("operation")} ${data.string("path")}", variant = "body2")
                }
                "schema_diff" -> {
                    head("diff", "neutral", time)
                    AlmadarText(data.string("filePath"), variant = "body2")
                }
                else -> {
                    AlmadarText(time ?: "", variant = "caption", color = "muted")
                    AlmadarText(data.string("type"), variant = "body2")
                }
            }
            else -> {
                val activities = traceSubagentActivities(data)
                head(data.string("role"), "default", time)
                AlmadarText(data.string("name"), variant = "body2", weight = "semibold")
                AlmadarText(data.string("task"), variant = "body2", color = "muted")
                if (activities.isNotEmpty()) {
                    AlmadarDivider()
                    TraceStream(activities)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SubagentTracePanelRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val closeEvent = props.string("onClose")
    val tabMode = props.string("mode", "overlay") == "tab"
    val level = props.int("disclosureLevel", 1)
    val rich = tabMode || level >= 3
    val focused = props.string("focusedOrbital")
    val subagents = props.array("subagents").mapNotNull { it as? JsonObject }
    val activities = props.array("coordinatorActivities").mapNotNull { it as? JsonObject }
    val messages = props.array("coordinatorMessages").mapNotNull { it as? JsonObject }
    var selected by remember { mutableStateOf<TraceEntry?>(null) }
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val densityLabel = if (rich) "Detailed" else "Compact"

    @Composable
    fun Header(title: @Composable () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space2")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlmadarGlyph("activity", size = "sm")
            Box(Modifier.weight(1f)) { title() }
            if (closeEvent.isNotEmpty()) AlmadarButton(props, "", closeEvent, onEvent, variant = "ghost", icon = "x", size = "sm", ariaLabel = "Close")
        }
        AlmadarDivider()
    }

    @Composable
    fun Centered(text: String, variant: String) {
        Box(Modifier.fillMaxWidth().padding(almadarSpace("space4")), contentAlignment = Alignment.Center) {
            AlmadarText(text, variant = variant, color = "muted", align = "center")
        }
    }

    @Composable
    fun ConversationHeader(count: String) {
        Row(
            modifier = Modifier.fillMaxWidth().background(palette.card).padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space1")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(AlmadarSizes.w1_5).background(palette.primary, CircleShape))
            AlmadarText("Coordinator", variant = "caption", weight = "semibold")
            AlmadarText(count, variant = "caption", color = "muted")
        }
        AlmadarDivider()
    }

    @Composable
    fun CoordinatorSection() {
        if (messages.isNotEmpty()) {
            ConversationHeader("${messages.size} messages")
            TraceScroll(AlmadarSizes.maxWXl, messages.size) {
                messages.forEachIndexed { index, message -> TraceChatRow(message, index, onEvent) }
            }
            AlmadarDivider()
        } else if (activities.isNotEmpty()) {
            ConversationHeader("${activities.size} events")
            TraceScroll(AlmadarSizes.w60, activities.size) { TraceStream(traceCoordinatorItems(activities)) }
            AlmadarDivider()
        }
    }

    if (tabMode) {
        val entries = messages.map { TraceEntry("message", it) } + activities.map { TraceEntry("activity", it) } + subagents.map { TraceEntry("subagent", it) }
        Box(modifier = modifier.fillMaxSize().background(palette.card)) {
            Column(Modifier.fillMaxSize()) {
                Header { AlmadarText("Trace", variant = "caption", weight = "semibold") }
                if (entries.isEmpty()) {
                    Centered("No agent activity yet", "body2")
                } else {
                    LazyColumn(Modifier.weight(1f).almadarFlexFill()) {
                        itemsIndexed(entries) { index, entry ->
                            val icon = traceIcon(entry)
                            val stamp = traceTimeOf(entry)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { selected = entry }
                                    .padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space2")),
                                horizontalArrangement = Arrangement.spacedBy(almadarSpace("space3"))
                            ) {
                                Column(Modifier.width(almadarSpace("space5")).heightIn(min = almadarSpace("space6")), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(almadarSpace("space2")).background(palette.primary, CircleShape))
                                    if (index < entries.size - 1) {
                                        Box(Modifier.padding(top = almadarSpace("space1")).width(AlmadarSizes.w0_5).heightIn(min = almadarSpace("space6")).background(palette.border))
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1")), verticalAlignment = Alignment.CenterVertically) {
                                        AlmadarGlyph(icon.first, color = icon.second, size = "xs")
                                        if (stamp != null) AlmadarText(stamp, variant = "caption", color = "muted")
                                        AlmadarBadge(traceLabel(entry), variant = "neutral")
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2"))) {
                                        AlmadarText(tracePreviewOf(entry), modifier = Modifier.weight(1f), variant = "body2", color = "muted", truncate = true)
                                        AlmadarLocalButton(props, "", { selected = entry }, variant = "ghost", icon = "maximize-2", size = "sm", ariaLabel = "Expand")
                                    }
                                }
                            }
                            AlmadarDivider()
                        }
                    }
                }
            }
            selected?.let { entry ->
                val stamp = traceTimeOf(entry)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize().background(palette.scrim).clickable { selected = null })
                    AlmadarModalFrame(
                        props, traceLabel(entry) + (stamp?.let { " · $it" } ?: ""), "", true, "centered-card",
                        almadarModalCap("centered-card", "lg"), almadarModalFloor("centered-card", "lg"), null,
                        Modifier.padding(almadarSpace("space4")), onEvent, onClose = { selected = null }
                    ) { TraceDetail(entry, onEvent) }
                }
            }
        }
    } else if (props.bool("open")) {
        val shown = if (focused.isEmpty()) subagents else subagents.filter { it.string("orbitalName") == focused }
        val hasCoordinator = messages.isNotEmpty() || activities.isNotEmpty()
        val width = if (rich) AlmadarSizes.maxWMd else AlmadarSizes.w80
        val shape = almadarShape(AlmadarCorner.Container)
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            Column(
                modifier = Modifier
                    .widthIn(max = width)
                    .fillMaxWidth()
                    .heightIn(max = (screenHeight * 0.6f).dp)
                    .almadarMaterialFill(shape, palette.card)
                    .clip(shape)
                    .then(almadarBorder(AlmadarCorner.Container, geometry.borderStandard, palette.border))
                    .verticalScroll(rememberScrollState())
            ) {
                if (shown.isEmpty() && !hasCoordinator) {
                    Header { AlmadarText("Subagents", variant = "caption", weight = "semibold") }
                    Centered(if (focused.isEmpty()) "No subagents active" else "No subagents for $focused", "caption")
                } else if (focused.isNotEmpty()) {
                    Header {
                        Column {
                            AlmadarText(focused, variant = "caption", weight = "semibold", truncate = true)
                            AlmadarText("${shown.size} subagent${if (shown.size == 1) "" else "s"} · $densityLabel", variant = "caption", color = "muted")
                        }
                    }
                    shown.forEach { if (rich) TraceRichCard(it) else TraceSubagentRow(it) }
                } else {
                    Header { AlmadarText("Subagents (${shown.size}) · $densityLabel", variant = "caption", weight = "semibold") }
                    if (rich) TraceCoordinatorCard(activities)
                    CoordinatorSection()
                    if (rich) {
                        shown.forEach { TraceRichCard(it) }
                    } else {
                        val order = mutableListOf<String>()
                        val grouped = mutableMapOf<String, MutableList<JsonObject>>()
                        shown.forEach { sub ->
                            val key = (sub["orbitalName"] as? JsonPrimitive)?.contentOrNull ?: "(unattached)"
                            if (!grouped.containsKey(key)) order.add(key)
                            grouped.getOrPut(key) { mutableListOf() }.add(sub)
                        }
                        order.forEach { TraceOrbitalGroup(it, grouped[it] ?: emptyList()) }
                    }
                }
            }
        }
    }
}
