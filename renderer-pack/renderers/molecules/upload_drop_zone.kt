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

import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UploadDropZoneRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf("") }
    val accept = props.string("accept")
    val maxSize = props.string("maxSize").toDoubleOrNull()?.toLong() ?: 0L
    val maxFiles = props.int("maxFiles", 1)
    val readAs = props.string("readAs", "dataUrl")
    val disabled = props.bool("disabled")
    val action = props.string("action")
    val label = props.string("label", "Drop files here or click to browse")
    val sizeText: (Long) -> String = { bytes ->
        if (bytes < 1024L) bytes.toString() + "B" else if (bytes < 1024L * 1024L) Math.round(bytes / 1024.0).toString() + "KB" else Math.round(bytes / (1024.0 * 1024.0)).toString() + "MB"
    }
    val defaultDescription = listOfNotNull(
        if (accept.isNotEmpty()) "Accepted: " + accept else null,
        if (maxSize > 0L) "Max size: " + sizeText(maxSize) else null,
        if (maxFiles > 1) "Up to " + maxFiles + " files" else null
    ).joinToString(". ")
    val accepted = accept.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val mimes = accepted.filter { it.contains("/") }.ifEmpty { listOf("*/*") }.toTypedArray()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        class Picked(val uri: android.net.Uri, val name: String, val size: Long, val type: String)
        val picked = uris.map { uri ->
            var name = ""
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameAt = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeAt = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameAt >= 0) name = cursor.getString(nameAt) ?: ""
                    if (sizeAt >= 0) size = cursor.getLong(sizeAt)
                }
            }
            Picked(uri, name, size, context.contentResolver.getType(uri) ?: "")
        }
        val invalid = picked.firstOrNull { file ->
            accepted.isNotEmpty() && accepted.none { type -> if (type.endsWith("/*")) file.type.startsWith(type.removeSuffix("*")) else file.type == type || file.name.endsWith(type) }
        }
        val tooLarge = picked.firstOrNull { maxSize > 0L && it.size > maxSize }
        error = if (picked.size > maxFiles) "Maximum " + maxFiles + " files allowed"
            else if (invalid != null) "Invalid file type: " + invalid.name
            else if (tooLarge != null) "File too large: " + tooLarge.name + " (max " + sizeText(maxSize) + ")"
            else ""
        if (error.isEmpty() && picked.isNotEmpty() && action.isNotEmpty()) {
            scope.launch {
                val files = try {
                    withContext(Dispatchers.IO) {
                        picked.map { file ->
                            val bytes = context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() } ?: ByteArray(0)
                            val content = if (readAs == "text") String(bytes, Charsets.UTF_8)
                                else "data:" + file.type + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                            almadarJson("name" to file.name, "size" to file.size, "type" to file.type, "content" to content)
                        }
                    }
                } catch (failure: java.io.IOException) {
                    null
                }
                if (files == null) {
                    error = "Could not read the selected file"
                } else {
                    val declared = props.jsonObject("actionPayload")
                    val bare = JsonObject(((props as? JsonObject) ?: JsonObject(emptyMap())) - "actionPayload")
                    onEvent(action, eventEnvelope(bare, JsonObject((declared ?: JsonObject(emptyMap())) + mapOf("files" to JsonArray(files)))))
                }
            }
        }
    }
    val borderWidth = LocalAlmadarGeometry.current.borderHeavy
    val outline = if (error.isNotEmpty()) palette.error else palette.border
    val shape = MaterialTheme.shapes.small
    Column(
        modifier = modifier.fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .drawBehind {
                val stroke = borderWidth.toPx()
                drawRoundRect(
                    color = outline,
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 4f, stroke * 3f)))
                )
            }
            .clickable(enabled = !disabled, role = Role.Button) { launcher.launch(mimes) }
            .almadarTag(almadarActionId(action, props))
            .almadarAlpha(if (disabled) ALMADAR_DISABLED_ALPHA else 1f)
            .almadarLabel(props.string("aria-label", label))
            .padding(almadarSpaceFor("xl")),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))
    ) {
        AlmadarGlyph(if (error.isNotEmpty()) "file-warning" else props.string("icon", "upload"), color = if (error.isNotEmpty()) "error" else "muted", size = "lg")
        AlmadarText(label, variant = "body1", weight = "medium", align = "center")
        if (error.isNotEmpty()) AlmadarText(error, variant = "caption", color = "error", align = "center")
        else AlmadarText(props.string("description", defaultDescription), variant = "caption", color = "muted", align = "center")
    }
}
