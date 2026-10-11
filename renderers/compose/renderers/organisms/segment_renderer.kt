package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.border

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

private val segmentStops = "<(?:activate|connect|reflect|bloom|prq|question|answer|visualize)|\\n\\n#|\\z"
private const val SEGMENT_LEVELS = "remember|understand|apply|analyze|evaluate|create"

private fun segmentRegex(pattern: String): Regex = Regex(pattern, RegexOption.IGNORE_CASE)

private fun segmentExtract(content: String, name: String): Pair<String, String>? {
    for (pattern in listOf("<$name>([\\s\\S]*?)</$name>", "<$name>([\\s\\S]*?)(?=$segmentStops)")) {
        val found = segmentRegex(pattern).find(content)
        if (found != null) return found.groupValues[1].trim() to found.value
    }
    return null
}

private fun segmentDrop(full: String, content: String): String = content.replaceFirst(full, "").trim()

private fun segmentPieces(text: String): List<JsonObject> {
    val out = mutableListOf<JsonObject>()
    var last = 0
    Regex("```([^\\n\\r]*)\\r?\\n([\\s\\S]*?)```").findAll(text).forEach { fence ->
        val before = text.substring(last, fence.range.first)
        if (before.isNotBlank()) out.add(almadarJson("type" to "markdown", "content" to before))
        val tokens = fence.groupValues[1].trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        var language = tokens.firstOrNull() ?: "text"
        val runnable = language.endsWith("-runnable")
        if (runnable) language = language.removeSuffix("-runnable")
        out.add(almadarJson("type" to "code", "language" to language.ifEmpty { "text" }, "content" to fence.groupValues[2].trim(), "runnable" to (runnable || tokens.contains("run"))))
        last = fence.range.last + 1
    }
    val rest = text.substring(last)
    if (rest.isNotBlank()) out.add(almadarJson("type" to "markdown", "content" to rest))
    return out
}

private fun segmentBloom(level: String, body: String): JsonObject {
    val question = segmentRegex("<question>([\\s\\S]*?)</question>").find(body)?.groupValues?.get(1)?.trim()
    val answer = segmentRegex("<answer>([\\s\\S]*?)</answer>").find(body)?.groupValues?.get(1)?.trim()
    if (question != null) return almadarJson("type" to "bloom", "level" to level, "question" to question, "answer" to (answer ?: "(Answer not provided)"))
    val cleaned = body
        .replace(segmentRegex("^\\*\\*Question\\s*\\d*:?\\*\\*\\s*"), "")
        .replace(segmentRegex("^\\*\\*Q\\d*:?\\*\\*\\s*"), "")
        .trim()
    return almadarJson("type" to "bloom", "level" to level, "question" to cleaned, "answer" to "(See answers section below)")
}

private fun segmentParse(lesson: String): List<JsonObject> {
    var content = lesson.replace(segmentRegex("<prq>[\\s\\S]*?</prq>"), "").trim()
    val out = mutableListOf<JsonObject>()
    segmentExtract(content, "activate")?.let { (text, full) ->
        out.add(almadarJson("type" to "activate", "question" to text))
        content = segmentDrop(full, content)
    }
    segmentExtract(content, "connect")?.let { (text, full) ->
        out.add(almadarJson("type" to "connect", "content" to text))
        content = segmentDrop(full, content)
    }
    val tags = listOf(
        "(<reflect>([\\s\\S]*?)</reflect>)",
        "(<reflect>([\\s\\S]*?)(?=$segmentStops))",
        "(<bloom\\s+level=\"($SEGMENT_LEVELS)\">([\\s\\S]*?)</bloom>)",
        "(<bloom\\s+level=\"($SEGMENT_LEVELS)\">([\\s\\S]*?)(?=$segmentStops))",
        "(<question>([\\s\\S]*?)</question>\\s*<answer>([\\s\\S]*?)</answer>)",
        "(<visualize\\s+type=\"(?:algorithms|math|physics|biology|chemistry|probability)\"\\s+description=\"[^\"]*?\"\\s*/?>)"
    ).joinToString("|")
    var last = 0
    segmentRegex(tags).findAll(content).forEach { found ->
        val before = content.substring(last, found.range.first)
        if (before.isNotBlank()) out.addAll(segmentPieces(before))
        val g = found.groups
        fun grab(index: Int): String = g[index]?.value?.trim() ?: ""
        when {
            g[1] != null || g[3] != null -> {
                val prompt = if (g[2] != null) grab(2) else grab(4)
                if (prompt.isNotEmpty()) out.add(almadarJson("type" to "reflect", "prompt" to prompt))
            }
            g[5] != null || g[8] != null -> {
                val closed = g[5] != null
                val body = grab(if (closed) 7 else 10)
                if (body.isNotEmpty()) out.add(segmentBloom(grab(if (closed) 6 else 9).lowercase(), body))
            }
            g[11] != null -> out.add(almadarJson("type" to "quiz", "question" to grab(12), "answer" to grab(13)))
        }
        last = found.range.last + 1
    }
    val rest = content.substring(last)
    if (rest.isNotBlank()) out.addAll(segmentPieces(rest))
    return out
}

