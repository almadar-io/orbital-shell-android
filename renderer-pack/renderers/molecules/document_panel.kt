package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.clickable

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.semantics.Role

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DocumentPanelRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val identity = almadarJson("id" to props.string("id"))
    val titleCommit = props.string("titleCommitEvent")
    val title = props.string("title")
    val editing = props.bool("editing")
    val value = props.string("value")
    val editEvent = props.string("editEvent")
    val doneEvent = props.string("doneEvent")
    val placeholder = props.string("placeholder", "Start writing…")
    var titleDraft by remember { mutableStateOf<String?>(null) }
    val commitTitle: (String) -> Unit = { draft ->
        titleDraft = null
        val next = draft.trim()
        if (titleCommit.isNotEmpty() && next.isNotEmpty() && next != title) {
            onEvent(titleCommit, eventEnvelope(props, almadarJson("title" to next, "id" to props.string("id"))))
        }
    }
    val actions = props.array("actions").mapNotNull { it as? JsonObject }.filter { it.string("event").isNotEmpty() }
    val cap = minOf(props.int("maxInlineActions", 1), 1)
    val inline = actions.filter { it.string("variant") == "primary" }.take(cap)
    val menu = actions.filter { it !in inline }
    val actionPayload = almadarJson("id" to props.string("id"), "title" to title)
    AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        if (props.string("coverImage").isNotEmpty()) {
            ImageRenderer(almadarJson("src" to props.string("coverImage"), "alt" to "", "aspect" to "21/9", "rounded" to "none"), Modifier.fillMaxWidth(), AlmadarNoEvent)
        }
        Column(modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("lg")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    if (props.string("icon").isNotEmpty()) AlmadarGlyph(props.string("icon"), color = "muted", size = "lg")
                    val draft = titleDraft
                    if (titleCommit.isNotEmpty() && draft != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                            AlmadarInput(
                                name = "document-title", value = draft, onChange = { titleDraft = it },
                                submitEvent = "document-title-commit", onSubmit = { commitTitle(it) }
                            )
                            AlmadarLocalButton(props, "", { commitTitle(draft) }, variant = "ghost", size = "sm", icon = "check", ariaLabel = "Save title")
                        }
                    } else if (titleCommit.isNotEmpty()) {
                        AlmadarText(
                            title.ifEmpty { "Untitled" },
                            modifier = Modifier.clickable(role = Role.Button) { titleDraft = title }.almadarTag(VerificationIds.field("document-title")),
                            variant = "h2", color = if (title.isEmpty()) "muted" else "primary"
                        )
                    } else {
                        AlmadarText(title.ifEmpty { "Untitled" }, variant = "h2")
                    }
                    if (props.string("subtitle").isNotEmpty()) AlmadarText(props.string("subtitle"), variant = "small", color = "secondary")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                    if (editing) {
                        AlmadarText(props.string("autosaveHint", "Saves as you type"), variant = "caption", color = "muted")
                        if (doneEvent.isNotEmpty()) {
                            AlmadarButton(props, props.string("doneLabel", "Done"), doneEvent, onEvent, payload = identity, variant = "primary", size = "sm")
                        }
                    } else {
                        if (editEvent.isNotEmpty()) {
                            AlmadarButton(props, props.string("editLabel", "Edit"), editEvent, onEvent, payload = identity, variant = "ghost", size = "sm", icon = "edit")
                        }
                        inline.forEach { action ->
                            AlmadarButton(
                                props, action.string("label"), action.string("event"), onEvent,
                                payload = actionPayload, variant = action.string("variant", "primary"), icon = action.string("icon"), size = "sm"
                            )
                        }
                        if (menu.isNotEmpty()) AlmadarOverflow(props, menu, actionPayload, onEvent)
                    }
                }
            }
            if (editing) {
                InputRenderer(
                    almadarJson(
                        "inputType" to "textarea", "name" to "document-body", "value" to value, "placeholder" to placeholder,
                        "onChange" to props.string("contentChangeEvent", "document-content-change"), "sourceTrait" to almadarTrait(props)
                    ),
                    Modifier.fillMaxWidth(), onEvent
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = LocalAlmadarDensity.current.rowHeightSpacious * 3)
                        .let { if (editEvent.isEmpty()) it else it.clickable(role = Role.Button) { onEvent(editEvent, eventEnvelope(props, identity)) } }
                ) {
                    if (value.isEmpty()) AlmadarText(placeholder, variant = "body1", color = "muted")
                    else MarkdownContentRenderer(almadarJson("content" to value), Modifier, AlmadarNoEvent)
                }
            }
        }
    }
}
