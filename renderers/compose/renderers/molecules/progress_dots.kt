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
fun ProgressDotsRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val count = props.int("count")
    val current = props.int("currentIndex")
    val base = LocalAlmadarIconography.current.defaultSize
    val rest = when (props.string("size", "md")) { "sm" -> 0.375f; "lg" -> 0.625f; else -> 0.5f }
    val active = when (props.string("size", "md")) { "sm" -> 0.5f; "lg" -> 0.875f; else -> 0.625f }
    Row(
        modifier = modifier.almadarLabel(props.string("aria-label", "Step " + (current + 1) + " of " + count)),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until count) {
            val isActive = index == current
            Box(
                modifier = Modifier
                    .size(if (isActive) base * active else base * rest)
                    .background(if (isActive) palette.primary else palette.muted, MaterialTheme.shapes.large)
                    .verificationContainer(VerificationIds.row(index.toString()))
            )
        }
    }
}
