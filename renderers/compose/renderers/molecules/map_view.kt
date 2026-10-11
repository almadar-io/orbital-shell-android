package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow

private fun almadarMapNumber(obj: JsonObject, key: String): Double? = (obj[key] as? JsonPrimitive)?.doubleOrNull

private fun almadarMapPoint(obj: JsonObject): GeoPoint? {
    val lat = almadarMapNumber(obj, "lat") ?: return null
    val lng = almadarMapNumber(obj, "lng") ?: return null
    return GeoPoint(lat, lng)
}

private fun almadarMapDash(raw: String, density: Float): FloatArray? {
    val values = raw.split(' ', ',').mapNotNull { it.trim().toFloatOrNull()?.times(density) }
    return if (values.isEmpty()) null else values.toFloatArray()
}

/** The web Leaflet pin (25 x 41 icon): a teardrop in the theme primary with a hole, tip on the position. */
private fun almadarMapPin(map: MapView, fill: Int, hole: Int, width: Float): android.graphics.drawable.Drawable {
    val height = width * 41f / 25f
    val bitmap = android.graphics.Bitmap.createBitmap(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = width / 2f
    val reach = height - radius
    val tangent = Math.acos((radius / reach).toDouble())
    val spread = (radius * Math.sin(tangent)).toFloat()
    val drop = (radius * Math.cos(tangent)).toFloat()
    paint.color = fill
    canvas.drawCircle(radius, radius, radius, paint)
    val tail = android.graphics.Path()
    tail.moveTo(radius, height)
    tail.lineTo(radius - spread, radius + drop)
    tail.lineTo(radius + spread, radius + drop)
    tail.close()
    canvas.drawPath(tail, paint)
    paint.color = hole
    canvas.drawCircle(radius, radius, radius * 0.4f, paint)
    return android.graphics.drawable.BitmapDrawable(map.resources, bitmap)
}

private fun almadarMapOverlay(
    map: MapView,
    markers: List<JsonObject>,
    routes: List<JsonObject>,
    pin: GeoPoint?,
    attribution: Boolean,
    markerEvent: String,
    pinFill: Int,
    pinHole: Int,
    pinWidth: Float,
    routeColor: (String) -> Int,
    onMapClick: (GeoPoint) -> Unit,
    onMarkerClick: (JsonObject) -> Unit,
) {
    map.overlays.clear()
    val pinIcon = almadarMapPin(map, pinFill, pinHole, pinWidth)
    map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
            onMapClick(p)
            return true
        }
        override fun longPressHelper(p: GeoPoint): Boolean = false
    }))
    routes.forEach { route ->
        val waypoints = (route["waypoints"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::almadarMapPoint) } ?: emptyList()
        if (waypoints.size < 2) return@forEach
        val line = Polyline(map)
        line.setPoints(waypoints)
        val opacity = almadarMapNumber(route, "opacity") ?: 0.8
        val color = routeColor((route["color"] as? JsonPrimitive)?.contentOrNull ?: "")
        line.outlinePaint.color = (color and 0x00FFFFFF) or ((opacity * 255).toInt() shl 24)
        line.outlinePaint.strokeWidth = ((almadarMapNumber(route, "weight") ?: 4.0) * map.resources.displayMetrics.density).toFloat()
        line.outlinePaint.strokeCap = Paint.Cap.ROUND
        line.outlinePaint.strokeJoin = Paint.Join.ROUND
        almadarMapDash((route["dashArray"] as? JsonPrimitive)?.contentOrNull ?: "", map.resources.displayMetrics.density)?.let { line.outlinePaint.pathEffect = DashPathEffect(it, 0f) }
        val label = (route["label"] as? JsonPrimitive)?.contentOrNull ?: ""
        line.title = label
        if (label.isEmpty()) line.setInfoWindow(null)
        line.setOnClickListener { polyline, view, position ->
            onMapClick(position)
            if (label.isNotEmpty()) {
                polyline.infoWindowLocation = position
                polyline.showInfoWindow()
            }
            true
        }
        map.overlays.add(line)
    }
    markers.forEach { item ->
        val position = almadarMapPoint(item) ?: return@forEach
        val marker = Marker(map)
        marker.position = position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = pinIcon
        val label = (item["label"] as? JsonPrimitive)?.contentOrNull ?: ""
        marker.title = label
        marker.snippet = (item["category"] as? JsonPrimitive)?.contentOrNull ?: ""
        if (label.isEmpty()) marker.setInfoWindow(null)
        marker.setOnMarkerClickListener { tapped, view ->
            if (markerEvent.isNotEmpty()) onMarkerClick(item)
            InfoWindow.closeAllInfoWindowsOn(view)
            if (label.isNotEmpty()) tapped.showInfoWindow()
            true
        }
        map.overlays.add(marker)
    }
    pin?.let { spot ->
        val marker = Marker(map)
        marker.position = spot
        marker.title = "%.5f, %.5f".format(spot.latitude, spot.longitude)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = pinIcon
        map.overlays.add(marker)
    }
    if (attribution) {
        map.overlays.add(CopyrightOverlay(map.context))
    }
    map.invalidate()
}

