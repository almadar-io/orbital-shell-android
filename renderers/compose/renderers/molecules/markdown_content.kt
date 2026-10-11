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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun MarkdownContentRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val blocks = almadarParseMarkdown(props.string("content"))
    val annotations = props.array("annotations").mapNotNull { it as? JsonObject }
    val ask = props.string("askEvent")
    val note = props.string("noteEvent")
    var selected by remember { mutableStateOf<Int?>(null) }
    val annotate: (String) -> Unit = { id ->
        val event = props.string("annotationEvent")
        if (event.isNotEmpty()) onEvent(event, eventEnvelope(props, almadarJson("annotationId" to id)))
    }
    val headingSize: (Int) -> String = { level ->
        when (level) { 1 -> "4xl"; 2 -> "3xl"; 3 -> "2xl"; 4 -> "xl"; 5 -> "lg"; else -> "base" }
    }
    val direction = if (props.string("direction", "ltr") == "rtl") LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Column(
            modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
        ) {
            blocks.forEachIndexed { index, block ->
                when (block.kind) {
                    "heading" -> AlmadarInlineText(
                        block.text, sizeKey = headingSize(block.level), weight = FontWeight.Bold,
                        isHeading = true, annotations = annotations, onAnnotation = annotate
                    )
                    "paragraph" -> {
                        val annotatable = ask.isNotEmpty() || note.isNotEmpty()
                        Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                            AlmadarInlineText(
                                block.text,
                                modifier = if (annotatable) {
                                    Modifier.fillMaxWidth()
                                        .background(if (selected == index) palette.primary.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(almadarSpaceFor("xs"))
                                        .verificationContainer(VerificationIds.row("paragraph-" + index))
                                } else Modifier,
                                annotations = annotations, onAnnotation = annotate,
                                onTap = if (annotatable) { { selected = if (selected == index) null else index } } else null
                            )
                            if (annotatable && selected == index) {
                                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                                    if (ask.isNotEmpty()) {
                                        AlmadarButton(
                                            props, props.string("askLabel", "Ask"), ask, onEvent,
                                            payload = almadarJson("selectedText" to block.text), variant = "secondary", size = "sm"
                                        )
                                    }
                                    if (note.isNotEmpty()) {
                                        AlmadarButton(
                                            props, props.string("noteLabel", "Note"), note, onEvent,
                                            payload = almadarJson("selectedText" to block.text), variant = "secondary", size = "sm"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "code" -> AlmadarFence(block.text, block.items.firstOrNull() ?: "", props, Modifier.fillMaxWidth(), onEvent)
                    "quote" -> Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                        Box(modifier = Modifier.width(LocalAlmadarGeometry.current.borderHeavy * 2).height(LocalAlmadarDensity.current.rowHeightNormal).background(palette.primary))
                        AlmadarInlineText(block.text, annotations = annotations, onAnnotation = annotate)
                    }
                    "list" -> Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        block.items.forEachIndexed { position, entry ->
                            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                                AlmadarText(if (block.ordered) (position + 1).toString() + "." else "•", color = "muted")
                                AlmadarInlineText(entry, annotations = annotations, onAnnotation = annotate)
                            }
                        }
                    }
                    "rule" -> AlmadarDivider()
                    "image" -> {
                        val split = block.text.lastIndexOf("](")
                        ImageRenderer(
                            almadarJson(
                                "src" to block.text.substring(split + 2).removeSuffix(")"),
                                "alt" to (if (split > 2) block.text.substring(2, split) else ""),
                                "fit" to "contain"
                            ),
                            Modifier.fillMaxWidth(), AlmadarNoEvent
                        )
                    }
                    "table" -> Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        block.rows.forEachIndexed { rowIndex, cells ->
                            Row(modifier = Modifier.background(if (rowIndex == 0) palette.muted else Color.Transparent).height(IntrinsicSize.Min)) {
                                cells.forEach { cell ->
                                    AlmadarInlineText(
                                        cell,
                                        modifier = Modifier.widthIn(min = LocalAlmadarDensity.current.rowHeightSpacious * 2)
                                            .border(LocalAlmadarGeometry.current.borderHairline, palette.border)
                                            .padding(almadarSpaceFor("sm")),
                                        sizeKey = "sm", weight = if (rowIndex == 0) FontWeight.Bold else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
