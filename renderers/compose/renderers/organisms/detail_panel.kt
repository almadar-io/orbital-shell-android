package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.ui.graphics.RectangleShape

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

@Composable
fun DetailPanelRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val entityNode = (props as? JsonObject)?.get("entity")
    val data: JsonObject = (if (entityNode is JsonArray) entityNode.firstOrNull() as? JsonObject else entityNode as? JsonObject)
        ?: props.jsonObject("initialData") ?: JsonObject(emptyMap())
    val rawFields = props.array("fields")
    val detailFields = rawFields.mapNotNull { it as? JsonObject }.filter { it["value"] != null && it.string("name").isEmpty() && it.string("key").isEmpty() }
    val fields = almadarFields(rawFields.filter { it !in detailFields } + props.array("fieldNames"))
    val sections = props.array("sections").mapNotNull { it as? JsonObject }
    val actions = props.array("actions").mapNotNull { it as? JsonObject }
    val backAction = props.jsonObject("backAction")
    val status = props.jsonObject("status")
    val closeEvent = props.string("closeEvent")
    val maxInline = props.int("maxInlineActions", 2)
    val titleField = fields.firstOrNull { it.variant == "h3" || it.variant == "h4" }
    val title = props.string("title", titleField?.let { formatFieldValue(almadarNested(data, it.path), it.format) } ?: "")
    val subtitle = props.string("subtitle")
    val rowPayload = almadarJson("id" to data["id"], "row" to data)
    val closeAction = if (closeEvent.isEmpty()) null else actions.firstOrNull { it.string("event") == closeEvent }
    val others = actions.filter { it !== closeAction }
    fun inlineOf(action: JsonObject): JsonObject = almadarJson(
        "label" to action.string("label"), "icon" to action.string("icon"), "event" to action.string("event"),
        "navigatesTo" to action.string("navigatesTo"), "variant" to action.string("variant", "secondary")
    )
    val statusBadges: @Composable () -> Unit = {
        if (status != null) AlmadarBadge(status.string("label"), variant = status.string("variant", "default"), size = "md")
        fields.filter { it.variant == "badge" }.forEach { field ->
            val text = formatFieldValue(almadarNested(data, field.path), field.format)
            if (text.isNotEmpty()) AlmadarBadge(field.labels?.string(text, text) ?: text, variant = almadarBadgeVariant(field, text), size = "md")
        }
    }
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "detail", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")))
    } else {
        val panel: @Composable (Modifier) -> Unit = { panelModifier ->
        LazyColumn(
            modifier = panelModifier,
            contentPadding = PaddingValues(LocalAlmadarDensity.current.dialogPadding),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.Top) {
                    if (backAction != null) {
                        AlmadarButton(
                            props, backAction.string("label"), backAction.string("event", "back"), onEvent,
                            payload = rowPayload, variant = backAction.string("variant", "ghost"),
                            icon = backAction.string("icon", "arrow-left"), size = "sm", ariaLabel = "Back"
                        )
                    }
                    val avatarName = props.string("avatarField").let { if (it.isEmpty()) "" else formatFieldValue(almadarNested(data, it), "") }
                    if (avatarName.isNotEmpty()) AvatarRenderer(almadarJson("name" to avatarName, "size" to "lg"), Modifier, onEvent)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                            if (title.isNotEmpty()) AlmadarText(title, variant = "h3", modifier = Modifier.weight(1f, fill = false))
                            statusBadges()
                        }
                        if (subtitle.isNotEmpty()) AlmadarText(subtitle, variant = "body1", color = "secondary")
                    }
                    if (props.bool("showActions", true) && (others.isNotEmpty() || closeAction != null)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                            AlmadarRowActions(props, others.take(maxInline).map { inlineOf(it) }, others.drop(maxInline), data, onEvent)
                            if (closeAction != null) {
                                AlmadarButton(props, "", closeEvent, onEvent, payload = rowPayload, variant = "ghost", icon = "x", size = "sm", ariaLabel = "Close panel")
                            }
                        }
                    }
                }
            }
            if (sections.isNotEmpty()) {
                sections.forEach { section ->
                    val sectionFields = section.array("fields")
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                            if (sections.size > 1) AlmadarText(section.string("title"), variant = "h5")
                            sectionFields.forEach { entry ->
                                val named = entry as? JsonObject
                                val label = if (named != null) named.string("label") else almadarFieldLabel((entry as? JsonPrimitive)?.contentOrNull ?: "")
                                val value = if (named != null) formatFieldValue(named["value"], "") else formatFieldValue(almadarNested(data, (entry as? JsonPrimitive)?.contentOrNull ?: ""), "")
                                Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                    AlmadarText(label, variant = "caption", color = "muted", weight = "medium")
                                    AlmadarText(value.ifEmpty { "—" })
                                }
                            }
                        }
                    }
                }
            } else {
                items(detailFields.size) { index ->
                    val entry = detailFields[index]
                    Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        AlmadarText(entry.string("label"), variant = "caption", color = "muted", weight = "medium")
                        AlmadarText(formatFieldValue(entry["value"], "").ifEmpty { "—" })
                    }
                }
                items(fields.filter { it.variant != "badge" && it !== titleField }.size) { index ->
                    val field = fields.filter { it.variant != "badge" && it !== titleField }[index]
                    val value = almadarNested(data, field.path)
                    val text = formatFieldValue(value, field.format)
                    Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        AlmadarText(field.label, variant = "caption", color = "muted", weight = "medium")
                        if (field.variant == "progress") {
                            val number = (value as? JsonPrimitive)?.floatOrNull
                            if (number != null) AlmadarProgress(number)
                        } else {
                            AlmadarText(text.ifEmpty { "—" })
                        }
                    }
                }
            }
        }
        }
        if (props.bool("slideOver")) {
            Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                panel(Modifier.widthIn(max = AlmadarSizes.maxW2xl).fillMaxWidth().almadarMaterialFill(RectangleShape, LocalAlmadarTheme.current.color.card))
            }
        } else {
            panel(modifier.fillMaxWidth().almadarFlexFill())
        }
    }
}
