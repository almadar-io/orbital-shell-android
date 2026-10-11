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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun CodeRunnerPanelRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val language = props.string("language", "text")
    if (!props.bool("runnable")) {
        CodeBlockRenderer(props.merging(mapOf("code" to JsonPrimitive(props.string("code")), "language" to JsonPrimitive(language))), modifier, onEvent)
    } else {
        val palette = LocalAlmadarTheme.current.color
        val geometry = LocalAlmadarGeometry.current
        val shape = almadarShape(AlmadarCorner.Container)
        val initial = props.string("code")
        val code = remember { mutableStateOf(initial) }
        var copied by remember { mutableStateOf(false) }
        val clipboard = LocalClipboardManager.current
        val copyEvent = "COPY_CODE"
        val copyAction: (String, JsonElement?) -> Unit = { _, _ ->
            clipboard.setText(AnnotatedString(code.value))
            copied = true
            onEvent(copyEvent, eventEnvelope(props, JsonObject(mapOf("language" to JsonPrimitive(language), "success" to JsonPrimitive(true)))))
        }
        val runEvent = props.string("runEvent", "RUN_CODE")
        val resetEvent = props.string("resetEvent")
        val runId = props.string("runId")
        val running = props.bool("running")
        val failure = props.string("error")
        val output = props.jsonObject("output")
        val blockProps = almadarJson(
            "code" to initial, "language" to language, "editable" to true, "showLanguageBadge" to true,
            "showCopyButton" to false, "maxHeight" to "22.5rem", "sourceTrait" to almadarTrait(props)
        )
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(almadarSpace("space3"))) {
            Box(modifier = Modifier.fillMaxWidth().height(parseCssLength("22.5rem", Dp.Unspecified))) {
                AlmadarCodeBlock(blockProps, Modifier.fillMaxSize(), onEvent, code)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(almadarSpace("space2"))
                        .clip(shape)
                        .background(palette.card.copy(alpha = 0.9f))
                        .then(almadarBorder(AlmadarCorner.Container, geometry.borderStandard, palette.border))
                        .padding(almadarSpace("space1")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpace("space1")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (copied) {
                        Box(
                            modifier = Modifier.heightIn(min = LocalAlmadarDensity.current.buttonHeightSm).padding(horizontal = almadarSpaceFor("sm"))
                                .almadarTag(almadarActionId(copyEvent, props)).clickable { copyAction("", null) },
                            contentAlignment = Alignment.Center
                        ) { AlmadarGlyph("check", color = "success", size = "sm", label = "Copy") }
                    } else AlmadarButton(
                        props, "", copyEvent,
                        copyAction,
                        variant = "ghost", icon = "copy", size = "sm", ariaLabel = "Copy"
                    )
                    if (resetEvent.isNotEmpty()) {
                        AlmadarButton(
                            props, "", resetEvent,
                            { _, _ ->
                                code.value = initial
                                onEvent(resetEvent, eventEnvelope(props, JsonObject(mapOf("runId" to JsonPrimitive(runId)))))
                            },
                            variant = "ghost", icon = "rotate-ccw", size = "sm", disabled = running, ariaLabel = "Reset"
                        )
                    } else {
                        AlmadarLocalButton(props, "", { code.value = initial }, variant = "ghost", icon = "rotate-ccw", size = "sm", disabled = running, ariaLabel = "Reset")
                    }
                    AlmadarButton(
                        props, if (running) "Loading..." else "Run", runEvent,
                        { _, _ -> onEvent(runEvent, eventEnvelope(props, JsonObject(mapOf("code" to JsonPrimitive(code.value), "language" to JsonPrimitive(language), "runId" to JsonPrimitive(runId))))) },
                        variant = "primary", icon = if (running) "rotate-cw" else "play", size = "sm", loading = running
                    )
                }
            }
            if (output != null || failure.isNotEmpty()) {
                val exit = output?.int("exitCode") ?: 0
                val stdout = output?.string("stdout") ?: ""
                val stderr = output?.string("stderr") ?: ""
                val tests = output?.array("testResults")?.mapNotNull { it as? JsonObject } ?: emptyList()
                Column(
                    modifier = Modifier.fillMaxWidth().clip(shape).background(palette.foreground)
                        .then(almadarBorder(AlmadarCorner.Container, geometry.borderStandard, palette.border))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(palette.card)
                            .padding(horizontal = almadarSpace("space3"), vertical = almadarSpace("space2")),
                        horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2")),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlmadarGlyph("terminal", color = "muted", size = "sm")
                        AlmadarText("Output", variant = "small", weight = "medium")
                        if (output != null) AlmadarBadge("Exit $exit", variant = if (exit == 0) "success" else "danger")
                    }
                    AlmadarDivider()
                    Column(modifier = Modifier.fillMaxWidth().padding(almadarSpace("space3"))) {
                        if (failure.isNotEmpty()) {
                            AlmadarMonoText(failure, size = "sm", color = palette.error)
                        } else {
                            if (stdout.isNotEmpty()) AlmadarMonoText(stdout, size = "sm", color = palette.background)
                            if (stderr.isNotEmpty()) AlmadarMonoText(stderr, size = "sm", color = palette.error)
                            if (stdout.isEmpty() && stderr.isEmpty()) AlmadarMonoText("No output", size = "sm", color = palette.background, italic = true)
                            if (tests.isNotEmpty()) {
                                Column(modifier = Modifier.padding(top = almadarSpace("space3")), verticalArrangement = Arrangement.spacedBy(almadarSpace("space2"))) {
                                    AlmadarDivider()
                                    tests.forEachIndexed { index, test ->
                                        val passed = test.bool("passed")
                                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2"))) {
                                            AlmadarGlyph(if (passed) "check-circle" else "x-circle", color = if (passed) "success" else "error", size = "sm")
                                            Column(verticalArrangement = Arrangement.spacedBy(almadarSpace("space1"))) {
                                                AlmadarMonoText(if (passed) "Test ${index + 1}: passed" else "Test ${index + 1}: failed", size = "sm", color = if (passed) palette.success else palette.error)
                                                AlmadarMonoText("Input: ${test.string("input")}", size = "sm", color = palette.background)
                                                AlmadarMonoText("Expected: ${test.string("expectedOutput")}", size = "sm", color = palette.background)
                                                AlmadarMonoText("Actual: ${test.string("actualOutput")}", size = "sm", color = palette.background)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
