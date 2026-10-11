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
fun WizardNavigationRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val current = props.int("currentStep")
    val total = props.int("totalSteps")
    val compact = props.bool("compact")
    val isValid = props.bool("isValid", true)
    val backEvent = props.string("onBack", "WIZARD_BACK")
    val nextEvent = props.string("onNext", "WIZARD_NEXT")
    val completeEvent = props.string("onComplete", "WIZARD_COMPLETE")
    val position = almadarJson("currentStep" to current, "totalSteps" to total)
    val isLast = current == total - 1
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        AlmadarDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor(if (compact) "md" else "lg"), vertical = almadarSpaceFor(if (compact) "sm" else "md")),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (props.bool("showBack", true)) {
                AlmadarButton(props, props.string("backLabel", "Back"), backEvent, onEvent, payload = position, variant = "secondary", icon = "chevron-left", disabled = current == 0)
            } else {
                Spacer(modifier = Modifier)
            }
            AlmadarText("Step " + (current + 1) + " of " + total, variant = "caption", color = "muted")
            if (isLast && props.bool("showComplete", true)) {
                AlmadarButton(props, props.string("completeLabel", "Complete"), completeEvent, onEvent, payload = position, disabled = !isValid)
            } else if (props.bool("showNext", true)) {
                ButtonRenderer(
                    almadarJson(
                        "label" to props.string("nextLabel", "Next"), "action" to nextEvent, "rightIcon" to "chevron-right",
                        "disabled" to !isValid, "actionPayload" to position, "sourceTrait" to almadarTrait(props)
                    ),
                    Modifier,
                    onEvent
                )
            } else {
                Spacer(modifier = Modifier)
            }
        }
    }
}
