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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight

// tier: atoms — a single Material 3 control

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Composable
fun AvatarRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val name = props.string("name")
    val initials = props.string("initials", name.trim().split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() })
    val iconName = props.string("icon")
    val status = props.string("status")
    val badge = props.string("badge")
    val action = props.string("action")
    val base = LocalAlmadarIconography.current.defaultSize
    val diameter = when (props.string("size", "md")) {
        "xs" -> base * 1.5f
        "sm" -> base * 2f
        "lg" -> base * 3f
        "xl" -> base * 4f
        else -> base * 2.5f
    }
    val shape = almadarShape(AlmadarCorner.Pill)
    val source = props.assetUrl("src")
    val faceFallback: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            if (initials.isNotEmpty()) {
                Text(text = initials, style = almadarType(if (diameter > base * 3f) "lg" else "sm").copy(fontWeight = FontWeight.Medium))
            } else {
                AlmadarIcon(name = iconName.ifEmpty { "user" }, modifier = Modifier.size(diameter / 2f))
            }
        }
    }
    val description = props.string("alt", props.string("aria-label", name))
    var tapped: Modifier = Modifier.size(diameter).clip(shape)
    if (action.isNotEmpty()) tapped = tapped.clickable { onEvent(action, eventEnvelope(props)) }.almadarTag(almadarActionId(action, props))
    Box(modifier = modifier.almadarLabel(description)) {
        Surface(modifier = tapped, shape = shape, color = palette.muted, contentColor = palette.mutedForeground) {
            if (source.isEmpty()) faceFallback()
            else SubcomposeAsyncImage(
                model = source,
                imageLoader = rememberAlmadarImageLoader(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    is AsyncImagePainter.State.Error -> faceFallback()
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        if (status.isNotEmpty()) {
            val dot = diameter / 4f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dot)
                    .background(palette.background, shape)
                    .padding(LocalAlmadarGeometry.current.borderStandard)
                    .background(if (status == "offline") palette.mutedForeground else almadarTone(status).solid, shape)
            )
        }
        if (badge.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = shape,
                color = palette.error,
                contentColor = palette.errorForeground
            ) {
                Text(
                    text = badge,
                    style = almadarType("xs"),
                    modifier = Modifier.padding(horizontal = almadarSpaceFor("xs"))
                )
            }
        }
    }
}
