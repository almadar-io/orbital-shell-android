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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

// tier: molecules — a grouped container styled by tokens only

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun almadarQrAnalyze(proxy: ImageProxy, scanner: BarcodeScanner, mainExecutor: Executor, onCode: (String) -> Unit) {
    val media = proxy.image
    if (media == null) {
        proxy.close()
        return
    }
    scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
        .addOnSuccessListener(mainExecutor) { codes -> onCode(codes.firstNotNullOfOrNull { it.rawValue }.orEmpty()) }
        .addOnCompleteListener { proxy.close() }
}

@Composable
private fun AlmadarQrOverlay(color: Color, arm: Float, weight: Float, hairline: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val left = size.width * 0.15f
        val right = size.width * 0.85f
        val top = size.height * 0.15f
        val bottom = size.height * 0.85f
        val middle = size.height / 2f
        val corners = Path().apply {
            moveTo(left, top + arm); lineTo(left, top); lineTo(left + arm, top)
            moveTo(right - arm, top); lineTo(right, top); lineTo(right, top + arm)
            moveTo(left, bottom - arm); lineTo(left, bottom); lineTo(left + arm, bottom)
            moveTo(right - arm, bottom); lineTo(right, bottom); lineTo(right, bottom - arm)
        }
        drawPath(corners, color, style = Stroke(width = weight))
        drawLine(color.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(left, middle), androidx.compose.ui.geometry.Offset(right, middle), strokeWidth = hairline)
    }
}

@Composable
fun QrScannerRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val density = LocalDensity.current
    val scanEvent = props.string("scanEvent")
    val currentProps by rememberUpdatedState(props)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val declaredPaused = props.bool("paused")
    val declaredFacing = props.string("facingMode", "environment")
    var paused by remember { mutableStateOf(declaredPaused) }
    var facing by remember { mutableStateOf(declaredFacing) }
    LaunchedEffect(declaredPaused) { paused = declaredPaused }
    LaunchedEffect(declaredFacing) { facing = declaredFacing }
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    val lastCode = remember { arrayOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(hasCamera, granted) { if (hasCamera && !granted) launcher.launch(Manifest.permission.CAMERA) }
    val previewView = remember(context, hasCamera) { if (hasCamera) PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } else null }
    val error = when {
        !hasCamera -> "Camera API not available in this environment"
        denied -> "Camera permission denied"
        else -> failure
    }
    fun emit(text: String) {
        if (scanEvent.isEmpty()) return
        currentOnEvent(scanEvent, eventEnvelope(currentProps, almadarJson("text" to text, "format" to "QR_CODE", "timestamp" to System.currentTimeMillis())))
    }
    DisposableEffect(hasCamera, granted, paused, facing) {
        val view = previewView
        if (view == null || !granted || paused) {
            ready = false
            return@DisposableEffect onDispose { }
        }
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        lastCode[0] = ""
        providerFuture.addListener({
            try {
                val bound = providerFuture.get()
                provider = bound
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(view.surfaceProvider)
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(analysisExecutor) { proxy ->
                    almadarQrAnalyze(proxy, scanner, mainExecutor) { code ->
                        if (code.isEmpty()) {
                            lastCode[0] = ""
                        } else if (code != lastCode[0]) {
                            lastCode[0] = code
                            emit(code)
                        }
                    }
                }
                val selector = if (facing == "user") CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                bound.unbindAll()
                bound.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                failure = null
                ready = true
            } catch (problem: Exception) {
                failure = problem.message ?: "Camera unavailable"
                ready = false
            }
        }, mainExecutor)
        onDispose {
            provider?.unbindAll()
            scanner.close()
            analysisExecutor.shutdown()
            ready = false
        }
    }
    val fallback = (props as? JsonObject)?.get("fallback")?.takeIf { (it is JsonPrimitive && it.content.isNotEmpty()) || (it is JsonArray && it.isNotEmpty()) || it is JsonObject }
    if (error != null && fallback != null) {
        Box(modifier) {
            if (fallback is JsonArray) fallback.forEach { RenderChild(it, Modifier, onEvent) } else RenderChild(fallback, Modifier, onEvent)
        }
    } else {
        CompositionLocalProvider(LocalContentColor provides palette.background) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = AlmadarSizes.maxWMd)
                    .aspectRatio(1f)
                    .clip(almadarShape(AlmadarCorner.Interactive))
                    .background(palette.foreground)
                    .almadarLabel(props.string("aria-label", "QR scanner"))
            ) {
                if (previewView != null) AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                if (props.bool("showOverlay", true) && ready && !paused) {
                    AlmadarQrOverlay(palette.background, with(density) { almadarSpace("space8").toPx() }, with(density) { geometry.borderHeavy.toPx() }, with(density) { geometry.borderHairline.toPx() })
                }
                if (error != null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(almadarSpace("space4")),
                        verticalArrangement = Arrangement.spacedBy(almadarSpace("space2"), Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AlmadarGlyph("camera", size = "lg")
                        AlmadarText("Camera unavailable", variant = "body2", color = "inherit", align = "center")
                        AlmadarText(error, modifier = Modifier.alpha(0.7f), variant = "caption", color = "inherit", align = "center")
                    }
                } else if (paused) {
                    Box(Modifier.fillMaxSize().background(palette.scrim), contentAlignment = Alignment.Center) {
                        AlmadarText("Paused", variant = "body2", color = "inherit")
                    }
                }
                if (props.bool("showCameraControls", true)) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = almadarSpace("space3")),
                        horizontalArrangement = Arrangement.spacedBy(almadarSpace("space2"))
                    ) {
                        val control: @Composable (String, String, () -> Unit) -> Unit = { icon, label, act ->
                            Box(
                                modifier = Modifier
                                    .size(LocalAlmadarDensity.current.buttonHeightSm)
                                    .clip(almadarShape(AlmadarCorner.Pill))
                                    .background(palette.foreground.copy(alpha = 0.6f))
                                    .almadarActionTag(ALMADAR_LOCAL_ACTION, props)
                                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { act() }
                                    .almadarLabel(label),
                                contentAlignment = Alignment.Center
                            ) { AlmadarGlyph(icon, color = "inherit", size = "sm") }
                        }
                        control(if (paused) "play" else "pause", if (paused) "Resume scanning" else "Pause scanning") { paused = !paused }
                        control("refresh-cw", if (facing == "environment") "Switch to front camera" else "Switch to rear camera") { facing = if (facing == "environment") "user" else "environment" }
                    }
                }
            }
        }
    }
}
