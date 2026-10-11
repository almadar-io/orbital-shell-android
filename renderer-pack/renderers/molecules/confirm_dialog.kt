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

// tier: molecules — a grouped container styled by tokens only

@Composable
fun ConfirmDialogRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val variant = props.string("variant", "danger")
    val loading = props.bool("isLoading")
    val closeEvent = props.string("onClose")
    val confirmEvent = props.string("onConfirm")
    val tone = almadarTone(if (variant == "default") "primary" else variant)
    val icon = when (variant) { "danger" -> "trash-2"; "warning" -> "alert-triangle"; else -> "check" }
    val message = root?.get("message") ?: root?.get("description")
    val error = props.jsonObject("error")
    val cap = when (props.string("size", "sm")) {
        "md" -> AlmadarSizes.maxW2xl
        "lg" -> AlmadarSizes.maxW3xl
        "xl" -> AlmadarSizes.maxW5xl
        "full" -> Dp.Infinity
        else -> AlmadarSizes.maxWMd
    }
    if (props.bool("isOpen", true)) {
        Box(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", props.string("title"))), contentAlignment = Alignment.TopCenter) {
            AlmadarPanel(modifier = Modifier.widthIn(max = cap).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("lg")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md")),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier.size(LocalAlmadarDensity.current.buttonHeightLg).background(tone.solid),
                        contentAlignment = Alignment.Center
                    ) { AlmadarIcon(name = icon, tint = tone.onSolid) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                        AlmadarText(props.string("title"), variant = "h5")
                        if (message is JsonPrimitive) AlmadarText(message.content, variant = "body2", color = "muted")
                        else AlmadarNode(message, onEvent, Modifier.fillMaxWidth(), variant = "body2")
                        if (error != null) AlmadarText(error.string("message"), variant = "body2", color = "error")
                    }
                }
                AlmadarDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().background(LocalAlmadarTheme.current.color.muted).padding(horizontal = almadarSpaceFor("lg"), vertical = almadarSpaceFor("md")),
                    horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"), Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlmadarButton(
                        props, props.string("cancelText", props.string("cancelLabel", "Cancel")), closeEvent.ifEmpty { ALMADAR_LOCAL_ACTION }, onEvent,
                        variant = "secondary", disabled = loading, local = closeEvent.isEmpty()
                    )
                    AlmadarButton(
                        props, if (loading) "Loading..." else props.string("confirmText", props.string("confirmLabel", "Confirm")),
                        confirmEvent.ifEmpty { ALMADAR_LOCAL_ACTION }, onEvent,
                        variant = if (variant == "danger" || variant == "warning") variant else "primary",
                        loading = loading, local = confirmEvent.isEmpty()
                    )
                }
            }
        }
    }
}
