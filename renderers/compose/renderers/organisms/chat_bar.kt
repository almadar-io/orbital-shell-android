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

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent

@Composable
fun ChatBarRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val sendEvent = props.string("sendEvent", "CHAT_SEND")
    val status = props.string("status", "idle")
    val context = props.string("context")
    val gate = props.string("activeGate")
    val jepa = (props as? JsonObject)?.get("jepaValidity")?.let { (it as? JsonPrimitive)?.doubleOrNull }
    val running = status == "running"
    val retryEvent = "RETRY_GENERATION"
    val pauseEvent = "PAUSE"
    val stopEvent = "STOP"
    val resumeEvent = "RESUME"
    var text by remember { mutableStateOf("") }
    // The host's focus request (`focusRequest`): its id is the field focus token, its prompt shows until the next send.
    val request = props.jsonObject("focusRequest")
    val requestToken = (request?.get("id") as? JsonPrimitive)?.contentOrNull
    var requestedPlaceholder by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(requestToken) { request?.string("placeholder")?.takeIf { it.isNotEmpty() }?.let { requestedPlaceholder = it } }
    val editFocus = props.jsonObject("editFocus")
    val trimmed = text.trim()
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val send: () -> Unit = {
        if (trimmed.isNotEmpty()) {
            onEvent(sendEvent, eventEnvelope(props, almadarJson("message" to JsonPrimitive(trimmed))))
            text = ""
            requestedPlaceholder = null
        }
    }
    val contextBadge: @Composable () -> Unit = { if (context.isNotEmpty()) AlmadarBadge(context, variant = "primary", onEvent = onEvent) }
    val field: @Composable (Modifier) -> Unit = { fieldModifier ->
        Row(modifier = fieldModifier, verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(LocalAlmadarFieldFocusToken provides requestToken) {
            TextareaRenderer(
                almadarJson(
                    "value" to text, "placeholder" to (requestedPlaceholder ?: props.string("placeholder", "Ask the agent anything…")), "rows" to 1,
                    "onChange" to ALMADAR_LOCAL_ACTION, "local" to true, "sourceTrait" to almadarTrait(props)
                ),
                Modifier.weight(1f).onPreviewKeyEvent { key ->
                    val enter = key.type == KeyEventType.KeyDown && key.key == Key.Enter && !key.isShiftPressed
                    if (enter && trimmed.isNotEmpty()) { send(); true } else false
                }
            ) { _, envelope -> text = envelope?.jsonObject("actionPayload")?.string("value", text) ?: text }
            }
            when (status) {
                "error" -> AlmadarButton(props, "Retry", retryEvent, onEvent, variant = "secondary", icon = "refresh-cw", size = "sm")
                "running" -> {
                    AlmadarButton(props, "", pauseEvent, onEvent, variant = "ghost", icon = "pause", size = "sm", ariaLabel = "Pause the agent — you can edit the canvas while it waits")
                    AlmadarButton(props, "", stopEvent, onEvent, variant = "ghost", icon = "square", size = "sm", ariaLabel = "Cancel generation")
                }
                "paused" -> AlmadarButton(props, "Resume", resumeEvent, onEvent, icon = "play", size = "sm", ariaLabel = "Resume the agent — your canvas edits are kept")
                else -> AlmadarButton(props, "", sendEvent, { _, _ -> send() }, icon = "send", size = "sm", disabled = trimmed.isEmpty(), ariaLabel = "Send message")
            }
        }
    }
    val cluster: @Composable () -> Unit = {
        if (gate.isNotEmpty() || jepa != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                if (running) {
                    val indicator = (props as? JsonObject)?.get("runningIndicator")
                    if (indicator != null) AlmadarNode(indicator, onEvent) else AlmadarSpinner(size = "sm", label = "Working")
                }
                if (gate.isNotEmpty()) AlmadarText(gate, variant = if (running) "body1" else "caption", color = if (running) "primary" else "muted", weight = if (running) "medium" else "none", truncate = true)
                if (jepa != null) AlmadarBadge("%.1f%%".format(jepa * 100), variant = if (jepa >= 0.9) "success" else if (jepa >= 0.5) "warning" else "danger", onEvent = onEvent)
            }
        }
    }
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxWidth().background(palette.card).almadarLabel(props.string("aria-label"))) {
        val compact = maxWidth < AlmadarBreakpoints.md
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(geometry.borderStandard).background(palette.border))
            // The picked canvas element a sent message edits (`editFocus`); the ✕ asks the owner to clear it.
            if (editFocus != null && editFocus.string("label").isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = almadarSpace("space4"), end = almadarSpace("space4"), top = almadarSpace("space2")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlmadarGlyph("mouse-pointer-2", size = "xs")
                    AlmadarText("Editing: " + editFocus.string("label"), variant = "caption", truncate = true)
                    AlmadarButton(props, "", CHAT_ELEMENT_SELECTED_EVENT, { _, _ -> onEvent(CHAT_ELEMENT_SELECTED_EVENT, eventEnvelope(props, almadarJson("focus" to JsonNull))) }, variant = "ghost", icon = "x", size = "sm", ariaLabel = "Clear selection")
                }
            }
            if (compact) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpace("space4"), vertical = almadarSpace("space2")),
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                        contextBadge()
                        field(Modifier.weight(1f))
                    }
                    cluster()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpace("space4"), vertical = almadarSpace("space2")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    contextBadge()
                    field(Modifier.weight(1f))
                    cluster()
                }
            }
        }
    }
}
