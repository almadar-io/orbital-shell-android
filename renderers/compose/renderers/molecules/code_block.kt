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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.offset

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
                if (bufferModeOn(props)) {
                    AlmadarCodeBuffer(props, limit, onEvent)
                } else if (props.bool("editable") && files.isEmpty()) {
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

internal data class BufferCursor(val row: Int, val col: Int)
internal data class BufferSelection(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int, val linewise: Boolean)
internal data class BufferDecoration(val id: String, val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int, val tone: String, val label: String?)
internal data class BufferPopover(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int, val title: String, val sections: List<Pair<String, List<String>>>, val busy: Boolean)
internal data class BufferPaint(
    val fgRole: String? = null,
    val bgRole: String? = null,
    val bgAlpha: Float = 1f,
    val italic: Boolean = false,
    val barBefore: Boolean = false,
    val underlineCaret: Boolean = false
)

private val bufferSyntaxRoles = mapOf(
    "keyword" to "primary", "string" to "success", "comment" to "mutedForeground", "number" to "warning",
    "type" to "info", "function" to "accent", "operator" to "foreground", "punctuation" to "mutedForeground", "variable" to "foreground"
)
private val bufferAnnotationRoles = mapOf(
    "search" to Pair("warning", 0.35f), "review" to Pair("info", 0.2f), "review-recent" to Pair("info", 0.4f),
    "error" to Pair("error", 0.3f), "warning" to Pair("warning", 0.25f), "info" to Pair("info", 0.2f)
)

internal fun bufferModeOn(props: JsonElement): Boolean = (props as? JsonObject)?.get("cursor") is JsonObject

internal fun bufferCursor(props: JsonElement): BufferCursor? =
    props.jsonObject("cursor")?.let { BufferCursor(it.int("row"), it.int("col")) }

internal fun bufferSelection(props: JsonElement): BufferSelection? =
    props.jsonObject("selection")?.let { BufferSelection(it.int("fromRow"), it.int("fromCol"), it.int("toRow"), it.int("toCol"), it.bool("linewise")) }

internal fun bufferDecorations(props: JsonElement): List<BufferDecoration> =
    props.array("decorations").mapNotNull { it as? JsonObject }.map {
        BufferDecoration(it.string("id"), it.int("fromRow"), it.int("fromCol"), it.int("toRow"), it.int("toCol"), it.string("tone"), it.string("label").ifEmpty { null })
    }

internal fun bufferPopover(props: JsonElement): BufferPopover? =
    props.jsonObject("popover")?.let { p ->
        BufferPopover(
            p.int("fromRow"), p.int("fromCol"), p.int("toRow"), p.int("toCol"), p.string("title"),
            p.array("sections").mapNotNull { it as? JsonObject }.map { section ->
                Pair(section.string("heading"), section.array("lines").mapNotNull { (it as? JsonPrimitive)?.contentOrNull })
            },
            p.bool("busy")
        )
    }

/** A line as its characters: Unicode scalar values, the units of `str/len`. */
internal fun bufferCells(line: String): List<String> {
    val cells = mutableListOf<String>()
    var index = 0
    while (index < line.length) {
        val width = Character.charCount(line.codePointAt(index))
        cells += line.substring(index, index + width)
        index += width
    }
    return cells
}

internal fun bufferGutter(count: Int, cursorRow: Int, relative: Boolean): List<String> =
    List(count) { row -> if (!relative || row == cursorRow) (row + 1).toString() else kotlin.math.abs(row - cursorRow).toString() }

private fun bufferRange(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): List<Int> =
    if (fromRow < toRow || (fromRow == toRow && fromCol <= toCol)) listOf(fromRow, fromCol, toRow, toCol) else listOf(toRow, toCol, fromRow, fromCol)

/** The cells `[start, end)` of a row a selection paints, the end cell included at the max end; the line-end cell is index `len`. */
private fun bufferSelectionSpan(selection: BufferSelection, row: Int, len: Int): IntRange? {
    val (r0, c0, r1, c1) = bufferRange(selection.fromRow, selection.fromCol, selection.toRow, selection.toCol)
    if (row < r0 || row > r1) return null
    if (selection.linewise) return 0..len
    val start = if (row == r0) c0 else 0
    val end = if (row == r1) c1 else len
    return maxOf(start, 0)..minOf(end, len)
}

private fun bufferDecorationSpan(decoration: BufferDecoration, row: Int, len: Int): IntRange? {
    val (r0, c0, r1, c1) = bufferRange(decoration.fromRow, decoration.fromCol, decoration.toRow, decoration.toCol)
    if (row < r0 || row > r1) return null
    val start = if (row == r0) c0 else 0
    val end = if (row == r1) c1 else len
    return if (end <= start) null else maxOf(start, 0)..(minOf(end, len + 1) - 1)
}

/** One paint per character cell plus the line-end cell, as theme token roles, never colors. */
internal fun bufferLinePaint(line: String, row: Int, cursor: BufferCursor?, caret: String, selection: BufferSelection?, decorations: List<BufferDecoration>): List<BufferPaint> {
    val len = bufferCells(line).size
    val paints = MutableList(len + 1) { BufferPaint() }
    for (decoration in decorations) {
        val span = bufferDecorationSpan(decoration, row, len) ?: continue
        val syntax = bufferSyntaxRoles[decoration.tone]
        val annotation = bufferAnnotationRoles[decoration.tone]
        for (i in span) {
            if (syntax != null) paints[i] = paints[i].copy(fgRole = syntax, italic = decoration.tone == "comment")
            else if (annotation != null) paints[i] = paints[i].copy(bgRole = annotation.first, bgAlpha = annotation.second)
        }
    }
    if (selection != null) bufferSelectionSpan(selection, row, len)?.let { span ->
        for (i in span) paints[i] = paints[i].copy(bgRole = "primary", bgAlpha = 0.3f)
    }
    if (cursor != null && cursor.row == row) {
        val col = cursor.col.coerceIn(0, len)
        paints[col] = when (caret) {
            "bar" -> paints[col].copy(barBefore = true)
            "underline" -> paints[col].copy(underlineCaret = true)
            else -> paints[col].copy(fgRole = "background", bgRole = "foreground", bgAlpha = 1f)
        }
    }
    return paints
}

internal fun bufferColAt(x: Float, cellWidth: Float, gutterWidth: Float, len: Int): Int =
    if (cellWidth <= 0f) 0 else kotlin.math.floor((x - gutterWidth) / cellWidth).toInt().coerceIn(0, len)

/** The row under `y` from the visible rows' (index, offset, size); above the first gives the first, below the last gives the last. */
internal fun bufferRowAt(y: Float, visible: List<Triple<Int, Int, Int>>): Int {
    val hit = visible.firstOrNull { y >= it.second && y < it.second + it.third }
    return hit?.first ?: if (visible.isEmpty() || y < visible.first().second) visible.firstOrNull()?.first ?: 0 else visible.last().first
}

/** The item to scroll to so `row` shows, or null when it already does. */
internal fun bufferScrollTarget(firstVisible: Int, lastVisible: Int, row: Int): Int? = when {
    row < firstVisible -> row
    row > lastVisible -> maxOf(0, row - (lastVisible - firstVisible))
    else -> null
}

internal fun bufferKeyPayload(editorId: String, key: String, code: String, ctrl: Boolean, alt: Boolean, shift: Boolean, meta: Boolean, repeat: Boolean): JsonObject =
    JsonObject(linkedMapOf(
        "editorId" to JsonPrimitive(editorId), "key" to JsonPrimitive(key), "code" to JsonPrimitive(code),
        "ctrl" to JsonPrimitive(ctrl), "alt" to JsonPrimitive(alt), "shift" to JsonPrimitive(shift), "meta" to JsonPrimitive(meta), "repeat" to JsonPrimitive(repeat)
    ))

internal fun bufferPointerPayload(editorId: String, row: Int, col: Int): JsonObject =
    JsonObject(linkedMapOf("editorId" to JsonPrimitive(editorId), "row" to JsonPrimitive(row), "col" to JsonPrimitive(col)))

private fun bufferNamedKey(event: androidx.compose.ui.input.key.KeyEvent): String = when (event.key) {
    androidx.compose.ui.input.key.Key.ShiftLeft, androidx.compose.ui.input.key.Key.ShiftRight -> "Shift"
    androidx.compose.ui.input.key.Key.CtrlLeft, androidx.compose.ui.input.key.Key.CtrlRight -> "Control"
    androidx.compose.ui.input.key.Key.AltLeft, androidx.compose.ui.input.key.Key.AltRight -> "Alt"
    androidx.compose.ui.input.key.Key.MetaLeft, androidx.compose.ui.input.key.Key.MetaRight -> "Meta"
    androidx.compose.ui.input.key.Key.CapsLock -> "CapsLock"
    androidx.compose.ui.input.key.Key.Insert -> "Insert"
    else -> ""
}

/** KeyboardEvent.key: with a chord modifier held the platform's character is control-mapped, so the unmodified (shift-aware) one is read. */
private fun bufferDomKey(event: androidx.compose.ui.input.key.KeyEvent): String {
    val named = almadarDomKey(event)
    val chord = event.isCtrlPressed || event.isAltPressed || event.isMetaPressed
    if (named.length == 1 && !chord) return named
    if (named.length > 1) return named
    val fromNamed = bufferNamedKey(event)
    if (fromNamed.isNotEmpty()) return fromNamed
    val shifted = if (event.isShiftPressed) android.view.KeyEvent.META_SHIFT_ON else 0
    val point = event.nativeKeyEvent.getUnicodeChar(shifted)
    return if (point > 0) String(Character.toChars(point)) else named
}

@Composable
internal fun AlmadarCodeBuffer(props: JsonElement, limit: androidx.compose.ui.unit.Dp, onEvent: (String, JsonElement?) -> Unit) {
    val theme = LocalAlmadarTheme.current
    val palette = theme.color
    val density = androidx.compose.ui.platform.LocalDensity.current
    val code = props.string("code")
    val lines = code.split("\n")
    val cursor = bufferCursor(props)
    val caret = props.string("caret", "block")
    val selection = bufferSelection(props)
    val decorations = bufferDecorations(props)
    val popover = bufferPopover(props)
    val numbered = props.bool("showLineNumbers") || props.bool("lineNumbers")
    val gutter = if (numbered) bufferGutter(lines.size, cursor?.row ?: -1, props.bool("relativeLineNumbers")) else emptyList()
    val editorId = props.string("editorId")
    val keyEvent = props.string("onKey")
    val pointerEvent = props.string("onPointerDown")
    val hoverEvent = props.string("onHover")
    val listState = rememberLazyListState()
    val hScroll = rememberScrollState()
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var viewport by remember { mutableStateOf(0) }
    var lastHover by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = almadarType("sm").copy(fontFamily = theme.typeScale.monoFamily)
    val cell = remember(style) { measurer.measure("M", style).size }
    val cellWidth = cell.width.toFloat()
    val padPx = with(density) { almadarSpaceFor("sm").toPx() }
    val gutterPx = if (numbered) gutter.maxOf { it.length } * cellWidth + padPx * 2 else 0f
    val currentLines by rememberUpdatedState(lines)
    val currentProps by rememberUpdatedState(props)

    fun role(name: String?): Color? = when (name) {
        "primary" -> palette.primary
        "success" -> palette.success
        "mutedForeground" -> palette.mutedForeground
        "warning" -> palette.warning
        "info" -> palette.info
        "accent" -> palette.accent
        "foreground" -> palette.foreground
        "background" -> palette.background
        "error" -> palette.error
        else -> null
    }

    LaunchedEffect(cursor?.row, cursor?.col) {
        val row = cursor?.row ?: return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) listState.scrollToItem(row.coerceIn(0, lines.size - 1))
        else bufferScrollTarget(visible.first().index, visible.last().index, row)?.let { listState.scrollToItem(it) }
        val x = (gutterPx + (cursor.col + 1) * cellWidth).toInt()
        if (viewport > 0 && x > hScroll.value + viewport) hScroll.scrollTo(x - viewport)
        else if (viewport > 0 && gutterPx + cursor.col * cellWidth < hScroll.value) hScroll.scrollTo(maxOf(0, (gutterPx + cursor.col * cellWidth).toInt() - padPx.toInt()))
    }

    fun hit(position: androidx.compose.ui.geometry.Offset): Pair<Int, Int> {
        val rows = listState.layoutInfo.visibleItemsInfo.map { Triple(it.index, it.offset, it.size) }
        val row = bufferRowAt(position.y, rows).coerceIn(0, currentLines.size - 1)
        return Pair(row, bufferColAt(position.x + hScroll.value, cellWidth, gutterPx, bufferCells(currentLines[row]).size))
    }

    Box(
        modifier = Modifier.fillMaxWidth().heightIn(max = limit)
            .onSizeChanged { viewport = it.width }
            .onFocusChanged { state ->
                if (state.hasFocus != focused) {
                    focused = state.hasFocus
                    val name = props.string(if (focused) "onEditorFocus" else "onEditorBlur", if (focused) "EDITOR_FOCUS" else "EDITOR_BLUR")
                    if (name.isNotEmpty()) onEvent(name, eventEnvelope(props, almadarJson("editorId" to editorId)))
                }
            }
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || keyEvent.isEmpty()) return@onPreviewKeyEvent false
                val key = bufferDomKey(event)
                if (key.isEmpty()) return@onPreviewKeyEvent false
                val payload = bufferKeyPayload(editorId, key, almadarDomCode(key), event.isCtrlPressed, event.isAltPressed, event.isShiftPressed, event.isMetaPressed, event.nativeKeyEvent.repeatCount > 0)
                onEvent(keyEvent, eventEnvelope(currentProps, payload))
                true
            }
            .focusable()
            .pointerInput(pointerEvent, hoverEvent, editorId) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press && change.pressed) {
                            focusRequester.requestFocus()
                            if (pointerEvent.isNotEmpty()) {
                                val (row, col) = hit(change.position)
                                onEvent(pointerEvent, eventEnvelope(currentProps, bufferPointerPayload(editorId, row, col)))
                            }
                        } else if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Move && hoverEvent.isNotEmpty() && change.type != androidx.compose.ui.input.pointer.PointerType.Touch) {
                            val cellHit = hit(change.position)
                            if (cellHit != lastHover) {
                                lastHover = cellHit
                                onEvent(hoverEvent, eventEnvelope(currentProps, bufferPointerPayload(editorId, cellHit.first, cellHit.second)))
                            }
                        }
                    }
                }
            }
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().horizontalScroll(hScroll).padding(vertical = almadarSpaceFor("sm"))) {
            items(count = lines.size) { row ->
                val paints = bufferLinePaint(lines[row], row, cursor, caret, selection, decorations)
                val cells = bufferCells(lines[row]) + " "
                val labels = decorations.filter { it.label != null && row in minOf(it.fromRow, it.toRow)..maxOf(it.fromRow, it.toRow) }.mapNotNull { it.label }
                val text = buildAnnotatedString {
                    cells.forEachIndexed { i, ch ->
                        val paint = paints[i]
                        withStyle(SpanStyle(
                            color = role(paint.fgRole) ?: Color.Unspecified,
                            background = role(paint.bgRole)?.copy(alpha = paint.bgAlpha) ?: Color.Unspecified,
                            fontStyle = if (paint.italic) androidx.compose.ui.text.font.FontStyle.Italic else null
                        )) { append(ch) }
                    }
                }
                val barAt = paints.indexOfFirst { it.barBefore }
                val underlineAt = paints.indexOfFirst { it.underlineCaret }
                val caretColor = palette.foreground
                Row(
                    modifier = Modifier.drawBehind {
                        if (barAt >= 0) drawRect(caretColor, androidx.compose.ui.geometry.Offset(gutterPx + barAt * cellWidth, 0f), androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                        if (underlineAt >= 0) drawRect(caretColor, androidx.compose.ui.geometry.Offset(gutterPx + underlineAt * cellWidth, size.height - 2.dp.toPx()), androidx.compose.ui.geometry.Size(cellWidth, 2.dp.toPx()))
                    }.let { if (labels.isEmpty()) it else it.semantics { contentDescription = labels.joinToString(", ") } }
                ) {
                    if (numbered) {
                        Text(
                            gutter[row], color = if (row == cursor?.row) palette.foreground else palette.mutedForeground, style = style, softWrap = false,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            modifier = Modifier.width(with(density) { gutterPx.toDp() }).padding(end = almadarSpaceFor("sm"))
                        )
                    }
                    Text(text, color = palette.foreground, style = style, softWrap = false)
                }
            }
        }
        if (popover != null) {
            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == popover.fromRow }
            if (info != null) {
                val x = (gutterPx + popover.fromCol * cellWidth - hScroll.value).toInt()
                Box(modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(x, info.offset) }.size(with(density) { cellWidth.toDp() }, with(density) { info.size.toDp() })) {
                    androidx.compose.ui.window.Popup(
                        popupPositionProvider = object : androidx.compose.ui.window.PopupPositionProvider {
                            override fun calculatePosition(anchorBounds: androidx.compose.ui.unit.IntRect, windowSize: androidx.compose.ui.unit.IntSize, layoutDirection: androidx.compose.ui.unit.LayoutDirection, popupContentSize: androidx.compose.ui.unit.IntSize): androidx.compose.ui.unit.IntOffset {
                                val below = anchorBounds.bottom
                                val top = if (below + popupContentSize.height <= windowSize.height) below else maxOf(0, anchorBounds.top - popupContentSize.height)
                                return androidx.compose.ui.unit.IntOffset(anchorBounds.left.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width)), top)
                            }
                        },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                    ) {
                        Surface(
                            modifier = Modifier.widthIn(max = almadarSpaceFor("2xl") * 5),
                            shape = MaterialTheme.shapes.medium,
                            color = palette.card,
                            contentColor = palette.cardForeground,
                            border = androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderStandard, palette.border),
                            shadowElevation = LocalAlmadarElevation.current.popoverElevation.radius
                        ) {
                            Column(modifier = Modifier.padding(almadarSpaceFor("md")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                if (popover.title.isNotEmpty()) AlmadarText(popover.title, variant = "small", weight = "medium")
                                popover.sections.forEach { (heading, bodyLines) ->
                                    if (heading.isNotEmpty()) AlmadarText(heading, variant = "small", weight = "medium", color = "muted")
                                    bodyLines.forEach { AlmadarText(it, variant = "small") }
                                }
                                if (popover.busy) AlmadarGlyph("loader", color = "primary", size = "sm", animation = "spin")
                            }
                        }
                    }
                }
            }
        }
    }
}
