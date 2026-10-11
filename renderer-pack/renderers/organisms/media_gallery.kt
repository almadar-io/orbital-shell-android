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
import androidx.compose.ui.graphics.Color

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MediaGalleryRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val root = props as? JsonObject
    val srcField = props.string("srcField", "src")
    val captionField = props.string("captionField", "caption")
    val selectable = props.bool("selectable")
    val selected = almadarStrings(root?.get("selectedItems")).toSet()
    val selectionEvent = props.string("selectionEvent", "SELECTION_CHANGE")
    val aspect = when (props.string("aspectRatio", "square")) { "landscape" -> 16f / 9f; "portrait" -> 3f / 4f; else -> 1f }
    val width = LocalConfiguration.current.screenWidthDp
    val requested = props.int("columns", 3)
    val columns = when {
        requested >= 6 -> if (width.dp >= AlmadarBreakpoints.lg) 6 else if (width.dp >= AlmadarBreakpoints.sm) 4 else 3
        requested == 5 -> if (width.dp >= AlmadarBreakpoints.lg) 5 else if (width.dp >= AlmadarBreakpoints.sm) 3 else 2
        requested == 4 -> if (width.dp >= AlmadarBreakpoints.lg) 4 else if (width.dp >= AlmadarBreakpoints.sm) 3 else 2
        requested == 3 -> if (width.dp >= AlmadarBreakpoints.sm) 3 else 2
        else -> 2
    }
    val items: List<JsonObject> = props.array("items").mapNotNull { it as? JsonObject }.ifEmpty {
        almadarRows(root?.get("entity")).mapIndexed { index, record ->
            almadarJson(
                "id" to (almadarScalar(record["id"]) ?: index.toString()),
                "src" to formatFieldValue(almadarNested(record, srcField), ""),
                "alt" to formatFieldValue(record["alt"], ""),
                "thumbnail" to formatFieldValue(record["thumbnail"], ""),
                "caption" to formatFieldValue(almadarNested(record, captionField), "")
            )
        }
    }
    var lightbox by remember { mutableStateOf<Int?>(null) }
    val actions = props.array("actions").mapNotNull { it as? JsonObject }.filter { almadarActionEvent(it).isNotEmpty() }
    val title = props.string("title")
    val upload = props.bool("showUpload")
    if (props.bool("isLoading")) {
        LoadingStateRenderer(almadarJson("message" to "Loading..."), modifier, AlmadarNoEvent)
    } else if (props.jsonObject("error") != null) {
        ErrorStateRenderer(almadarJson("title" to "Gallery error", "message" to (props.jsonObject("error")?.string("message") ?: "")), modifier, AlmadarNoEvent)
    } else if (items.isEmpty() && !upload) {
        EmptyStateRenderer(almadarJson("icon" to "image", "title" to "No media", "description" to "No media items to display."), modifier, AlmadarNoEvent)
    } else {
        Column(
            modifier = modifier.fillMaxWidth().padding(almadarSpaceFor("lg")).almadarLabel(props.string("aria-label")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
        ) {
            if (title.isNotEmpty() || upload || actions.isNotEmpty()) {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    if (title.isNotEmpty()) AlmadarText(title, variant = "h5")
                    Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                        if (upload) AlmadarButton(props, "Upload", MEDIA_UPLOAD_EVENT, onEvent, variant = "secondary", size = "sm", icon = "upload")
                        actions.forEach { action ->
                            AlmadarButton(
                                props, action.string("label"), almadarActionEvent(action), onEvent,
                                payload = almadarActionPayload(action, null), variant = "ghost", size = "sm"
                            )
                        }
                    }
                }
            }
            if (selectable && selected.isNotEmpty()) AlmadarBadge(selected.size.toString() + " selected", variant = "info")
            Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                items.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                        rowItems.forEachIndexed { offset, item ->
                            val index = rowIndex * columns + offset
                            val id = item.string("id")
                            val chosen = selected.contains(id)
                            val caption = item.string("caption")
                            val pick: () -> Unit = {
                                if (selectable) {
                                    val next = if (chosen) selected - id else selected + id
                                    onEvent(selectionEvent, eventEnvelope(props, almadarJson("selection" to JsonArray(next.map { JsonPrimitive(it) }))))
                                } else {
                                    lightbox = index
                                }
                                onEvent(MEDIA_SELECT_EVENT, eventEnvelope(props, almadarJson("row" to item)))
                            }
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(aspect)
                                    .clip(MaterialTheme.shapes.medium)
                                    .border(LocalAlmadarGeometry.current.borderHeavy, if (chosen) palette.primary else Color.Transparent, MaterialTheme.shapes.medium)
                                    .clickable(role = Role.Button) { pick() }
                                    .then(if (selectable) Modifier.almadarActionTag(selectionEvent, props) else Modifier.verificationContainer(VerificationIds.row(id)))
                                    .semantics { this.selected = chosen }
                                    .almadarLabel(item.string("alt", caption.ifEmpty { id }))
                            ) {
                                ImageRenderer(
                                    almadarJson("src" to item.string("thumbnail", item.string("src")), "alt" to item.string("alt", caption), "sourceTrait" to almadarTrait(props)),
                                    Modifier.fillMaxSize(), AlmadarNoEvent
                                )
                                if (caption.isNotEmpty()) {
                                    Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = palette.scrim, contentColor = palette.background) {
                                        AlmadarText(caption, modifier = Modifier.padding(almadarSpaceFor("sm")), variant = "caption", color = "inherit", truncate = true)
                                    }
                                }
                                if (selectable && chosen) {
                                    Box(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(almadarSpaceFor("sm")).background(palette.primary, MaterialTheme.shapes.large).padding(almadarSpaceFor("xs"))
                                    ) { AlmadarGlyph("check", color = "inherit", size = "xs") }
                                }
                            }
                        }
                        repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
            val open = lightbox
            if (open != null) {
                LightboxRenderer(
                    almadarJson(
                        "images" to JsonArray(items.map { almadarJson("src" to it.string("src"), "alt" to it.string("alt", it.string("caption")), "caption" to it.string("caption")) }),
                        "currentIndex" to open, "isOpen" to true, "closeAction" to "LIGHTBOX_CLOSE", "local" to true
                    ),
                    Modifier.fillMaxWidth()
                ) { _, _ -> lightbox = null }
            }
        }
    }
}
