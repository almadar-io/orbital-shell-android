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

private class ContentSegment(val type: String, val content: String, val language: String, val question: String, val answer: String)

private val contentQuizTag = Regex("<question>([\\s\\S]*?)</question>\\s*<answer>([\\s\\S]*?)</answer>", RegexOption.IGNORE_CASE)
private val contentFence = Regex("```(\\w+)?\\n([\\s\\S]*?)```")

private fun contentIsOrbital(code: String): Boolean {
    val parsed = try {
        Json.parseToJsonElement(code) as? JsonObject
    } catch (failure: kotlinx.serialization.SerializationException) {
        null
    }
    return parsed != null && ((parsed.containsKey("states") && parsed.containsKey("transitions")) || parsed["orbitals"] is JsonArray)
}

private fun contentFenced(text: String): List<ContentSegment> {
    val out = mutableListOf<ContentSegment>()
    var last = 0
    contentFence.findAll(text).forEach { match ->
        val before = text.substring(last, match.range.first)
        if (before.isNotBlank()) out.add(ContentSegment("markdown", before, "", "", ""))
        val language = match.groupValues[1].ifEmpty { "text" }
        val code = match.groupValues[2].trim()
        val orbital = (language == "json" || language == "orb") && contentIsOrbital(code)
        out.add(ContentSegment(if (orbital) "orbital" else "code", code, language, "", ""))
        last = match.range.last + 1
    }
    val rest = text.substring(last)
    if (rest.isNotBlank()) out.add(ContentSegment("markdown", rest, "", "", ""))
    return out
}

private fun contentSegments(content: String): List<ContentSegment> {
    val out = mutableListOf<ContentSegment>()
    var last = 0
    contentQuizTag.findAll(content).forEach { match ->
        val before = content.substring(last, match.range.first)
        if (before.isNotBlank()) out.addAll(contentFenced(before))
        out.add(ContentSegment("quiz", "", "", match.groupValues[1].trim(), match.groupValues[2].trim()))
        last = match.range.last + 1
    }
    val rest = content.substring(last)
    if (rest.isNotBlank()) out.addAll(contentFenced(rest))
    return out
}

private fun contentDeclared(raw: List<JsonElement>): List<ContentSegment> = raw.mapNotNull { item ->
    val obj = item as? JsonObject ?: return@mapNotNull null
    ContentSegment(obj.string("type"), obj.string("content"), obj.string("language"), obj.string("question"), obj.string("answer"))
}

@Composable
fun ContentRendererRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val declared = (props as? JsonObject)?.get("segments") as? JsonArray
    val content = props.string("content")
    val segments = remember(declared, content) { if (declared != null) contentDeclared(declared.toList()) else contentSegments(content) }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "text", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier)
    } else if (segments.isNotEmpty()) {
        Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
            segments.forEach { segment ->
                when (segment.type) {
                    "markdown" -> MarkdownContentRenderer(
                        almadarJson("content" to segment.content, "direction" to props.string("direction"), "sourceTrait" to almadarTrait(props)),
                        Modifier.fillMaxWidth(), onEvent
                    )
                    "code", "orbital" -> CodeBlockRenderer(
                        almadarJson("code" to segment.content, "language" to segment.language, "sourceTrait" to almadarTrait(props)),
                        Modifier.fillMaxWidth(), onEvent
                    )
                    "quiz" -> QuizBlockRenderer(
                        almadarJson("question" to segment.question, "answer" to segment.answer, "sourceTrait" to almadarTrait(props)),
                        Modifier.fillMaxWidth(), onEvent
                    )
                    else -> Unit
                }
            }
        }
    }
}
