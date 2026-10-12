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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AlmadarCodeBlock(props: JsonElement, modifier: Modifier, onEvent: (String, JsonElement?) -> Unit, shared: MutableState<String>? = null) {
    val palette = LocalAlmadarTheme.current.color
    val files = props.array("files").mapNotNull { it as? JsonObject }
    val localDraft = remember { mutableStateOf(props.string("code")) }
    val draftState = shared ?: localDraft
    var folded by remember { mutableStateOf(false) }
    var wrapped by remember { mutableStateOf(props.bool("wordWrap")) }
    var copied by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(0) }
    var focused by remember { mutableStateOf(false) }
    var capturedKeys by remember { mutableStateOf(emptySet<String>()) }
    val setModeEvent = props.string("onSetMode", "SET_MODE")
    val editorId = props.string("editorId")
    // CodeBlock.tsx useEditorCapabilities: the latest SET_MODE for this editor names the keys it captures.
    LaunchedEffect(setModeEvent, editorId) {
        AlmadarBus.events.collect { bus ->
            val payload = bus.payload
            if (bus.event == setModeEvent && payload != null && payload.string("editorId") == editorId) {
                capturedKeys = payload.array("keys").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet()
            }
        }
    }
    val clipboard = LocalClipboardManager.current
    val file = files.getOrNull(minOf(active, maxOf(files.size - 1, 0)))
    val code = if (file != null) file.string("code") else if (props.bool("editable")) draftState.value else props.string("code")
    val language = if (file != null) file.string("language", props.string("language", "text")) else props.string("language", "text")
    val error = props.jsonObject("error")?.string("message") ?: ""
    val declaredDiff = props.array("diff").mapNotNull { it as? JsonObject }
    val raw = props as? JsonObject
    val diffLines: List<AlmadarDiffLine>? = if (declaredDiff.isNotEmpty()) {
        declaredDiff.map { AlmadarDiffLine(it.string("type"), null, it.int("lineNumber").takeIf { n -> n > 0 }, it.string("content")) }
    } else if (props.string("mode") == "diff" || raw?.get("oldValue") != null || raw?.get("newValue") != null) {
        almadarLineDiff(props.string("oldValue"), props.string("newValue"))
    } else null
    val maxHeight = props.string("maxHeight", "60vh")
    val screen = LocalConfiguration.current.screenHeightDp
    val limit = if (maxHeight.endsWith("vh")) (maxHeight.removeSuffix("vh").toFloatOrNull() ?: 60f).let { screen * it / 100f }.dp
        else parseCssLength(maxHeight, (screen * 0.6f).dp)
    val copyable = raw?.get("showCopy")?.jsonPrimitive?.booleanOrNull ?: props.bool("showCopyButton", true)
    val actions = props.array("actions").mapNotNull { it as? JsonObject }.filter { almadarActionEvent(it).isNotEmpty() }
    val title = props.string("title")
    if (props.bool("isLoading")) {
        SkeletonRenderer(almadarJson("spec" to "text"), modifier, AlmadarNoEvent)
    } else if (error.isNotEmpty()) {
        AlertRenderer(almadarJson("variant" to "error", "message" to error), modifier, AlmadarNoEvent)
    } else {
        AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), muted = true) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("sm"), vertical = almadarSpaceFor("xs")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
            ) {
                if (props.bool("foldable")) {
                    AlmadarLocalButton(
                        props, "", { folded = !folded },
                        variant = "ghost", size = "sm", icon = if (folded) "chevron-right" else "chevron-down",
                        ariaLabel = if (folded) "Expand code" else "Collapse code"
                    )
                }
                if (title.isNotEmpty()) AlmadarText(title, variant = "small", weight = "medium")
                if (props.bool("showLanguageBadge", true) && language.isNotEmpty()) AlmadarBadge(language)
                actions.forEach { action ->
                    AlmadarButton(
                        props, action.string("label"), almadarActionEvent(action), onEvent,
                        payload = almadarActionPayload(action, null),
                        variant = action.string("variant", "secondary"), icon = action.string("icon"), size = "sm"
                    )
                }
                AlmadarLocalButton(
                    props, "", { wrapped = !wrapped },
                    variant = "ghost", size = "sm", icon = "wrap-text",
                    ariaLabel = if (wrapped) "Disable word wrap" else "Enable word wrap"
                )
                if (copyable) {
                    AlmadarLocalButton(
                        props, "", { clipboard.setText(AnnotatedString(code)); copied = true },
                        variant = "ghost", size = "sm", icon = if (copied) "check" else "copy",
                        ariaLabel = if (copied) "Copied" else "Copy code"
                    )
                }
            }
            if (files.size > 1) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = almadarSpaceFor("xs")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                ) {
                    files.forEachIndexed { index, entry ->
                        AlmadarLocalButton(
                            props, entry.string("label"), { active = index },
                            variant = if (index == active) "secondary" else "ghost", size = "sm"
                        )
                    }
                }
            }
            if (!folded) {
                if (props.bool("editable") && files.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(max = limit).padding(almadarSpaceFor("xs")).onPreviewKeyEvent { event ->
                            // A captured key (or chord) is kept out of the text and emitted as UI:{onKey} — CodeBlock.tsx handleEditableKeyDown.
                            val keyEvent = props.string("onKey")
                            if (event.type != KeyEventType.KeyDown || keyEvent.isEmpty() || editorId.isEmpty()) return@onPreviewKeyEvent false
                            val key = almadarDomKey(event)
                            val chord = almadarKeyChord(key, event.isCtrlPressed, event.isAltPressed, event.isShiftPressed, event.isMetaPressed)
                            if (key !in capturedKeys && chord !in capturedKeys) return@onPreviewKeyEvent false
                            onEvent(keyEvent, eventEnvelope(props, JsonObject(mapOf("editorId" to JsonPrimitive(editorId), "key" to JsonPrimitive(key), "code" to JsonPrimitive(almadarDomCode(key)), "ctrl" to JsonPrimitive(event.isCtrlPressed), "alt" to JsonPrimitive(event.isAltPressed), "shift" to JsonPrimitive(event.isShiftPressed), "meta" to JsonPrimitive(event.isMetaPressed), "repeat" to JsonPrimitive(event.nativeKeyEvent.repeatCount > 0)))))
                            true
                        }.onFocusChanged { state ->
                            if (state.hasFocus != focused) {
                                focused = state.hasFocus
                                val event = props.string(if (focused) "onEditorFocus" else "onEditorBlur", if (focused) "EDITOR_FOCUS" else "EDITOR_BLUR")
                                if (event.isNotEmpty()) onEvent(event, eventEnvelope(props, almadarJson("editorId" to props.string("editorId"))))
                            }
                        }
                    ) {
                        AlmadarInput(
                            name = props.string("editorId", "code-editor"), value = draftState.value, onChange = { draftState.value = it },
                            inputType = "textarea", ariaLabel = title.ifEmpty { "Code editor" }
                        )
                    }
                } else if (code.isEmpty() && diffLines == null) {
                    AlmadarText("Nothing to show", modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("md")), variant = "small", color = "muted", align = "center")
                } else {
                    val marked = props.jsonObject("errorLines")
                    val numbered = props.bool("showLineNumbers") || props.bool("lineNumbers")
                    val shown = diffLines?.map { Triple(it.content, it.type, it.after ?: it.before) }
                        ?: code.split("\n").mapIndexed { index, line -> Triple(line, "plain", index + 1) }
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = limit).verticalScroll(rememberScrollState())
                            .let { if (wrapped) it else it.horizontalScroll(rememberScrollState()) }
                            .padding(vertical = almadarSpaceFor("sm"))
                    ) {
                        shown.forEach { (text, kind, number) ->
                            val added = kind == "add" || kind == "added"
                            val removed = kind == "remove" || kind == "removed"
                            val tone = marked?.string((number ?: 0).toString()) ?: ""
                            val tint = if (added) palette.success else if (removed) palette.error else palette.foreground
                            val fill = if (tone == "error") palette.error.copy(alpha = 0.15f)
                                else if (tone == "warning") palette.warning.copy(alpha = 0.15f)
                                else if (added) palette.success.copy(alpha = 0.1f)
                                else if (removed) palette.error.copy(alpha = 0.1f)
                                else Color.Transparent
                            Row(
                                modifier = Modifier.background(fill).padding(horizontal = almadarSpaceFor("sm")),
                                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
                            ) {
                                if (numbered) AlmadarMonoText(number?.toString() ?: "", Modifier.widthIn(min = almadarSpaceFor("xl")), color = palette.mutedForeground)
                                if (kind != "plain") AlmadarMonoText(if (added) "+" else if (removed) "-" else " ", color = tint)
                                AlmadarMonoText(text.ifEmpty { " " }, color = if (kind == "plain") null else tint)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CodeBlockRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    AlmadarCodeBlock(props, modifier, onEvent)
}
