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

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun WizardProgressRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val steps = props.array("steps").mapIndexed { index, step ->
        if (step is JsonPrimitive) Pair("step-" + index, step.contentOrNull ?: "") else Pair(step.string("id", "step-" + index), step.string("title"))
    }
    val current = props.int("currentStep")
    val compact = props.bool("compact")
    val allowNavigation = props.bool("allowNavigation", true)
    val stepClickEvent = props.string("stepClickEvent")
    val wide = LocalConfiguration.current.screenWidthDp.dp >= AlmadarBreakpoints.md
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label"))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor(if (compact) "md" else "lg"), vertical = almadarSpaceFor(if (compact) "sm" else "md")),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, step ->
                val active = index == current
                val completed = index < current
                val canNavigate = completed && allowNavigation
                AlmadarButton(
                    props, if (completed) "" else (index + 1).toString(), stepClickEvent, onEvent,
                    modifier = Modifier.semantics { selected = active },
                    payload = almadarJson("stepIndex" to index),
                    variant = if (active || completed) "primary" else "secondary",
                    icon = if (completed) "check" else "",
                    size = if (compact) "sm" else "md",
                    disabled = !canNavigate,
                    ariaLabel = "Step " + (index + 1) + " of " + steps.size + ": " + step.second,
                    local = !canNavigate
                )
                if (!compact && (active || wide)) {
                    AlmadarText(step.second, variant = "small", weight = "medium", color = if (active) "primary" else "muted", truncate = true)
                }
                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier.weight(1f).widthIn(min = almadarSpaceFor("md")).height(LocalAlmadarGeometry.current.borderHeavy)
                            .background(if (index < current) palette.primary else palette.border)
                    )
                }
            }
        }
        AlmadarDivider()
    }
}
