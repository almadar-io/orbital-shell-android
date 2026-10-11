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

@Composable
fun WizardContainerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val steps = props.array("steps").mapNotNull { it as? JsonObject }
    val nextEvent = props.string("nextEvent")
    val backEvent = props.string("backEvent")
    val completeEvent = props.string("completeEvent")
    val compact = props.bool("compact")
    val allowBack = props.bool("allowBack", true)
    val controlled = (props as? JsonObject)?.get("currentStep")?.let { almadarScalar(it)?.toIntOrNull() }
    var internal by remember { mutableStateOf(0) }
    val current = controlled ?: internal
    val total = steps.size
    val step = steps.getOrNull(current)
    val isFirst = current == 0
    val isLast = current == total - 1
    val padding = almadarSpaceFor(if (compact) "md" else "lg")
    if (props.bool("isLoading")) {
        AlmadarSkeletonSlot(props, "card", modifier)
    } else if (props.jsonObject("error") != null) {
        AlmadarErrorText(props, modifier.fillMaxWidth().padding(vertical = almadarSpaceFor("lg")))
    } else {
        Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
            if (props.bool("showProgress", true)) {
                WizardProgressRenderer(
                    almadarJson(
                        "steps" to JsonArray(steps.mapIndexed { index, item ->
                            almadarJson("id" to item.string("id", item.string("tabId", "step-" + index)), "title" to item.string("title", item.string("name", "Step " + (index + 1) + " of " + total)))
                        }),
                        "currentStep" to current, "allowNavigation" to allowBack, "compact" to compact, "sourceTrait" to almadarTrait(props)
                    ),
                    Modifier.fillMaxWidth(),
                    onEvent
                )
            }
            if (!compact && step != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = padding, vertical = almadarSpaceFor("md"))) {
                    AlmadarText(step.string("title", step.string("name", "Step " + (current + 1) + " of " + total)), variant = "h4")
                    if (step.string("description").isNotEmpty()) AlmadarText(step.string("description"), variant = "body2", color = "muted")
                }
                AlmadarDivider()
            }
            Box(modifier = Modifier.fillMaxWidth().padding(padding)) {
                AlmadarNode(step?.get("content"), onEvent, Modifier.fillMaxWidth())
            }
            AlmadarDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = padding, vertical = almadarSpaceFor(if (compact) "sm" else "md")),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlmadarButton(
                    props, "Back", backEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                    { _, _ ->
                        if (!isFirst && allowBack) {
                            if (controlled == null) internal = current - 1
                            if (backEvent.isNotEmpty()) onEvent(backEvent, eventEnvelope(props, almadarJson("step" to current - 1)))
                        }
                    },
                    payload = almadarJson("step" to current - 1), variant = "secondary", icon = "chevron-left",
                    disabled = isFirst || !allowBack, local = backEvent.isEmpty()
                )
                AlmadarText("Step " + (current + 1) + " of " + total, variant = "caption", color = "muted")
                if (isLast) {
                    AlmadarButton(
                        props, "Complete", completeEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                        { _, _ -> if (completeEvent.isNotEmpty()) onEvent(completeEvent, eventEnvelope(props, almadarJson("step" to current))) },
                        payload = almadarJson("step" to current), local = completeEvent.isEmpty()
                    )
                } else {
                    AlmadarButton(
                        props, "Next", nextEvent.ifEmpty { ALMADAR_LOCAL_ACTION },
                        {  _, _ ->
                            if (controlled == null && current + 1 < total) internal = current + 1
                            if (nextEvent.isNotEmpty()) onEvent(nextEvent, eventEnvelope(props, almadarJson("step" to current + 1)))
                        },
                        payload = almadarJson("step" to current + 1), icon = "chevron-right", local = nextEvent.isEmpty()
                    )
                }
            }
        }
    }
}
