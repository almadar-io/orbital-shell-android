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

// tier: molecules — a grouped container styled by tokens only

@Composable
fun BloomQuizBlockRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val level = props.string("level", "remember")
    val palette = LocalAlmadarTheme.current.color
    val shape = almadarShape(AlmadarCorner.Container)
    val standard = LocalAlmadarGeometry.current.borderStandard
    val answerEvent = props.string("answerEvent", "ANSWER_BLOOM")
    var revealed by remember { mutableStateOf(false) }
    val pill = when (level) {
        "understand" -> palette.info to palette.infoForeground
        "apply" -> palette.success to palette.successForeground
        "analyze" -> palette.warning to palette.warningForeground
        "evaluate" -> palette.accent to palette.accentForeground
        "create" -> palette.primary to palette.primaryForeground
        else -> palette.secondary to palette.secondaryForeground
    }
    val tint = if (level == "remember" || level !in setOf("understand", "apply", "analyze", "evaluate", "create")) palette.muted else pill.first.copy(alpha = 0.1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tint, shape)
            .then(almadarBorder(AlmadarCorner.Container, standard, palette.primary))
            .padding(almadarSpace("space4"))
            .almadarLabel(props.string("aria-label")),
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
            if ((props as? JsonObject)?.get("index") != null) {
                AlmadarText("Question ${props.int("index") + 1}", variant = "small", color = "muted", weight = "medium")
            }
            CompositionLocalProvider(LocalContentColor provides pill.second) {
                AlmadarText(
                    level.replaceFirstChar { it.uppercase() }, variant = "caption", color = "inherit", weight = "medium",
                    modifier = Modifier.background(pill.first, almadarShape(AlmadarCorner.Pill)).padding(horizontal = almadarSpace("space2"), vertical = almadarSpace("space1"))
                )
            }
            Spacer(Modifier.weight(1f))
            if (props.bool("isAnswered")) AlmadarGlyph("check-circle", color = "success")
        }
        MarkdownContentRenderer(almadarJson("content" to props.string("question"), "sourceTrait" to almadarTrait(props)), Modifier.fillMaxWidth(), onEvent)
        AlmadarButton(
            props, if (revealed) "Hide Answer" else "Reveal Answer", answerEvent,
            { _, _ ->
                if (!revealed) onEvent(answerEvent, eventEnvelope(props, almadarJson("index" to props.int("index"), "level" to level)))
                revealed = !revealed
            },
            size = "sm"
        )
        if (revealed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.card, shape)
                    .then(almadarBorder(AlmadarCorner.Container, standard, palette.primary))
                    .padding(almadarSpace("space3")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) {
                AlmadarText("ANSWER:", variant = "caption", color = "muted", weight = "medium")
                MarkdownContentRenderer(almadarJson("content" to props.string("answer"), "sourceTrait" to almadarTrait(props)), Modifier.fillMaxWidth(), onEvent)
            }
        }
    }
}
