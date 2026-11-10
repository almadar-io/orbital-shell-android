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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlin.math.roundToInt

// tier: molecules — a grouped container styled by tokens only

@Composable
fun AnnotatedMediaRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    if (props.string("mediaType", "image") == "video") {
        FallbackRenderer("annotated-media", props, modifier, onEvent)
        return
    }
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val diameter = LocalAlmadarDensity.current.buttonHeightSm
    val radiusPx = with(LocalDensity.current) { diameter.toPx() } / 2f
    val title = props.string("title")
    val source = props.assetUrl("src")
    val placeEvent = props.string("placeEvent")
    val pinSelectEvent = props.string("pinSelectEvent")
    val placing = props.bool("placing", true)
    val activePinId = props.string("activePinId")
    val pins = props.array("pins").mapNotNull { it as? JsonObject }
    var box by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = modifier.fillMaxWidth()
            .almadarFlexFill()
            .heightIn(min = LocalAlmadarIconography.current.defaultSize * 5f)
            .clip(almadarShape(AlmadarCorner.Container))
            .background(palette.muted)
            .onSizeChanged { box = it }
            .pointerInput(placing, placeEvent) {
                detectTapGestures { tap ->
                    if (placing && placeEvent.isNotEmpty() && box.width > 0 && box.height > 0) {
                        val x = ((tap.x / box.width * 100f).coerceIn(0f, 100f) * 10f).roundToInt() / 10f
                        val y = ((tap.y / box.height * 100f).coerceIn(0f, 100f) * 10f).roundToInt() / 10f
                        onEvent(placeEvent, eventEnvelope(props, almadarJson("x" to x, "y" to y, "time" to 0)))
                    }
                }
            }
            .almadarActionTag(placeEvent, props)
            .almadarLabel(props.string("aria-label", title.ifEmpty { source })),
        contentAlignment = Alignment.Center
    ) {
        if (source.isNotEmpty()) {
            SubcomposeAsyncImage(
                model = source,
                imageLoader = rememberAlmadarImageLoader(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    is AsyncImagePainter.State.Error -> AlmadarIcon(name = "image", tint = palette.mutedForeground, modifier = Modifier.size(LocalAlmadarIconography.current.defaultSize * 2f))
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        pins.forEachIndexed { index, pin ->
            val x = pin.primitive("x")?.doubleOrNull
            val y = pin.primitive("y")?.doubleOrNull
            if (x != null && y != null) {
                val id = pin.string("id")
                val active = id == activePinId
                val resolved = pin.bool("resolved")
                val ink = if (resolved) palette.mutedForeground else palette.primaryForeground
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset((box.width * x.toFloat() / 100f - radiusPx).roundToInt(), (box.height * y.toFloat() / 100f - radiusPx).roundToInt()) }
                        .size(diameter)
                        .clip(almadarShape(AlmadarCorner.Pill))
                        .background(if (resolved) palette.muted else palette.primary)
                        .then(almadarBorder(AlmadarCorner.Pill, if (active) geometry.borderHeavy else geometry.borderStandard, if (active) palette.ring else palette.background))
                        .clickable(enabled = pinSelectEvent.isNotEmpty()) { onEvent(pinSelectEvent, eventEnvelope(props, almadarJson("id" to id))) }
                        .almadarActionTag(pinSelectEvent, props)
                        .almadarLabel(pin.string("label").ifEmpty { (index + 1).toString() }),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides ink) {
                        AlmadarText((index + 1).toString(), variant = "caption", color = "inherit", weight = "bold", align = "center")
                    }
                }
            }
        }
    }
}
