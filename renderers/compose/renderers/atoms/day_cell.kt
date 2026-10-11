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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.ui.semantics.Role

@Composable
fun DayCellRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val raw = props.string("date")
    val today = props.bool("isToday")
    val clickEvent = props.string("onClick")
    val date = remember(raw) {
        runCatching { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).parse(raw.substringBefore('T')) }.getOrNull() ?: java.util.Date()
    }
    val weekday = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(date)
    val dayNumber = java.util.Calendar.getInstance().apply { time = date }.get(java.util.Calendar.DAY_OF_MONTH).toString()
    val full = java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(date)
    var cell: Modifier = Modifier.fillMaxWidth().background(if (today) palette.primary.copy(alpha = 0.1f) else Color.Transparent)
    if (clickEvent.isNotEmpty()) {
        cell = cell
            .clickable(role = Role.Button, onClickLabel = full) { onEvent(clickEvent, eventEnvelope(props, JsonObject(mapOf("date" to JsonPrimitive(raw))))) }
            .almadarTag(almadarActionId(clickEvent, props))
    }
    Box(modifier = modifier.almadarLabel(props.string("aria-label", full)), propagateMinConstraints = true) {
        Column(
            modifier = cell.padding(almadarSpaceFor("sm")),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weekday,
                style = almadarType("sm").copy(fontWeight = FontWeight.Medium),
                color = if (today) palette.primary else palette.mutedForeground
            )
            Surface(
                modifier = Modifier.size(LocalAlmadarIconography.current.defaultSize * 2f),
                shape = MaterialTheme.shapes.large,
                color = if (today) palette.primary else Color.Transparent,
                contentColor = if (today) palette.primaryForeground else palette.foreground
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = dayNumber, style = almadarType("base").copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}
