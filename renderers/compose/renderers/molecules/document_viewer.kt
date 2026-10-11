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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// tier: molecules — a grouped container styled by tokens only

private const val DOCUMENT_PAGE_EVENT: String = "DOCUMENT_PAGE_CHANGE"
private const val DOCUMENT_DOWNLOAD_EVENT: String = "DOCUMENT_DOWNLOAD"
private const val DOCUMENT_PRINT_EVENT: String = "DOCUMENT_PRINT"

@Composable
fun DocumentViewerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val documents = props.array("documents").mapNotNull { it as? JsonObject }
    val totalPages = props.int("totalPages", 0)
    val title = props.string("title")
    val actions = props.array("actions").mapNotNull { it as? JsonObject }
    var zoom by remember { mutableStateOf(100) }
    var page by remember(props.int("currentPage", 1)) { mutableStateOf(props.int("currentPage", 1)) }
    var active by remember { mutableStateOf(0) }
    val doc = documents.getOrNull(active)
    val src = doc?.string("src").orEmpty().ifEmpty { props.string("src") }
    val content = doc?.string("content").orEmpty().ifEmpty { props.string("content") }
    val kind = doc?.string("documentType").orEmpty().ifEmpty { props.string("documentType", "pdf") }
    if (props.bool("isLoading")) {
        LoadingStateRenderer(almadarJson("message" to "Loading..."), modifier, AlmadarNoEvent)
    } else if (props.jsonObject("error") != null) {
        ErrorStateRenderer(
            almadarJson("title" to "Document error", "message" to props.jsonObject("error")?.string("message")),
            modifier, AlmadarNoEvent
        )
    } else if (src.isEmpty() && content.isEmpty()) {
        EmptyStateRenderer(
            almadarJson("icon" to "file-text", "title" to "No document", "description" to "No document to display.", "sourceTrait" to almadarTrait(props)),
            modifier, onEvent
        )
    } else {
        AlmadarPanel(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
            if (documents.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(almadarSpaceFor("xs")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
                ) {
                    documents.forEachIndexed { index, item ->
                        AlmadarLocalButton(props, item.string("label"), { active = index }, variant = if (index == active) "secondary" else "ghost", size = "sm")
                    }
                }
                AlmadarDivider()
            }
            if (props.bool("showToolbar", true)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(palette.muted).padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("xs")),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                        AlmadarGlyph("file-text", color = "muted", size = "sm")
                        if (title.isNotEmpty()) AlmadarText(title, variant = "small", weight = "medium", truncate = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                        AlmadarLocalButton(props, "", { zoom = maxOf(zoom - 25, 50) }, variant = "ghost", icon = "zoom-out", size = "sm", ariaLabel = "Zoom out")
                        AlmadarText(zoom.toString() + "%", variant = "caption", color = "secondary")
                        AlmadarLocalButton(props, "", { zoom = minOf(zoom + 25, 200) }, variant = "ghost", icon = "zoom-in", size = "sm", ariaLabel = "Zoom in")
                        if (totalPages > 1) {
                            AlmadarButton(
                                props, "", DOCUMENT_PAGE_EVENT, { name, envelope -> page = maxOf(page - 1, 1); onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload"))) },
                                payload = almadarJson("page" to page - 1), variant = "ghost", icon = "chevron-left", size = "sm",
                                disabled = page <= 1, ariaLabel = "Previous page"
                            )
                            AlmadarText(page.toString() + " / " + totalPages, variant = "caption", color = "secondary")
                            AlmadarButton(
                                props, "", DOCUMENT_PAGE_EVENT, { name, envelope -> page = minOf(page + 1, totalPages); onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload"))) },
                                payload = almadarJson("page" to page + 1), variant = "ghost", icon = "chevron-right", size = "sm",
                                disabled = page >= totalPages, ariaLabel = "Next page"
                            )
                        }
                        if (props.bool("showDownload") && src.isNotEmpty()) {
                            AlmadarButton(
                                props, "", DOCUMENT_DOWNLOAD_EVENT, { name, envelope -> onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload"))); uriHandler.openUri(src) },
                                payload = almadarJson("src" to src), variant = "ghost", icon = "download", size = "sm", ariaLabel = "Download document"
                            )
                        }
                        if (props.bool("showPrint")) {
                            AlmadarButton(props, "", DOCUMENT_PRINT_EVENT, onEvent, variant = "ghost", icon = "printer", size = "sm", ariaLabel = "Print document")
                        }
                        actions.forEach { action ->
                            val event = action.string("event")
                            Box(
                                modifier = Modifier
                                    .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                                        if (event.isNotEmpty()) onEvent(event, eventEnvelope(props, almadarJson("page" to page)))
                                    }
                                    .almadarActionTag(event, props)
                            ) { AlmadarBadge(action.string("label")) }
                        }
                    }
                }
                AlmadarDivider()
            }
            val factor = zoom / 100f
            if (kind == "pdf" && src.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("lg")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                    AlmadarText(title.ifEmpty { src }, variant = "body2", color = "muted")
                    AlmadarLocalButton(props, "Open document", { uriHandler.openUri(src) }, variant = "secondary", icon = "external-link")
                }
            } else if (kind == "markdown") {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(almadarSpaceFor("md"))) {
                    MarkdownContentRenderer(almadarJson("content" to content, "sourceTrait" to almadarTrait(props)), Modifier.fillMaxWidth(), onEvent)
                }
            } else {
                val shown = if (kind == "html") android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_LEGACY).toString() else content
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(almadarSpaceFor("md"))) {
                    AlmadarMonoText(shown, size = if (factor > 1.5f) "lg" else if (factor > 1f) "base" else if (factor < 1f) "xs" else "sm")
                }
            }
        }
    }
}
