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
import androidx.compose.ui.draw.clip

// tier: atoms — a single Material 3 control

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Composable
fun ImageRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val alt = props.string("alt")
    val source = props.assetUrl("src")
    if (source.isEmpty()) return
    val aspect = when (props.string("aspect", "auto")) {
        "1/1" -> 1f
        "4/3" -> 4f / 3f
        "3/2" -> 3f / 2f
        "16/9" -> 16f / 9f
        "21/9" -> 21f / 9f
        else -> 0f
    }
    val corner = when (props.string("rounded", "md")) {
        "none" -> AlmadarCorner.None
        "sm" -> AlmadarCorner.Interactive
        "full" -> AlmadarCorner.Pill
        else -> AlmadarCorner.Container
    }
    val shape = almadarShape(corner)
    val scale = when (props.string("fit", "cover")) {
        "contain", "scale-down" -> ContentScale.Fit
        "fill" -> ContentScale.FillBounds
        "none" -> ContentScale.None
        else -> ContentScale.Crop
    }
    var frame: Modifier = modifier.fillMaxWidth()
    frame = if (aspect > 0f) frame.aspectRatio(aspect) else frame.heightIn(min = LocalAlmadarIconography.current.defaultSize * 5f)
    frame = frame.clip(shape)
    if (props.bool("bordered")) frame = frame.then(almadarBorder(corner, LocalAlmadarGeometry.current.borderStandard, palette.border))
    Box(
        modifier = frame.background(palette.muted).almadarLabel(props.string("aria-label", alt.ifEmpty { source })),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = source,
            imageLoader = rememberAlmadarImageLoader(),
            contentDescription = null,
            contentScale = scale,
            modifier = Modifier.fillMaxSize()
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                is AsyncImagePainter.State.Error -> AlmadarIcon(name = "image", tint = palette.mutedForeground, modifier = Modifier.size(LocalAlmadarIconography.current.defaultSize * 2f))
                else -> Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