@Composable
fun MapViewRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAlmadarTheme.current.color
    val markerEvent = props.string("markerClickEvent")
    val mapEvent = props.string("mapClickEvent")
    val centerLat = props.float("centerLat", 51.505f).toDouble()
    val centerLng = props.float("centerLng", -0.09f).toDouble()
    val zoom = props.float("zoom", 13f).toDouble()
    val showPin = props.bool("showClickedPin")
    val attribution = props.bool("showAttribution", true)
    val markers = props.array("markers").mapNotNull { it as? JsonObject }
    val routes = props.array("routes").mapNotNull { it as? JsonObject }
    val tint = palette.primary.toArgb()
    val pinHole = palette.primaryForeground.toArgb()
    val pinWidth = with(LocalDensity.current) { almadarSpace("space6").toPx() }
    val routeColors = routes.associate { route ->
        val raw = (route["color"] as? JsonPrimitive)?.contentOrNull ?: ""
        raw to (if (raw.isEmpty()) palette.primary else almadarCssColor(raw, palette.primary)).toArgb()
    }
    var pin by remember { mutableStateOf<GeoPoint?>(null) }
    val applied = remember { arrayOf("", "") }
    val currentProps by rememberUpdatedState(props)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val shape = almadarShape(AlmadarCorner.Container)
    val height = parseCssLength(props.string("height", "400px"), Dp.Unspecified)
    val contentKey = listOf(markers.toString(), routes.toString(), pin?.toString().orEmpty(), attribution, markerEvent, mapEvent, showPin, tint, pinHole, pinWidth).joinToString("|")
    val viewKey = "$centerLat|$centerLng|$zoom"
    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
            Configuration.getInstance().userAgentValue = ctx.packageName
            Configuration.getInstance().osmdroidBasePath = ctx.cacheDir
            Configuration.getInstance().osmdroidTileCache = java.io.File(ctx.cacheDir, "osmdroid-tiles")
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
            }
        },
        update = { map ->
            if (applied[0] != viewKey) {
                applied[0] = viewKey
                map.controller.setZoom(zoom)
                map.controller.setCenter(GeoPoint(centerLat, centerLng))
            }
            if (applied[1] != contentKey) {
                applied[1] = contentKey
                almadarMapOverlay(
                    map, markers, routes, pin, attribution, markerEvent, tint, pinHole, pinWidth,
                    { raw -> routeColors[raw] ?: tint },
                    { point ->
                        if (showPin) pin = point
                        if (mapEvent.isNotEmpty()) currentOnEvent(mapEvent, eventEnvelope(currentProps, almadarJson("latitude" to point.latitude, "longitude" to point.longitude)))
                    },
                    { marker -> currentOnEvent(markerEvent, eventEnvelope(currentProps, marker)) },
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (height == Dp.Unspecified) Modifier else Modifier.height(height))
            .clip(shape)
            .then(if (mapEvent.isEmpty()) Modifier else Modifier.almadarActionTag(mapEvent, props))
    )
}
