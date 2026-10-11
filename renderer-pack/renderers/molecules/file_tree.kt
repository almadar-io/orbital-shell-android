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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun FileTreeRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val tree = props.array("tree").mapNotNull { it as? JsonObject }
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val indent = props.float("indent", 16f).dp
    val look = props.string("look", "files")
    val known = items.map { it.string("id") }.toSet()
    val initialOpen = almadarStrings((props as? JsonObject)?.get("expandedPaths")).toSet() +
        tree.filter { it.string("type") == "dir" }.map { it.string("path") } +
        items.filter { it.string("parentId").isEmpty() || it.string("parentId") !in known }.map { it.string("id") }
    var open by remember { mutableStateOf(initialOpen) }
    var picked by remember { mutableStateOf<String?>(null) }
    class FileRow(val node: JsonObject, val depth: Int, val key: String, val flat: Boolean, val expandable: Boolean)
    val rows = mutableListOf<FileRow>()
    fun walk(node: JsonObject, depth: Int) {
        val children = node.array("children").mapNotNull { it as? JsonObject }
        val expandable = node.string("type") == "dir" || children.isNotEmpty() || node.bool("expandable")
        rows.add(FileRow(node, depth, node.string("path"), false, expandable))
        if (expandable && open.contains(node.string("path"))) children.forEach { walk(it, depth + 1) }
    }
    fun walkItem(node: JsonObject, depth: Int) {
        val children = items.filter { it.string("parentId") == node.string("id") }
        rows.add(FileRow(node, depth, node.string("id"), true, children.isNotEmpty()))
        if (children.isNotEmpty() && open.contains(node.string("id"))) children.forEach { walkItem(it, depth + 1) }
    }
    if (tree.isNotEmpty()) tree.forEach { walk(it, 0) }
    else items.filter { it.string("parentId").isEmpty() || it.string("parentId") !in known }.forEach { walkItem(it, 0) }
    val selectedKey = if (look == "files" || tree.isNotEmpty()) props.string("selectedPath").ifEmpty { picked ?: "" } else props.string("selectedId").ifEmpty { picked ?: "" }
    val glyph: (String) -> String = { name ->
        when (name.substringAfterLast('.', "").lowercase()) {
            "ts", "tsx", "js", "jsx", "py", "html" -> "file-code"
            "json", "orb" -> "file-json"
            "css", "scss", "md" -> "file-text"
            else -> "file"
        }
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("xs")).almadarLabel(props.string("aria-label")),
        verticalArrangement = Arrangement.Top
    ) {
        rows.forEach { row ->
            val node = row.node
            val directory = !row.flat && node.string("type") == "dir"
            val isOpen = open.contains(row.key)
            val chosen = selectedKey == row.key
            val nav = row.flat && look == "nav"
            val icon = if (row.flat) node.string("icon", if (nav) "file-text" else if (!row.expandable) "file" else if (isOpen) "folder-open" else "folder")
                else if (directory) (if (isOpen) "folder-open" else "folder") else glyph(node.string("name"))
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(if (chosen) palette.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable(role = Role.Button) {
                        if (row.flat) {
                            if (row.expandable) open = if (isOpen) open - row.key else open + row.key
                            picked = row.key
                            onEvent("nodeSelect", eventEnvelope(props, almadarJson("id" to row.key)))
                        } else if (directory) {
                            open = if (isOpen) open - row.key else open + row.key
                        } else {
                            picked = row.key
                            onEvent("fileSelect", eventEnvelope(props, almadarJson("path" to row.key, "name" to node.string("name"), "type" to node.string("type"))))
                        }
                    }
                    .verificationContainer(VerificationIds.row(row.key))
                    .semantics { selected = chosen }
                    .padding(start = indent * row.depth + almadarSpaceFor("sm"), top = almadarSpaceFor("xs"), end = almadarSpaceFor("sm"), bottom = almadarSpaceFor("xs")),
                horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (row.expandable) AlmadarGlyph(if (isOpen) "chevron-down" else "chevron-right", color = "muted", size = "xs")
                else Spacer(modifier = Modifier.width(almadarSpaceFor("md")))
                AlmadarGlyph(icon, color = if (directory) "warning" else "muted", size = "xs")
                if (row.flat) {
                    AlmadarText(node.string("label"), variant = "caption", truncate = true)
                } else {
                    AlmadarMonoText(node.string("name"), weight = if (chosen) FontWeight.SemiBold else null)
                    if (node.string("note").isNotEmpty()) AlmadarText(node.string("note"), variant = "caption", color = "muted")
                    if (!directory && node["size"] != null) {
                        Spacer(modifier = Modifier.weight(1f))
                        val bytes = node.int("size")
                        AlmadarText(if (bytes < 1024) bytes.toString() + "B" else Math.round(bytes / 1024.0).toString() + "KB", variant = "caption", color = "muted")
                    }
                }
            }
        }
    }
}
