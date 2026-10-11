package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: atoms — a single Material 3 control

@Composable
fun InfiniteScrollSentinelRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val loadEvent = props.string("loadMoreEvent")
    val loading = props.bool("isLoading")
    val hasMore = props.bool("hasMore", true)
    LaunchedEffect(hasMore, loading, loadEvent) {
        if (hasMore && !loading && loadEvent.isNotEmpty()) {
            onEvent(loadEvent, eventEnvelope(props, props.jsonObject("loadMorePayload")))
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(almadarSpaceFor("md"))
            .almadarTag(almadarActionId(loadEvent, props))
            .almadarLabel(if (loading) props.string("aria-label", "Loading") else ""),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(LocalAlmadarIconography.current.defaultSize * 1.5f),
                color = LocalAlmadarTheme.current.color.foreground,
                strokeWidth = LocalAlmadarGeometry.current.borderHeavy
            )
        }
    }
}
