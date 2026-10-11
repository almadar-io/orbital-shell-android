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

// tier: molecules — a grouped container styled by tokens only

import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role

@Composable
fun ImportSourcePickerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val context = LocalContext.current
    val shape = almadarShape(AlmadarCorner.Interactive)
    val sources = props.array("sources").mapNotNull { it as? JsonObject }
    val filesEvent = props.string("onFilesSelected")
    val eventOf: (JsonObject) -> String = { source -> props.string(if (source.string("kind") == "file") "onFilesSelected" else "onSelect") }
    val deliver: (List<Uri>) -> Unit = { uris ->
        val resolver = context.contentResolver
        val files = uris.mapNotNull { uri ->
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@mapNotNull null
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: (uri.lastPathSegment ?: "")
            almadarJson("name" to name, "size" to bytes.size, "type" to (resolver.getType(uri) ?: ""), "text" to bytes.decodeToString())
        }
        if (files.isNotEmpty()) onEvent(filesEvent, eventEnvelope(props, JsonObject(mapOf("files" to JsonArray(files)))))
    }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) deliver(listOf(uri)) }
    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> deliver(uris) }
    // The web `accept` list (`.md,text/markdown`): an extension or a MIME type per entry.
    val mimeTypes: (String) -> Array<String> = { accept ->
        val types = accept.split(",").map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { entry ->
            if (entry.startsWith(".")) MimeTypeMap.getSingleton().getMimeTypeFromExtension(entry.drop(1)) else entry
        }
        if (types.isEmpty()) arrayOf("*/*") else types.toTypedArray()
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(almadarSpace("space2"))
    ) {
        if (props.string("title").isNotEmpty()) AlmadarText(props.string("title"), variant = "h4")
        sources.forEach { source ->
            val disabled = source.bool("disabled")
            val event = eventOf(source)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (disabled) 0.5f else 1f)
                    .clip(shape)
                    .background(palette.card, shape)
                    .then(almadarBorder(AlmadarCorner.Interactive, geometry.borderStandard, palette.border))
                    .clickable(enabled = !disabled && event.isNotEmpty(), role = Role.Button) {
                        if (source.string("kind") == "file") {
                            val types = mimeTypes(source.string("accept"))
                            if (source.bool("multiple")) pickMany.launch(types) else pickOne.launch(types)
                        } else {
                            onEvent(event, eventEnvelope(props, almadarJson("sourceId" to source.string("id"))))
                        }
                    }
                    .almadarActionTag(event, props)
                    .padding(horizontal = almadarSpace("space4"), vertical = almadarSpace("space3")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpace("space3")),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (source.string("icon").isNotEmpty()) AlmadarGlyph(source.string("icon"))
                Column {
                    AlmadarText(source.string("label"), variant = "label")
                    if (source.string("description").isNotEmpty()) AlmadarText(source.string("description"), variant = "caption", color = "muted")
                }
            }
        }
        (props as? JsonObject)?.get("moreSources")?.takeIf { (it is JsonPrimitive && it.content.isNotEmpty()) || (it is JsonArray && it.isNotEmpty()) || it is JsonObject }?.let { more ->
            if (more is JsonArray) more.forEach { RenderChild(it, Modifier, onEvent) } else RenderChild(more, Modifier, onEvent)
        }
    }
}
