package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: molecules — a grouped container styled by tokens only

private const val SECTION_ADDED_EVENT: String = "SECTION_ADDED"
private const val SECTION_REMOVED_EVENT: String = "SECTION_REMOVED"

@Composable
fun RepeatableFormSectionRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val sectionType = props.string("sectionType")
    val items = props.array("items").mapNotNull { it as? JsonObject }
    val minItems = props.int("minItems", 0)
    val maxItems = props.int("maxItems", Int.MAX_VALUE)
    val readOnly = props.bool("readOnly")
    val canAdd = !readOnly && items.size < maxItems
    val canRemove = !readOnly && items.size > minItems
    val canReorder = props.bool("allowReorder") && !readOnly
    val track = props.bool("trackAddedInState") && props.string("currentState").isNotEmpty()
    val template = (props as? JsonObject)?.get("renderItem") as? JsonObject
    val density = LocalAlmadarDensity.current
    var order by remember(items) { mutableStateOf(items) }
    val addButton: @Composable (String) -> Unit = { variant ->
        AlmadarButton(
            props, props.string("addLabel", "Add"), SECTION_ADDED_EVENT,
            { name, _ ->
                onEvent(
                    name,
                    eventEnvelope(
                        props,
                        almadarJson(
                            "sectionType" to sectionType, "index" to items.size,
                            "addedInState" to (if (track) props.string("currentState") else ""),
                            "addedAt" to (if (track) java.time.Instant.now().toString() else "")
                        )
                    )
                )
            },
            variant = variant, icon = "plus", size = "sm"
        )
    }
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarText(props.string("title"), variant = "h4")
                AlmadarText("(" + items.size + (if (maxItems != Int.MAX_VALUE) "/" + maxItems else "") + ")", variant = "caption", color = "muted")
            }
            if (canAdd) addButton("secondary")
        }
        if (order.isEmpty()) {
            AlmadarPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(density.cardPaddingLg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
                ) {
                    AlmadarText(props.string("emptyMessage", "No items added yet"), color = "muted")
                    if (canAdd) addButton("primary")
                }
            }
        } else {
            order.forEachIndexed { index, item ->
                val id = item.string("id", index.toString())
                AlmadarPanel(modifier = Modifier.fillMaxWidth().verificationContainer(VerificationIds.row(id))) {
                    Column(modifier = Modifier.padding(density.cardPaddingMd), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                        if (props.bool("showAuditInfo") && (item.string("addedInState").isNotEmpty() || item.string("addedAt").isNotEmpty())) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (item.string("addedInState").isNotEmpty()) AlmadarText("Added in: " + item.string("addedInState"), variant = "caption", color = "muted")
                                if (item.string("addedAt").isNotEmpty()) AlmadarText(item.string("addedAt"), variant = "caption", color = "muted")
                            }
                            AlmadarDivider()
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.Top) {
                            if (canReorder) {
                                Column {
                                    AlmadarLocalButton(
                                        props, "", { val next = order.toMutableList(); next.add(index - 1, next.removeAt(index)); order = next },
                                        variant = "ghost", size = "sm", icon = "chevron-up", disabled = index == 0, ariaLabel = "Move up"
                                    )
                                    AlmadarLocalButton(
                                        props, "", { val next = order.toMutableList(); next.add(index + 1, next.removeAt(index)); order = next },
                                        variant = "ghost", size = "sm", icon = "chevron-down", disabled = index == order.lastIndex, ariaLabel = "Move down"
                                    )
                                }
                            }
                            if (template != null) {
                                RenderChild(template, Modifier.weight(1f), onEvent)
                            } else {
                                AlmadarRecordFields(
                                    almadarFields(item.entries.filter { it.key != "id" && it.value is JsonPrimitive }.map { JsonPrimitive(it.key) }),
                                    item, Modifier.weight(1f)
                                )
                            }
                            if (canRemove) {
                                AlmadarButton(
                                    props, "", SECTION_REMOVED_EVENT,
                                    { name, _ -> onEvent(name, eventEnvelope(props, almadarJson("sectionType" to sectionType, "index" to index, "itemId" to id))) },
                                    variant = "ghost", size = "sm", icon = "trash-2", ariaLabel = "Remove"
                                )
                            }
                        }
                    }
                }
            }
        }
        if (items.size < minItems) {
            AlmadarText(
                if (minItems == 1) "At least 1 item required" else "At least " + minItems + " items required",
                variant = "caption", color = "warning"
            )
        }
    }
}
