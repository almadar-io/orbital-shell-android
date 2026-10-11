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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PaginationRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val current = props.int("currentPage", 1)
    val total = props.int("totalPages", 1)
    val pageEvent = props.string("pageChangeEvent")
    val sizeEvent = props.string("pageSizeChangeEvent")
    val maxVisible = props.int("maxVisiblePages", 7)
    val pageSize = props.int("pageSize", 0)
    val sizeOptions = props.array("pageSizeOptions").mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() }.ifEmpty { listOf(10, 20, 50, 100) }
    val showJump = props.bool("showJumpToPage")
    var jump by remember { mutableStateOf("") }
    val half = maxVisible / 2
    val pages: List<Int?> = if (total <= maxVisible) {
        (1..total).toList()
    } else if (current <= half + 1) {
        (1..(maxVisible - 2)).toList() + listOf(null, total)
    } else if (current >= total - half) {
        listOf(1, null) + ((total - maxVisible + 3)..total).toList()
    } else {
        listOf(1, null) + ((current - half + 2)..(current + half - 2)).toList() + listOf(null, total)
    }
    val goTo: (Int) -> JsonElement = { page -> almadarJson("page" to page) }
    AlmadarBoxWithConstraints(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", "Pagination"))) {
        val roomy = maxWidth >= AlmadarBreakpoints.md
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"), Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
        ) {
            if (props.bool("showTotal") && (props as? JsonObject)?.get("totalItems") != null) {
                AlmadarText("Total: " + props.int("totalItems"), variant = "small", color = "secondary")
            }
            if (props.bool("showPageSize") && pageSize > 0 && sizeEvent.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                    AlmadarText("Show:", variant = "small", color = "secondary")
                    SelectRenderer(
                        almadarJson(
                            "name" to "page-size", "value" to pageSize.toString(), "aria-label" to "Page size",
                            "options" to JsonArray(sizeOptions.map { almadarJson("value" to it.toString(), "label" to it.toString()) }),
                            "onValueChange" to sizeEvent, "sourceTrait" to almadarTrait(props)
                        ),
                        Modifier.widthIn(min = almadarSpaceFor("2xl") * 2)
                    ) { name, envelope ->
                        val next = envelope?.jsonObject("actionPayload")?.string("value")?.toIntOrNull()
                        if (next != null) onEvent(name, eventEnvelope(props, almadarJson("pageSize" to next)))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarButton(
                    props, if (roomy) "Previous" else "", pageEvent, onEvent,
                    payload = goTo(current - 1), variant = "secondary", icon = "chevron-left", size = "sm",
                    disabled = current <= 1, ariaLabel = "Previous"
                )
                if (roomy) {
                    pages.forEach { page ->
                        if (page == null) {
                            AlmadarText("…", modifier = Modifier.padding(horizontal = almadarSpaceFor("sm")), variant = "small", color = "muted")
                        } else {
                            AlmadarButton(
                                props, page.toString(), pageEvent, onEvent,
                                payload = goTo(page), variant = if (page == current) "primary" else "ghost", size = "sm"
                            )
                        }
                    }
                } else {
                    AlmadarText(current.toString() + " / " + total, modifier = Modifier.padding(horizontal = almadarSpaceFor("sm")), variant = "small", color = "secondary")
                }
                AlmadarButton(
                    props, if (roomy) "Next" else "", pageEvent, onEvent,
                    payload = goTo(current + 1), variant = "secondary", icon = "chevron-right", size = "sm",
                    disabled = current >= total, ariaLabel = "Next"
                )
            }
            if (showJump) {
                Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                    AlmadarText("Go to:", variant = "small", color = "secondary")
                    AlmadarInput(
                        name = "page-jump", value = jump, onChange = { jump = it },
                        modifier = Modifier.widthIn(min = almadarSpaceFor("2xl") * 2, max = almadarSpaceFor("2xl") * 2),
                        placeholder = "Page", inputType = "number"
                    )
                    AlmadarButton(
                        props, "Go", pageEvent,
                        { name, envelope ->
                            jump = ""
                            onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload")))
                        },
                        payload = goTo(jump.toIntOrNull() ?: 0), variant = "ghost", size = "sm",
                        disabled = (jump.toIntOrNull() ?: 0) !in 1..total
                    )
                }
            }
        }
    }
}
