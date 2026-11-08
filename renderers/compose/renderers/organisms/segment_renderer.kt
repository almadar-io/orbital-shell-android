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

@Composable
fun SegmentRendererRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val declared = (props as? JsonObject)?.get("segments") as? JsonArray
    val segments: List<JsonObject> = remember(declared) { declared?.mapNotNull { it as? JsonObject } ?: emptyList() }
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