@Composable
fun SegmentRendererRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val lesson = (props as? JsonObject)?.get("lesson") as? JsonPrimitive
    val declared = (props as? JsonObject)?.get("segments") as? JsonArray
    val segments: List<JsonObject> = remember(lesson, declared) {
        if (lesson != null) segmentParse(lesson.content) else declared?.mapNotNull { it as? JsonObject } ?: emptyList()
    }
    val progress = (props as? JsonObject)?.get("userProgress") as? JsonObject
    val annotations = props.array("annotations").mapNotNull { it as? JsonObject }
    val owners = mutableMapOf<Int, MutableList<JsonObject>>()
    annotations.forEach { annotation ->
        val text = annotation.string("text")
        val at = segments.indexOfFirst { it.string("type") == "markdown" && it.string("content").contains(text) }
        if (at >= 0) owners.getOrPut(at) { mutableListOf() }.add(annotation)
    }
    val trait = almadarTrait(props)
    fun node(vararg pairs: Pair<String, JsonElement?>): JsonObject {
        val entries = mutableMapOf<String, JsonElement>()
        pairs.forEach { (key, value) -> if (value != null) entries[key] = value }
        if (trait != null) entries["sourceTrait"] = trait
        return JsonObject(entries)
    }
    fun carried(target: String, key: String): Pair<String, JsonElement?> =
        target to props.string(key).takeIf { it.isNotEmpty() }?.let { JsonPrimitive(it) }
    var reflectIndex = 0
    var bloomIndex = 0
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    if (segments.isNotEmpty()) {
        AlmadarBoxWithConstraints(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
            val pad = if (maxWidth >= AlmadarBreakpoints.md) almadarSpace("space4") else almadarSpace("space2")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(almadarBorder(AlmadarCorner.Container, geometry.borderStandard, palette.border))
                    .padding(pad),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("lg"))
            ) {
                segments.forEachIndexed { index, segment ->
                    when (segment.string("type")) {
                        "markdown" -> MarkdownContentRenderer(
                            node(
                                "content" to JsonPrimitive(segment.string("content")),
                                "annotations" to owners[index]?.let { JsonArray(it) },
                                carried("annotationEvent", "annotationEvent"), carried("askEvent", "askEvent"), carried("noteEvent", "noteEvent"),
                                carried("askLabel", "askLabel"), carried("noteLabel", "noteLabel")
                            ),
                            Modifier.fillMaxWidth(), onEvent
                        )
                        "code" -> if (segment.bool("runnable") && props.string("runCodeEvent").isNotEmpty()) {
                            val runId = index.toString()
                            CodeRunnerPanelRenderer(
                                node(
                                    "code" to JsonPrimitive(segment.string("content")), "language" to JsonPrimitive(segment.string("language", "text")),
                                    "runnable" to JsonPrimitive(true), "runEvent" to JsonPrimitive(props.string("runCodeEvent")),
                                    carried("resetEvent", "resetCodeEvent"), "runId" to JsonPrimitive(runId),
                                    "output" to (props as? JsonObject)?.get("codeOutputs")?.let { it as? JsonObject }?.get(runId),
                                    "running" to JsonPrimitive(props.string("runningCode") == runId)
                                ),
                                Modifier.fillMaxWidth(), onEvent
                            )
                        } else AlmadarFence(segment.string("content"), segment.string("language", "text"), props, Modifier.fillMaxWidth(), onEvent)
                        "quiz" -> QuizBlockRenderer(
                            node("question" to JsonPrimitive(segment.string("question")), "answer" to JsonPrimitive(segment.string("answer"))),
                            Modifier.fillMaxWidth(), onEvent
                        )
                        "activate" -> ActivationBlockRenderer(
                            node(
                                "question" to JsonPrimitive(segment.string("question")),
                                "savedResponse" to progress?.get("activationResponse"),
                                carried("saveEvent", "activationSaveEvent")
                            ),
                            Modifier.fillMaxWidth(), onEvent
                        )
                        "connect" -> ConnectionBlockRenderer(node("content" to JsonPrimitive(segment.string("content"))), Modifier.fillMaxWidth(), onEvent)
                        "reflect" -> {
                            val position = reflectIndex++
                            ReflectionBlockRenderer(
                                node(
                                    "prompt" to JsonPrimitive(segment.string("prompt")),
                                    "index" to JsonPrimitive(position),
                                    "savedNote" to (progress?.get("reflectionNotes") as? JsonArray)?.getOrNull(position),
                                    carried("saveEvent", "reflectionSaveEvent")
                                ),
                                Modifier.fillMaxWidth(), onEvent
                            )
                        }
                        "bloom" -> {
                            val position = bloomIndex++
                            BloomQuizBlockRenderer(
                                node(
                                    "level" to JsonPrimitive(segment.string("level")),
                                    "question" to JsonPrimitive(segment.string("question")),
                                    "answer" to JsonPrimitive(segment.string("answer")),
                                    "index" to JsonPrimitive(position),
                                    "isAnswered" to (progress?.get("bloomAnswered") as? JsonObject)?.get(position.toString()),
                                    carried("answerEvent", "bloomAnswerEvent")
                                ),
                                Modifier.fillMaxWidth(), onEvent
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
