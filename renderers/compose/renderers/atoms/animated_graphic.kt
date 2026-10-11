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

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Composable
fun AnimatedGraphicRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val alt = props.string("alt", props.string("aria-label"))
    val source = props.assetUrl("src")
    val placeholder: @Composable () -> Unit = {
        AlmadarIcon(name = "image", tint = palette.mutedForeground, modifier = Modifier.size(LocalAlmadarIconography.current.defaultSize * 2f))
    }
    Box(
        modifier = modifier
            .heightIn(min = LocalAlmadarIconography.current.defaultSize * 5f)
            .almadarLabel(alt),
        contentAlignment = Alignment.Center
    ) {
        if (source.isEmpty()) {
            placeholder()
        } else {
            SubcomposeAsyncImage(
                model = source,
                imageLoader = rememberAlmadarImageLoader(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    is AsyncImagePainter.State.Error -> placeholder()
                    else -> placeholder()
                }
            }
        }
    }
}
