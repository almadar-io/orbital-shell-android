package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.toIntSize
import coil.ImageLoader
import coil.decode.Decoder
import coil.decode.SvgDecoder
import coil.fetch.SourceResult
import coil.request.Options

internal const val NAVIGATE_EVENT: String = "NAVIGATE"
internal const val SIGN_OUT_EVENT: String = "SIGN_OUT"

/** The MIME type Coil reports for an SVG source (data URI header, HTTP Content-Type, or file extension); the only key that selects the SVG decoder. */
internal const val ALMADAR_SVG_MIME: String = "image/svg+xml"

internal class AlmadarSvgDecoderFactory : Decoder.Factory {
    private val svg = SvgDecoder.Factory()
    override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? =
        if (result.mimeType == ALMADAR_SVG_MIME) svg.create(result, options, imageLoader) else null
}

/** The one process-wide Coil loader every image-bearing atom shares; the SVG decoder is registered here and nowhere else. */
internal object AlmadarImages {
    private var loader: ImageLoader? = null

    @Synchronized
    fun loader(context: Context): ImageLoader =
        loader ?: ImageLoader.Builder(context)
            .components { add(AlmadarSvgDecoderFactory()) }
            .build()
            .also { loader = it }
}

@Composable
internal fun rememberAlmadarImageLoader(): ImageLoader = AlmadarImages.loader(LocalContext.current.applicationContext)
internal const val ALMADAR_LOCAL_ACTION: String = "local"

internal const val ALMADAR_DISABLED_ALPHA: Float = 0.38f

internal class AlmadarTone(val solid: Color, val onSolid: Color)

@Composable
internal fun almadarTone(name: String): AlmadarTone {
    val palette = LocalAlmadarTheme.current.color
    return when (name) {
        "primary" -> AlmadarTone(palette.primary, palette.primaryForeground)
        "secondary" -> AlmadarTone(palette.secondary, palette.secondaryForeground)
        "accent" -> AlmadarTone(palette.accent, palette.accentForeground)
        "success", "online", "active" -> AlmadarTone(palette.success, palette.successForeground)
        "warning", "away", "pending" -> AlmadarTone(palette.warning, palette.warningForeground)
        "error", "danger", "critical", "busy" -> AlmadarTone(palette.error, palette.errorForeground)
        "info" -> AlmadarTone(palette.info, palette.infoForeground)
        else -> AlmadarTone(palette.muted, palette.mutedForeground)
    }
}

@Composable
internal fun almadarSpace(key: String): Dp =
    LocalAlmadarDensity.current.spacing[key] ?: LocalAlmadarDensity.current.spacing["space0"] ?: Dp.Hairline

@Composable
internal fun almadarSpaceFor(token: String): Dp {
    val key = when (token) {
        "xs" -> "space1"
        "sm" -> "space2"
        "md" -> "space4"
        "lg" -> "space6"
        "xl" -> "space8"
        "2xl" -> "space12"
        else -> "space0"
    }
    return almadarSpace(key)
}

@Composable
internal fun almadarType(key: String): TextStyle {
    val typeScale = LocalAlmadarTheme.current.typeScale
    val entry = typeScale.scale[key] ?: return LocalTextStyle.current
    return TextStyle(fontSize = entry.size, lineHeight = entry.lineHeight, fontFamily = typeScale.bodyFamily)
}

@Composable
internal fun almadarFieldColors(drawsOwnBorder: Boolean = false): TextFieldColors {
    val palette = LocalAlmadarTheme.current.color
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.foreground,
        unfocusedTextColor = palette.foreground,
        errorTextColor = palette.foreground,
        disabledTextColor = palette.mutedForeground,
        focusedContainerColor = palette.card,
        unfocusedContainerColor = palette.card,
        errorContainerColor = palette.card,
        disabledContainerColor = palette.muted,
        focusedBorderColor = if (drawsOwnBorder) Color.Transparent else palette.ring,
        unfocusedBorderColor = if (drawsOwnBorder) Color.Transparent else palette.input,
        errorBorderColor = if (drawsOwnBorder) Color.Transparent else palette.error,
        disabledBorderColor = if (drawsOwnBorder) Color.Transparent else palette.border,
        focusedLabelColor = palette.foreground,
        unfocusedLabelColor = palette.mutedForeground,
        errorLabelColor = palette.error,
        cursorColor = palette.primary,
        errorCursorColor = palette.error,
        focusedPlaceholderColor = palette.placeholder,
        unfocusedPlaceholderColor = palette.placeholder,
        focusedSupportingTextColor = palette.mutedForeground,
        unfocusedSupportingTextColor = palette.mutedForeground,
        errorSupportingTextColor = palette.error
    )
}

internal fun Modifier.almadarLabel(label: String): Modifier =
    if (label.isEmpty()) this else this.semantics { contentDescription = label }

internal fun almadarActionId(eventKey: String, props: JsonElement): String =
    if (eventKey.isEmpty() || props.bool("local")) "" else VerificationIds.action(eventKey)

/** The id of an element whose [gesture] dispatches [eventKey] (`VerificationIds.longPress` / `swipeLeft` / `swipeRight`). */
internal fun almadarGestureId(eventKey: String, props: JsonElement, gesture: (String) -> String): String =
    if (eventKey.isEmpty() || props.bool("local")) "" else gesture(eventKey)

internal fun almadarFieldId(name: String, props: JsonElement): String =
    if (name.isEmpty() || props.bool("local")) "" else VerificationIds.field(name)

internal fun Modifier.almadarActionTag(eventKey: String, props: JsonElement): Modifier =
    this.almadarTag(almadarActionId(eventKey, props))

internal fun Modifier.almadarTag(id: String): Modifier =
    if (id.isEmpty()) this else this.verificationId(id)

internal fun Modifier.almadarAlpha(value: Float): Modifier = this.alpha(value)

internal fun Modifier.almadarHover(key: Any, onHover: (Boolean) -> Unit): Modifier =
    this.pointerInput(key) {
        detectTapGestures(onPress = {
            onHover(true)
            tryAwaitRelease()
            onHover(false)
        })
    }

// name is a canonical Lucide (kebab-case) icon name — the same key
// space IconMap.swift/IconMap.kt both resolve from. IconMap.resolveVector
// (generated by almadar-sync icon-map) owns the Material Symbols name ->
// Icons.Filled.<Member> conversion and its material-icons-extended
// verification; this composable only renders the resolved vector.
// description is the accessible label; null marks the glyph decorative.
@Composable
internal fun AlmadarIcon(
    name: String?,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = LocalContentColor.current,
    description: String? = null
) {
    Icon(imageVector = IconMap.resolveVector(name ?: ""), contentDescription = description, tint = tint, modifier = modifier)
}

internal val AlmadarNoEvent: (String, JsonElement?) -> Unit = { _, _ -> }

internal fun almadarJson(vararg pairs: Pair<String, Any?>): JsonObject {
    val entries = mutableMapOf<String, JsonElement>()
    for ((key, value) in pairs) {
        when (value) {
            is JsonElement -> entries[key] = value
            is Boolean -> entries[key] = JsonPrimitive(value)
            is Number -> entries[key] = JsonPrimitive(value)
            is String -> if (value.isNotEmpty()) entries[key] = JsonPrimitive(value)
            else -> Unit
        }
    }
    return JsonObject(entries)
}

internal fun almadarTrait(parent: JsonElement): JsonElement? = (parent as? JsonObject)?.get("sourceTrait")

@Composable
internal fun AlmadarText(
    text: String,
    modifier: Modifier = Modifier,
    variant: String = "body1",
    color: String = "primary",
    weight: String = "none",
    align: String = "",
    truncate: Boolean = false
) {
    TypographyRenderer(
        almadarJson("content" to text, "variant" to variant, "color" to color, "weight" to weight, "align" to align, "truncate" to truncate),
        modifier,
        AlmadarNoEvent
    )
}

@Composable
internal fun AlmadarGlyph(name: String, modifier: Modifier = Modifier, color: String = "inherit", size: String = "md", label: String = "", animation: String = "") {
    IconRenderer(almadarJson("icon" to name, "color" to color, "size" to size, "aria-label" to label, "animation" to animation), modifier, AlmadarNoEvent)
}

@Composable
internal fun AlmadarButton(
    parent: JsonElement,
    label: String,
    action: String,
    onEvent: (String, JsonElement?) -> Unit,
    modifier: Modifier = Modifier,
    payload: JsonElement? = null,
    variant: String = "primary",
    icon: String = "",
    size: String = "md",
    disabled: Boolean = false,
    loading: Boolean = false,
    ariaLabel: String = "",
    local: Boolean = false
) {
    ButtonRenderer(
        almadarJson(
            "label" to label, "action" to action, "variant" to variant, "leftIcon" to icon, "size" to size,
            "disabled" to disabled, "isLoading" to loading, "aria-label" to ariaLabel,
            "actionPayload" to payload, "sourceTrait" to almadarTrait(parent), "local" to local
        ),
        modifier,
        onEvent
    )
}

@Composable
internal fun AlmadarLocalButton(
    parent: JsonElement,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: String = "primary",
    icon: String = "",
    size: String = "md",
    disabled: Boolean = false,
    ariaLabel: String = ""
) {
    AlmadarButton(
        parent, label, ALMADAR_LOCAL_ACTION, { _, _ -> onClick() }, modifier,
        variant = variant, icon = icon, size = size, disabled = disabled, ariaLabel = ariaLabel, local = true
    )
}

@Composable
internal fun AlmadarFab(
    parent: JsonElement,
    action: String,
    onEvent: (String, JsonElement?) -> Unit,
    icon: String,
    modifier: Modifier = Modifier,
    payload: JsonElement? = null,
    variant: String = "primary",
    ariaLabel: String = "",
    local: Boolean = false
) {
    val tone = almadarTone(variant)
    val fabProps = almadarJson("action" to action, "actionPayload" to payload, "sourceTrait" to almadarTrait(parent), "local" to local)
    FloatingActionButton(
        onClick = { if (action.isNotEmpty()) onEvent(action, eventEnvelope(fabProps)) },
        modifier = modifier.almadarActionTag(action, fabProps).almadarLabel(ariaLabel),
        shape = CircleShape,
        containerColor = tone.solid,
        contentColor = tone.onSolid
    ) { AlmadarIcon(name = icon, modifier = Modifier.size(LocalAlmadarIconography.current.defaultSize)) }
}

/** An S-expression (or any JSON value) as text — the twin of the Swift kit `AtomKit.sexpr`. */
internal fun almadarSexpr(value: JsonElement?): String = when (value) {
    is JsonArray -> "(" + value.joinToString(" ") { almadarSexpr(it) } + ")"
    is JsonObject -> "{" + value.keys.sorted().joinToString(", ") { it + ": " + almadarSexpr(value[it]) } + "}"
    is JsonPrimitive -> value.contentOrNull ?: "null"
    else -> "null"
}

@Composable
internal fun AlmadarBadge(
    label: String,
    modifier: Modifier = Modifier,
    variant: String = "default",
    size: String = "sm",
    icon: String = "",
    removeEvent: String = "",
    removeLabel: String = "",
    local: Boolean = false,
    onEvent: (String, JsonElement?) -> Unit = AlmadarNoEvent
) {
    BadgeRenderer(
        almadarJson("label" to label, "variant" to variant, "size" to size, "icon" to icon, "onRemove" to removeEvent, "removeLabel" to removeLabel, "local" to local),
        modifier,
        onEvent
    )
}

@Composable
internal fun AlmadarDivider(modifier: Modifier = Modifier, label: String = "", vertical: Boolean = false) {
    DividerRenderer(almadarJson("label" to label, "orientation" to if (vertical) "vertical" else ""), modifier, AlmadarNoEvent)
}

@Composable
internal fun AlmadarSpinner(modifier: Modifier = Modifier, size: String = "sm", label: String = "") {
    SpinnerRenderer(almadarJson("size" to size, "aria-label" to label), modifier, AlmadarNoEvent)
}

@Composable
internal fun AlmadarProgress(value: Float, modifier: Modifier = Modifier, max: Float = 100f, label: String = "", color: String = "primary") {
    ProgressBarRenderer(almadarJson("value" to value, "max" to max, "label" to label, "color" to color), modifier, AlmadarNoEvent)
}

@Composable
internal fun AlmadarCheckbox(
    checked: Boolean,
    event: String,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    name: String = "",
    disabled: Boolean = false,
    local: Boolean = false
) {
    CheckboxRenderer(
        almadarJson("checked" to checked, "label" to label, "name" to name, "onChange" to event, "disabled" to disabled, "local" to local),
        modifier
    ) { _, envelope -> onToggle(envelope?.jsonObject("actionPayload")?.bool("checked", !checked) ?: !checked) }
}

@Composable
internal fun AlmadarInput(
    name: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    helper: String = "",
    disabled: Boolean = false,
    leftIcon: String = "",
    clearable: Boolean = false,
    ariaLabel: String = "",
    inputType: String = "text",
    error: String = "",
    submitEvent: String = "",
    onSubmit: (String) -> Unit = {}
) {
    InputRenderer(
        almadarJson(
            "name" to name, "value" to value, "label" to label, "placeholder" to placeholder, "helperText" to helper,
            "disabled" to disabled, "leftIcon" to leftIcon, "clearable" to clearable, "aria-label" to ariaLabel,
            "inputType" to inputType, "error" to error,
            "onChange" to name, "action" to submitEvent
        ),
        modifier
    ) { event, envelope ->
        val typed = envelope?.jsonObject("actionPayload")?.string("value", "") ?: ""
        if (event == submitEvent) onSubmit(typed) else onChange(typed)
    }
}

internal fun formatFieldValue(value: JsonElement?, format: String?): String {
    val raw = (value as? JsonPrimitive)?.contentOrNull ?: return ""
    return when (format) {
        "currency" -> value.floatOrNull?.let { "$" + "%.2f".format(it) } ?: raw
        "percent" -> value.floatOrNull?.let { "${(it * 100).toInt()}%" } ?: raw
        "number" -> value.floatOrNull?.let { if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() } ?: raw
        "boolean" -> when (raw) { "true" -> "Yes"; "false" -> "No"; else -> raw }
        "date" -> try {
            java.time.LocalDate.parse(raw.take(10)).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (e: Exception) { raw }
        else -> raw
    }
}

internal fun almadarRows(entity: JsonElement?): List<JsonObject> = when (entity) {
    is JsonArray -> entity.mapNotNull { it as? JsonObject }
    is JsonObject -> listOf(entity)
    else -> emptyList()
}

internal fun almadarNested(row: JsonElement?, path: String): JsonElement? {
    var current: JsonElement? = row
    for (segment in path.split(".")) current = (current as? JsonObject)?.get(segment)
    return current
}

internal fun almadarFieldLabel(name: String): String {
    val leaf = name.substringAfterLast('.')
    val spaced = StringBuilder()
    leaf.forEachIndexed { index, ch ->
        if (ch == '_') spaced.append(' ')
        else {
            if (index > 0 && ch.isUpperCase() && leaf[index - 1].isLowerCase()) spaced.append(' ')
            spaced.append(ch)
        }
    }
    return spaced.toString().replaceFirstChar { it.uppercase() }
}

internal class AlmadarField(
    val key: String,
    val path: String,
    val label: String,
    val format: String,
    val variant: String,
    val icon: String,
    val colorMap: JsonObject?,
    val labels: JsonObject?,
    val sortable: Boolean
)

internal fun almadarFields(raw: List<JsonElement>): List<AlmadarField> = raw.mapNotNull { item ->
    when (item) {
        is JsonPrimitive -> item.contentOrNull?.takeIf { it.isNotEmpty() }
            ?.let { AlmadarField(it, it, almadarFieldLabel(it), "", "", "", null, null, false) }
        is JsonObject -> {
            val path = item.string("field", item.string("name", item.string("key", "")))
            if (path.isEmpty()) null else AlmadarField(
                item.string("key", path), path, item.string("label", item.string("header", almadarFieldLabel(path))),
                item.string("format"), item.string("variant"), item.string("icon"),
                item.jsonObject("colorMap"), item.jsonObject("labels"), item.bool("sortable")
            )
        }
        else -> null
    }
}

internal fun almadarBadgeVariant(field: AlmadarField, value: String): String {
    val declared = field.colorMap?.string(value, "") ?: ""
    return if (declared == "destructive") "danger" else if (declared.isEmpty()) "default" else declared
}

@Composable
internal fun AlmadarFieldCell(field: AlmadarField, row: JsonObject, modifier: Modifier = Modifier) {
    val value = almadarNested(row, field.path)
    val text = formatFieldValue(value, field.format)
    if (text.isNotEmpty()) {
        if (field.variant == "badge" || field.format == "badge") {
            AlmadarBadge(field.labels?.string(text, text) ?: text, modifier, almadarBadgeVariant(field, text))
        } else if (field.format == "boolean") {
            AlmadarBadge(text, modifier, if (text == "Yes") "success" else "neutral")
        } else if (field.variant == "h3" || field.variant == "h4") {
            AlmadarText(text, modifier, field.variant, weight = "medium")
        } else {
            AlmadarText(text, modifier, "small", "secondary")
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AlmadarRecordFields(fields: List<AlmadarField>, row: JsonObject, modifier: Modifier = Modifier, skipTitle: Boolean = false) {
    val title = fields.firstOrNull { it.variant == "h3" || it.variant == "h4" }
    val badges = fields.filter { it.variant == "badge" && it !== title }
    val body = fields.filter { it !== title && it !in badges }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        if (title != null && !skipTitle) AlmadarFieldCell(title, row)
        if (badges.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                badges.forEach { AlmadarFieldCell(it, row) }
            }
        }
        body.forEach { field ->
            val value = almadarNested(row, field.path)
            val text = formatFieldValue(value, field.format)
            if (field.variant == "progress") {
                val number = (value as? JsonPrimitive)?.floatOrNull
                if (number != null) AlmadarProgress(number, label = field.label)
            } else if (text.isNotEmpty()) {
                if (field.variant == "caption" && field.format != "boolean") {
                    AlmadarText(text, variant = "small", color = "secondary")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                        if (field.icon.isNotEmpty()) AlmadarGlyph(field.icon, color = "muted", size = "xs", label = field.label)
                        else AlmadarText(field.label + ":", variant = "caption", color = "secondary")
                        AlmadarFieldCell(field, row)
                    }
                }
            }
        }
    }
}

internal fun almadarActionEvent(action: JsonObject): String =
    action.string("event", if (action.string("navigatesTo").isNotEmpty()) NAVIGATE_EVENT else "")

internal fun almadarActionPayload(action: JsonObject, row: JsonObject?): JsonObject? {
    val navigatesTo = action.string("navigatesTo")
    if (row == null && navigatesTo.isEmpty()) return null
    val base = mutableMapOf<String, JsonElement>()
    if (row != null) {
        base["row"] = row
        row["id"]?.let { base["id"] = it }
    }
    if (navigatesTo.isNotEmpty()) {
        var url = navigatesTo
        Regex("\\{\\{(?:row\\.)?([\\w.]+)\\}\\}").findAll(navigatesTo).forEach { match ->
            url = url.replace(match.value, formatFieldValue(almadarNested(row, match.groupValues[1]), ""))
        }
        base["url"] = JsonPrimitive(url)
    }
    return JsonObject(base)
}

@Composable
internal fun AlmadarOverflow(
    parent: JsonElement,
    menu: List<JsonObject>,
    row: JsonObject?,
    onEvent: (String, JsonElement?) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .heightIn(min = LocalAlmadarDensity.current.buttonHeightSm)
                .widthIn(min = LocalAlmadarDensity.current.buttonHeightSm)
                .clickable(role = Role.Button) { open = true }
                .almadarTag(VerificationIds.ACTION_OVERFLOW)
                .almadarLabel("Actions"),
            contentAlignment = Alignment.Center
        ) { AlmadarGlyph("more-horizontal", color = "muted", size = "sm") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.verificationWindowRoot()) {
            Column(modifier = Modifier.padding(horizontal = almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                menu.forEach { action ->
                    AlmadarButton(
                        parent, action.string("label"), almadarActionEvent(action),
                        { name, envelope -> open = false; onEvent(name, envelope) },
                        payload = almadarActionPayload(action, row),
                        variant = if (action.string("variant") == "danger") "danger" else "ghost",
                        icon = action.string("icon"), size = "sm", disabled = action.bool("disabled")
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AlmadarRowActions(
    parent: JsonElement,
    inline: List<JsonObject>,
    menu: List<JsonObject>,
    row: JsonObject?,
    onEvent: (String, JsonElement?) -> Unit,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
        inline.forEach { action ->
            val glyphOnly = iconOnly && action.string("icon").isNotEmpty()
            AlmadarButton(
                parent, if (glyphOnly) "" else action.string("label"), almadarActionEvent(action), onEvent,
                payload = almadarActionPayload(action, row), variant = action.string("variant", "primary"),
                icon = action.string("icon"), size = "sm", disabled = action.bool("disabled"),
                ariaLabel = if (glyphOnly) action.string("label") else ""
            )
        }
        if (menu.isNotEmpty()) AlmadarOverflow(parent, menu, row, onEvent)
    }
}

@Composable
internal fun AlmadarEmptySlot(parent: JsonElement, fallbackTitle: String, onEvent: (String, JsonElement?) -> Unit, modifier: Modifier = Modifier) {
    val action = parent.jsonObject("emptyAction")
    EmptyStateRenderer(
        almadarJson(
            "icon" to parent.string("emptyIcon"),
            "title" to parent.string("emptyTitle", fallbackTitle),
            "description" to parent.string("emptyDescription"),
            "actionLabel" to action?.string("label"),
            "actionEvent" to action?.string("event"),
            "sourceTrait" to almadarTrait(parent)
        ),
        modifier,
        onEvent
    )
}

@Composable
internal fun AlmadarErrorText(parent: JsonElement, modifier: Modifier = Modifier) {
    val error = parent.jsonObject("error")
    if (error != null) AlmadarText(error.string("message"), modifier, color = "error", align = "center")
}

@Composable
internal fun AlmadarSkeletonSlot(parent: JsonElement, fallbackVariant: String, modifier: Modifier = Modifier) {
    SkeletonRenderer(
        almadarJson("spec" to (parent as? JsonObject)?.get("skeleton"), "variant" to fallbackVariant),
        modifier.fillMaxWidth(),
        AlmadarNoEvent
    )
}

internal fun almadarSelectedPayload(selected: Set<String>): JsonObject =
    almadarJson("selectedIds" to JsonArray(selected.map { JsonPrimitive(it) }))

internal class AlmadarActivityLine(
    val icon: String,
    val color: String,
    val spin: Boolean,
    val text: String,
    val details: List<String>,
    val raw: String
)

internal fun almadarActivityFacts(items: List<JsonObject>): List<Pair<Boolean, Int>> {
    val settled = BooleanArray(items.size)
    val readyCount = IntArray(items.size)
    val open = mutableMapOf<String, MutableList<Int>>()
    var ready = mutableSetOf<String>()
    items.forEachIndexed { index, activity ->
        val orbital = activity.string("orbitalName")
        when (activity.string("type")) {
            "orbital_started" -> open.getOrPut(orbital) { mutableListOf() }.add(index)
            "orbital_done" -> {
                val last = open[orbital]?.removeLastOrNull()
                if (last != null) settled[last] = true
                ready.add(orbital)
            }
            "done" -> {
                readyCount[index] = ready.size
                ready = mutableSetOf()
            }
            else -> Unit
        }
    }
    return items.indices.map { settled[it] to readyCount[it] }
}

internal fun almadarActivityLine(activity: JsonObject, settled: Boolean, readyCount: Int): AlmadarActivityLine {
    val orbital = activity.string("orbitalName")
    val tool = activity.string("tool")
    val traitAndTransition = listOf(activity.string("traitName"), activity.string("transitionEvent")).filter { it.isNotEmpty() }
    return when (activity.string("type")) {
        "message" -> AlmadarActivityLine("info", "muted", false, activity.string("content"), emptyList(), "")
        "tool_call" -> AlmadarActivityLine(
            "wrench", "muted", false, "Called " + tool, emptyList(),
            activity.string("argsText").let { if (it == "{}") "" else it }
        )
        "tool_result" -> AlmadarActivityLine(
            if (activity.bool("success")) "corner-down-right" else "alert-circle",
            if (activity.bool("success")) "muted" else "error", false, tool + " returned",
            listOfNotNull(activity.string("detail").ifEmpty { null }, activity.string("rows").ifEmpty { null }?.let { it + " rows" }),
            activity.string("resultText")
        )
        "file_operation" -> AlmadarActivityLine("file-text", "muted", false, activity.string("operation"), listOf(activity.string("path")), "")
        "schema_diff" -> AlmadarActivityLine("file-diff", "muted", false, "Edited " + activity.string("filePath"), emptyList(), "")
        "error" -> AlmadarActivityLine("alert-circle", "error", false, activity.string("message"), listOfNotNull(activity.string("code").ifEmpty { null }), "")
        "coordinator_decision" -> AlmadarActivityLine("compass", "muted", false, "Chose " + activity.string("organism"), listOf(activity.string("reason")), "")
        "plan_committed" -> {
            val names = activity.array("orbitals").map { it.string("name", (it as? JsonPrimitive)?.contentOrNull ?: "") }
            AlmadarActivityLine("layers", "muted", false, "Planning " + names.size + " orbitals", listOf(names.joinToString(", ")), "")
        }
        "pending_question" -> AlmadarActivityLine("help-circle", "warning", false, activity.string("question"), emptyList(), "")
        "clarification_question" -> AlmadarActivityLine(
            "help-circle", "warning", false, activity.string("question"),
            activity.array("candidates").map { c -> c.string("label") + (c.string("whyThisFits").let { if (it.isEmpty()) "" else " — " + it }) }, ""
        )
        "analysis" -> AlmadarActivityLine(
            "check", "success", false, "Analyzed your request — " + activity.string("organism"),
            listOfNotNull(activity.string("organismReason").ifEmpty { null }, activity.jsonObject("complexity")?.string("reasoning")?.ifEmpty { null }) +
                activity.array("renames").map { "Rename " + it.string("from") + " → " + it.string("to") } +
                activity.array("deletes").map { "Remove " + ((it as? JsonPrimitive)?.contentOrNull ?: "") },
            ""
        )
        "orbital_started" ->
            if (settled) AlmadarActivityLine("layers", "muted", false, "Started " + orbital, emptyList(), "")
            else AlmadarActivityLine("loader", "primary", true, "Working on " + orbital + "…", emptyList(), "")
        "orbital_done" -> AlmadarActivityLine(
            "check", "success", false, orbital + " ready",
            if (activity.string("traitCount").isNotEmpty() || activity.string("transitionCount").isNotEmpty())
                listOf(activity.int("traitCount").toString() + " traits, " + activity.int("transitionCount") + " transitions") else emptyList(),
            ""
        )
        "schema_change" -> {
            val kind = activity.string("changeKind")
            val text = when (kind) {
                "orbital-added" -> "Added " + orbital
                "orbital-removed" -> "Removed " + orbital
                "entity-fields-changed" -> "Changed the data fields of " + orbital
                "trait-added" -> "Added a behavior to " + orbital
                "trait-removed" -> "Removed a behavior from " + orbital
                "trait-config-changed" -> "Changed the settings of " + orbital
                "state-machine-changed" -> "Changed the flow of " + orbital
                "guard-changed" -> "Changed a rule in " + orbital
                "effect-changed" -> "Changed what " + orbital + " does"
                "render-ui-changed" -> "Changed the design of " + orbital
                "event-wiring-changed" -> "Changed the connections of " + orbital
                "page-changed" -> "Changed the pages of " + orbital
                "theme-changed" -> "Changed the theme of " + orbital
                else -> "Composed a behavior into " + orbital
            }
            val icon = when (kind) {
                "orbital-added" -> "circle-plus"
                "orbital-removed" -> "circle-minus"
                "entity-fields-changed" -> "database"
                "trait-added" -> "plus-square"
                "trait-removed" -> "minus-square"
                "trait-config-changed" -> "sliders-horizontal"
                "state-machine-changed" -> "git-branch"
                "guard-changed" -> "shield"
                "effect-changed" -> "zap"
                "render-ui-changed" -> "layout-panel-top"
                "event-wiring-changed" -> "cable"
                "page-changed" -> "file"
                "theme-changed" -> "palette"
                else -> "layers"
            }
            AlmadarActivityLine(icon, "primary", false, text, traitAndTransition, "")
        }
        "done" -> AlmadarActivityLine("check-circle", "success", false, "Done — " + readyCount + " orbitals ready", emptyList(), "")
        "cancelled" -> AlmadarActivityLine("circle-x", "muted", false, activity.string("message", "Cancelled"), emptyList(), "")
        "llm_response" -> {
            val llm = activity.jsonObject("llm")
            val summary = if (llm == null) "Raw LLM response" else listOfNotNull(
                llm.string("service") + " · " + llm.string("model"),
                (llm.float("durationMs") / 1000f).let { "%.1f".format(it) + "s" },
                if (llm.string("promptTokens").isNotEmpty() || llm.string("completionTokens").isNotEmpty())
                    llm.int("promptTokens").toString() + " in / " + llm.int("completionTokens") + " out tokens" else null,
                llm.string("costUSD").ifEmpty { null }?.let { "$" + "%.4f".format(it.toFloat()) }
            ).joinToString(" · ")
            AlmadarActivityLine(
                "cpu", "muted", false, summary, emptyList(),
                listOfNotNull(
                    "Response\n" + activity.string("content"),
                    llm?.string("systemPrompt")?.ifEmpty { null }?.let { "System prompt\n" + it },
                    llm?.string("userPrompt")?.ifEmpty { null }?.let { "User prompt\n" + it },
                    llm?.string("toolCallsJson")?.ifEmpty { null }?.let { "Tool calls\n" + it }
                ).joinToString("\n\n")
            )
        }
        else -> AlmadarActivityLine("info", "muted", false, activity.string("type"), emptyList(), "")
    }
}

@Composable
internal fun AlmadarActivityItem(activity: JsonObject, settled: Boolean, readyCount: Int, modifier: Modifier = Modifier) {
    val palette = LocalAlmadarTheme.current.color
    val type = activity.string("type")
    val role = activity.string("role")
    var open by remember { mutableStateOf(false) }
    if (type == "message" && role != "system") {
        val mine = role == "user"
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
            Surface(
                color = if (mine) palette.primary else palette.surface,
                contentColor = if (mine) palette.primaryForeground else palette.foreground,
                shape = MaterialTheme.shapes.large
            ) {
                Box(modifier = Modifier.padding(horizontal = almadarSpaceFor("md"), vertical = almadarSpaceFor("sm"))) {
                    if (mine) AlmadarText(activity.string("content"), variant = "body2", color = "inherit")
                    else MarkdownContentRenderer(almadarJson("content" to activity.string("content")), Modifier, AlmadarNoEvent)
                }
            }
        }
    } else {
        val line = almadarActivityLine(activity, settled, readyCount)
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.Top) {
            AlmadarGlyph(line.icon, color = line.color, size = "sm", animation = if (line.spin) "spin" else "")
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                if (line.raw.isEmpty()) {
                    AlmadarText(line.text, variant = if (type == "llm_response") "caption" else "body2", color = if (type == "llm_response") "muted" else "primary")
                } else {
                    AlmadarLocalButton(
                        activity, line.text, { open = !open },
                        variant = "ghost", size = "sm", icon = if (open) "chevron-down" else "chevron-right"
                    )
                    if (open) AlmadarText(line.raw, variant = "caption")
                }
                line.details.forEach { AlmadarText(it, variant = "caption", color = "muted") }
            }
        }
    }
}

@Composable
internal fun AlmadarThinkingRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
        AlmadarGlyph("loader", color = "primary", size = "sm", animation = "spin")
        AlmadarText("Thinking…", variant = "body2", color = "muted")
    }
}

internal const val MEDIA_UPLOAD_EVENT: String = "MEDIA_UPLOAD"
internal const val MEDIA_SELECT_EVENT: String = "MEDIA_SELECT"

@Composable
internal fun AlmadarMonoText(text: String, modifier: Modifier = Modifier, size: String = "xs", color: Color? = null, weight: FontWeight? = null, italic: Boolean = false) {
    val theme = LocalAlmadarTheme.current
    Text(
        text = text,
        modifier = modifier,
        color = color ?: theme.color.foreground,
        style = almadarType(size).copy(fontFamily = theme.typeScale.monoFamily, fontWeight = weight, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal)
    )
}

@Composable
internal fun AlmadarPanel(modifier: Modifier = Modifier, muted: Boolean = false, material: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalAlmadarTheme.current.color
    Surface(
        modifier = if (material) modifier.almadarSurfaceMaterial(MaterialTheme.shapes.medium) else modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (muted) palette.muted else palette.card,
        contentColor = palette.foreground,
        border = androidx.compose.foundation.BorderStroke(LocalAlmadarGeometry.current.borderStandard, palette.border)
    ) { Column(modifier = if (material) Modifier.almadarSurfaceImage(LocalAlmadarSurface.current.cardImage, MaterialTheme.shapes.medium) else Modifier, content = content) }
}

internal fun almadarScalar(value: JsonElement?): String? = (value as? JsonPrimitive)?.contentOrNull

internal fun almadarStrings(value: JsonElement?): List<String> =
    (value as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()

internal fun almadarIsAbsoluteHref(href: String): Boolean = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(href)

/** A horizontal stack's row, laid out as a CSS flex row. Each child's flex basis is its max-content width and its floor is its min-content width, both read from the child's own intrinsic measurements, so a child that cannot report one must declare it (`almadarFlexFill()`, `almadarIntrinsicWidth()`); nothing is guessed. When the bases overflow the row, every child shrinks in proportion to its basis down to its floor, never off the edge; otherwise a child that fills the width leaves its later siblings their basis. */
@Composable
internal fun AlmadarFlexRow(gap: androidx.compose.ui.unit.Dp, vertical: Alignment.Vertical, modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val limit = constraints.maxWidth
        val bounded = limit != androidx.compose.ui.unit.Constraints.Infinity
        val spacing = gapPx * (measurables.size - 1).coerceAtLeast(0)
        val basis = measurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }
        val floor = measurables.map { it.minIntrinsicWidth(constraints.maxHeight) }
        val allotted = basis.toIntArray()
        val overflow = bounded && basis.sum() + spacing > limit
        if (overflow) {
            var excess = basis.sum() + spacing - limit
            while (excess > 0) {
                val open = allotted.indices.filter { allotted[it] > floor[it] }
                if (open.isEmpty()) break
                val weight = open.sumOf { basis[it] }.coerceAtLeast(1)
                var taken = 0
                open.forEach { i ->
                    val cut = minOf((excess.toLong() * basis[i] / weight).toInt().coerceAtLeast(1), allotted[i] - floor[i])
                    allotted[i] -= cut
                    taken += cut
                }
                excess -= taken
            }
        }
        var used = 0
        val placeables = measurables.mapIndexed { i, m ->
            val ahead = (i + 1 until measurables.size).sumOf { basis[it] + gapPx }
            val room = when {
                !bounded -> androidx.compose.ui.unit.Constraints.Infinity
                overflow -> allotted[i]
                else -> (limit - used - ahead).coerceAtLeast(0)
            }
            m.measure(androidx.compose.ui.unit.Constraints(maxWidth = room, maxHeight = constraints.maxHeight)).also { used += it.width + gapPx }
        }
        val width = (placeables.sumOf { it.width } + spacing).coerceIn(constraints.minWidth, limit)
        val height = (placeables.maxOfOrNull { it.height } ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var x = 0
            placeables.forEach { p ->
                p.placeRelative(x, vertical.align(p.height, height))
                x += p.width + gapPx
            }
        }
    }
}

/** A node that reports declared intrinsic widths instead of forwarding the query to its subtree. A subtree that cannot answer an intrinsic query (SubcomposeLayout: BoxWithConstraints, Lazy lists and grids) throws when an ancestor asks, so it declares itself here. */
private class AlmadarIntrinsicNode(var min: Int, var max: Int, var declareHeight: Boolean) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = min
    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = max
    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = if (declareHeight) 0 else measurable.minIntrinsicHeight(width)
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = if (declareHeight) 0 else measurable.maxIntrinsicHeight(width)
}

private data class AlmadarIntrinsicElement(val min: Int, val max: Int, val declareHeight: Boolean) : ModifierNodeElement<AlmadarIntrinsicNode>() {
    override fun create(): AlmadarIntrinsicNode = AlmadarIntrinsicNode(min, max, declareHeight)
    override fun update(node: AlmadarIntrinsicNode) {
        node.min = min
        node.max = max
        node.declareHeight = declareHeight
    }
}

/** Declares the width this child asks a flex row for: `min` is its floor, `max` its basis (px). Heights still come from the child. */
internal fun Modifier.almadarIntrinsicWidth(min: Int, max: Int): Modifier = this.then(AlmadarIntrinsicElement(min, max, false))

/** Declares that this subtree has no natural size and fills whatever width and height it is offered (basis and floor 0); required on every BoxWithConstraints and Lazy root. */
internal fun Modifier.almadarFlexFill(): Modifier = this.then(AlmadarIntrinsicElement(0, 0, true))

@Composable
internal fun AlmadarBoxWithConstraints(modifier: Modifier = Modifier, contentAlignment: Alignment = Alignment.TopStart, content: @Composable BoxWithConstraintsScope.() -> Unit) {
    BoxWithConstraints(modifier = modifier.almadarFlexFill(), contentAlignment = contentAlignment, content = content)
}

/** HTML `<input>` default `size`: the field is this many characters wide before padding and border. */
internal const val ALMADAR_FIELD_DEFAULT_CHARS: Int = 20

/** The width (px) a text field asks a flex row for, derived the way the web `Input` is laid out: 20 characters of its text style plus `px-3` (or `pl-10`/`pr-10` when an icon or clear button sits in that edge) plus the border. Its floor is 0: the web input has `w-full`, a compressible percentage width. */
@Composable
internal fun almadarFieldBasis(leadingIcon: Boolean, trailingIcon: Boolean): Int {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = almadarType("sm")
    val digit = remember(style, density) { measurer.measure("0", style).size.width }
    val edge = almadarSpace("space3")
    val iconEdge = almadarSpace("space10")
    val border = LocalAlmadarGeometry.current.borderStandard
    return digit * ALMADAR_FIELD_DEFAULT_CHARS + with(density) { ((if (leadingIcon) iconEdge else edge) + (if (trailingIcon) iconEdge else edge) + border * 2).roundToPx() }
}

/** What a surface blurs: the page recorded without any material drawn (pass 1), so a material never samples itself. `consumers` counts the mounted materials; the host records only while it is non-zero. */
internal class AlmadarBackdropState(val layer: GraphicsLayer) {
    var origin: Offset = Offset.Zero
    var recording: Boolean = false
    var consumers: Int by mutableIntStateOf(0)
}

internal val LocalAlmadarBackdrop = compositionLocalOf<AlmadarBackdropState?> { null }

internal val LocalAlmadarGameOverlay = staticCompositionLocalOf { false }

/** The web `backdrop-filter` source: wraps the whole page, records it once into a GraphicsLayer when a material is mounted, then draws it normally so materials blur the recording behind themselves. */
@Composable
internal fun AlmadarBackdropHost(content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    val state = remember(layer) { AlmadarBackdropState(layer) }
    CompositionLocalProvider(LocalAlmadarBackdrop provides state) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { state.origin = it.positionInWindow() }
                .drawWithContent {
                    if (state.consumers > 0) {
                        state.recording = true
                        state.layer.record { this@drawWithContent.drawContent() }
                        state.recording = false
                    }
                    drawContent()
                },
            propagateMinConstraints = true
        ) { content() }
    }
}

/** Draws the page recording behind this node, blurred by `blur` (and saturated by `saturation`), clipped to `shape`: the web `backdrop-filter: blur() saturate()`. Place it before the node's own fill. Needs API 31 (RenderEffect); below that the node is drawn without the material, as a browser without backdrop-filter does. */
@Composable
internal fun Modifier.almadarBackdrop(shape: Shape, blur: androidx.compose.ui.unit.Dp, saturation: Float = 1f): Modifier {
    val state = LocalAlmadarBackdrop.current
    if (state == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    DisposableEffect(state) {
        state.consumers += 1
        onDispose { state.consumers -= 1 }
    }
    val materialLayer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val blurPx = with(LocalDensity.current) { blur.toPx() }
    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawBehind {
            if (!state.recording) {
                val behind = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
                val shift = state.origin - origin
                materialLayer.renderEffect = BlurEffect(blurPx, blurPx, TileMode.Clamp)
                materialLayer.colorFilter = if (saturation == 1f) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
                materialLayer.record(size.toIntSize()) {
                    clipPath(behind) { translate(shift.x, shift.y) { drawLayer(state.layer) } }
                }
                drawLayer(materialLayer)
            }
        }
}

/** The web `.surface-material`: the theme's `surface.backdrop` (blur radius and saturation) behind a panel; no token, no material. */
@Composable
internal fun Modifier.almadarSurfaceMaterial(shape: Shape): Modifier {
    val backdrop = LocalAlmadarSurface.current.backdrop ?: return this
    return almadarBackdrop(shape, backdrop.blurRadius, backdrop.saturation.toFloat())
}

/** The web `.surface-material` over a fill: the backdrop behind, then the fill, then the card image (`--surface-card-image`) over it, in the web's paint order. */
@Composable
internal fun Modifier.almadarMaterialFill(shape: Shape, color: Color): Modifier =
    almadarSurfaceMaterial(shape).background(color, shape).almadarSurfaceImage(LocalAlmadarSurface.current.cardImage, shape)

/** Tailwind `backdrop-blur-sm` (4px), the fixed blur the web `Overlay` scrim takes when `blur` is set; it is a utility class, not a theme token. */
internal val ALMADAR_OVERLAY_BLUR: androidx.compose.ui.unit.Dp = 4.dp

/** A parsed CSS `linear-gradient(...)`: the CSS angle (0deg is up, clockwise) and the stops, each a 0..1 position and a resolved colour. */
internal class AlmadarGradient(val angleDegrees: Double, val stops: List<Pair<Float, Color>>)

internal fun almadarSplitTopLevel(text: String): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var start = 0
    text.forEachIndexed { index, c ->
        when (c) {
            '(' -> depth += 1
            ')' -> depth -= 1
            ',' -> if (depth == 0) {
                parts.add(text.substring(start, index).trim())
                start = index + 1
            }
        }
    }
    parts.add(text.substring(start).trim())
    return parts
}

internal fun almadarGradientColor(raw: String, palette: AlmadarColorPalette): Color? {
    if (raw.startsWith("var(--color-") && raw.endsWith(")")) {
        return when (raw.removePrefix("var(--color-").removeSuffix(")")) {
            "primary" -> palette.primary
            "secondary" -> palette.secondary
            "accent" -> palette.accent
            "muted" -> palette.muted
            "muted-foreground" -> palette.mutedForeground
            "background" -> palette.background
            "foreground" -> palette.foreground
            "card" -> palette.card
            "surface" -> palette.surface
            "border" -> palette.border
            "error" -> palette.error
            "success" -> palette.success
            "warning" -> palette.warning
            "info" -> palette.info
            else -> null
        }
    }
    val parsed = parseCssColor(raw, Color.Unspecified)
    return if (parsed == Color.Unspecified) null else parsed
}

/** Only the plain `linear-gradient(<angle>|to <side>, <colour> [<pct>], ...)` grammar; anything else (corner directions, `color-mix`, unlisted `var()`, non-percentage positions) is null and the caller logs the skip. */
internal fun almadarParseLinearGradient(css: String, palette: AlmadarColorPalette): AlmadarGradient? {
    val open = "linear-gradient("
    if (!css.startsWith(open) || !css.endsWith(")")) return null
    val args = almadarSplitTopLevel(css.removePrefix(open).removeSuffix(")"))
    val head = args.firstOrNull() ?: return null
    var angle = 180.0
    var stopArgs = args
    if (head.endsWith("deg")) {
        angle = head.removeSuffix("deg").toDoubleOrNull() ?: return null
        stopArgs = args.drop(1)
    } else if (head.startsWith("to ")) {
        angle = when (head.removePrefix("to ")) {
            "top" -> 0.0
            "right" -> 90.0
            "bottom" -> 180.0
            "left" -> 270.0
            else -> return null
        }
        stopArgs = args.drop(1)
    }
    if (stopArgs.size < 2) return null
    val position = Regex("^(.*?)\\s+(-?[0-9.]+)%$")
    val colors = mutableListOf<Color>()
    val declared = mutableListOf<Float?>()
    for (stop in stopArgs) {
        val match = position.find(stop)
        val colorText = match?.groupValues?.get(1) ?: stop
        colors.add(almadarGradientColor(colorText, palette) ?: return null)
        declared.add(match?.groupValues?.get(2)?.toFloatOrNull()?.div(100f))
    }
    if (declared.first() == null) declared[0] = 0f
    if (declared.last() == null) declared[declared.lastIndex] = 1f
    var anchor = 0
    for (index in 1 until declared.size) {
        if (declared[index] == null) continue
        val gap = index - anchor
        for (between in anchor + 1 until index) {
            declared[between] = declared[anchor]!! + (declared[index]!! - declared[anchor]!!) * (between - anchor) / gap
        }
        anchor = index
    }
    return AlmadarGradient(angle, colors.indices.map { declared[it]!! to colors[it] })
}

internal fun almadarGradientBrush(gradient: AlmadarGradient, size: Size): Brush {
    val radians = Math.toRadians(gradient.angleDegrees)
    val dx = Math.sin(radians).toFloat()
    val dy = -Math.cos(radians).toFloat()
    val length = Math.abs(size.width * dx) + Math.abs(size.height * dy)
    val center = Offset(size.width / 2f, size.height / 2f)
    return Brush.linearGradient(
        *gradient.stops.toTypedArray(),
        start = Offset(center.x - dx * length / 2f, center.y - dy * length / 2f),
        end = Offset(center.x + dx * length / 2f, center.y + dy * length / 2f)
    )
}

/** The web `background-image: var(--surface-card-image)`: a gradient value is painted over the fill inside `shape`; a value this painter cannot express (a `url()` mask, a corner direction, `color-mix`) is logged and skipped, never approximated. */
@Composable
internal fun Modifier.almadarSurfaceImage(image: CssSurfaceImage, shape: Shape): Modifier {
    val css = (image as? CssSurfaceImage.Css)?.value ?: return this
    val palette = LocalAlmadarTheme.current.color
    val gradient = remember(css, palette) {
        almadarParseLinearGradient(css, palette) ?: run {
            Log.w("AlmadarSurface", "image: unsupported value skipped: $css")
            null
        }
    } ?: return this
    return this.clip(shape).drawBehind { drawRect(brush = almadarGradientBrush(gradient, size)) }
}

/** A code fence: a `mermaid` fence is drawn by the Rust diagram renderer when it can draw that source, every other fence (and a mermaid source it cannot draw) is the code block. */
@Composable
internal fun AlmadarFence(code: String, language: String, parent: JsonElement, modifier: Modifier, onEvent: (String, JsonElement?) -> Unit) {
    val diagram = remember(code) { almadarJson("code" to code) }
    val drawable = remember(code) { language == "mermaid" && orbitalSceneDrawable("mermaid", diagram) }
    if (drawable) {
        LearningCanvasRenderer(pattern = "mermaid", props = diagram, modifier = modifier, onEvent = onEvent)
    } else {
        CodeBlockRenderer(almadarJson("code" to code, "language" to language, "sourceTrait" to almadarTrait(parent)), modifier, onEvent)
    }
}

/** The web `StateIndicator`: a pill carrying the state glyph on its tone. The web's default remote sprite is not drawn natively. */
@Composable
internal fun AlmadarStateIndicator(state: String, modifier: Modifier = Modifier) {
    val palette = LocalAlmadarTheme.current.color
    val (glyph, fill) = when (state) {
        "idle" -> "⏸" to palette.muted
        "active" -> "▶" to palette.success
        "sleeping" -> "💤" to palette.muted
        "moving" -> "🚶" to palette.info
        "eating" -> "🍽️" to palette.success
        "waiting" -> "⏳" to palette.warning
        "happy" -> "😊" to palette.success
        "scared" -> "😨" to palette.error
        "done", "ready" -> "✓" to palette.success
        "error" -> "✗" to palette.error
        "cooldown" -> "🔄" to palette.warning
        else -> "?" to palette.muted
    }
    Row(
        modifier = modifier.background(fill, almadarShape(AlmadarCorner.Pill)).padding(horizontal = almadarSpaceFor("sm"), vertical = AlmadarSizes.w0_5),
        horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = glyph, style = almadarType("xs"), color = palette.foreground)
        Text(text = state, style = almadarType("xs").copy(fontWeight = FontWeight.Medium), color = palette.foreground)
    }
}

/** The web `TraitStateViewer` (linear / compact / full) over a `{name, states, currentState, transitions, description}` machine. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AlmadarTraitStateViewer(
    trait: JsonObject,
    modifier: Modifier = Modifier,
    variant: String = "compact",
    size: String = "md",
    showTransitions: Boolean = true,
    onStateClick: ((String) -> Unit)? = null
) {
    val palette = LocalAlmadarTheme.current.color
    val geometry = LocalAlmadarGeometry.current
    val states = trait.array("states").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    val current = trait.string("currentState")
    val name = trait.string("name")
    val description = trait.string("description")
    val transitions = trait.array("transitions").mapNotNull { it as? JsonObject }
    val outgoing = transitions.filter { it.string("from") == current }
    val tiny = almadarSpaceFor("xs")
    val gap = almadarSpaceFor("sm")
    val textSize = when (size) { "sm" -> "xs"; "lg" -> "base"; else -> "sm" }
    val nodeHeight = when (size) { "sm" -> almadarSpace("space8"); "lg" -> almadarSpace("space12"); else -> almadarSpace("space10") }
    val nodeWidth = when (size) { "sm" -> almadarSpace("space12"); "lg" -> AlmadarSizes.w20; else -> AlmadarSizes.w16 }
    val nodeGap = when (size) { "sm" -> almadarSpace("space2"); "lg" -> almadarSpace("space6"); else -> almadarSpace("space4") }
    val note: @Composable (String, Color) -> Unit = { text, color -> Text(text = text, style = almadarType("xs"), color = color) }
    val rule: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth().height(geometry.borderStandard).background(palette.border))
    }
    val chip: @Composable (JsonObject, Boolean) -> Unit = { transition, strong ->
        Row(
            modifier = Modifier
                .background(if (strong) palette.primary.copy(alpha = 0.1f) else palette.muted, almadarShape(AlmadarCorner.Interactive))
                .then(if (strong) almadarBorder(AlmadarCorner.Interactive, geometry.borderStandard, palette.primary.copy(alpha = 0.3f)) else Modifier)
                .padding(horizontal = gap, vertical = tiny),
            horizontalArrangement = Arrangement.spacedBy(tiny),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = transition.string("event"), style = almadarType("xs").copy(fontWeight = if (strong) FontWeight.Medium else null), color = palette.accent)
            Text(text = "→", style = almadarType("xs"), color = palette.mutedForeground)
            Text(text = transition.string("to"), style = almadarType("xs"), color = palette.success)
            if (transition.string("guardHint").isNotEmpty()) Text(text = "⚠", style = almadarType("xs"), color = palette.warning)
        }
    }
    val header: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AlmadarText(name, variant = "body2", weight = "bold")
            AlmadarStateIndicator(current)
        }
        if (description.isNotEmpty()) AlmadarText(description, variant = "caption", color = "muted")
    }
    Column(
        modifier = modifier
            .background(palette.card, almadarShape(AlmadarCorner.Container))
            .then(almadarBorder(AlmadarCorner.Container, geometry.borderStandard, palette.border))
            .padding(almadarSpaceFor("md")),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        when (variant) {
            "linear" -> {
                if (description.isNotEmpty()) note(description, palette.mutedForeground)
                val position = states.indexOf(current)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(tiny), verticalArrangement = Arrangement.spacedBy(tiny)) {
                    states.forEachIndexed { index, state ->
                        val done = index < position
                        val here = index == position
                        if (index > 0) Text(text = "→", style = almadarType("lg"), color = if (done || here) palette.primary else palette.mutedForeground)
                        Row(
                            modifier = Modifier
                                .background(if (done) palette.success.copy(alpha = 0.2f) else if (here) palette.primary.copy(alpha = 0.2f) else palette.muted, almadarShape(AlmadarCorner.Pill))
                                .then(almadarBorder(AlmadarCorner.Pill, geometry.borderHeavy, if (done) palette.success else if (here) palette.primary else palette.border))
                                .padding(horizontal = almadarSpaceFor("md"), vertical = tiny),
                            horizontalArrangement = Arrangement.spacedBy(tiny),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tone = if (done || here) palette.foreground else palette.mutedForeground
                            Text(text = if (done) "✔" else if (here) "●" else "○", style = almadarType(textSize), color = tone)
                            Text(text = state, style = almadarType(textSize).copy(fontWeight = if (here) FontWeight.Bold else null), color = tone)
                        }
                    }
                }
            }
            "full" -> {
                header()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(nodeGap), verticalArrangement = Arrangement.spacedBy(nodeGap)) {
                    states.forEach { state ->
                        val here = state == current
                        val leaves = transitions.any { it.string("from") == state }
                        Box(
                            modifier = Modifier
                                .widthIn(min = nodeWidth).height(nodeHeight)
                                .alpha(if (here || leaves) 1f else 0.6f)
                                .background(if (here) palette.primary.copy(alpha = 0.2f) else if (leaves) palette.muted else palette.background, almadarShape(AlmadarCorner.Container))
                                .then(almadarBorder(AlmadarCorner.Container, geometry.borderHeavy, if (here) palette.primary else palette.border))
                                .then(if (onStateClick != null) Modifier.clickable { onStateClick(state) } else Modifier)
                                .padding(horizontal = gap),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state, maxLines = 1, softWrap = false,
                                style = almadarType(textSize).copy(fontWeight = if (here) FontWeight.Bold else null),
                                color = if (here) palette.primary else palette.foreground.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                if (showTransitions) {
                    rule()
                    note("Transitions:", palette.mutedForeground)
                    Column(verticalArrangement = Arrangement.spacedBy(tiny)) {
                        transitions.forEach { transition ->
                            val active = transition.string("from") == current
                            FlowRow(
                                modifier = Modifier.background(if (active) palette.primary.copy(alpha = 0.1f) else palette.muted.copy(alpha = 0.5f), almadarShape(AlmadarCorner.Interactive)).padding(horizontal = gap, vertical = tiny),
                                horizontalArrangement = Arrangement.spacedBy(tiny),
                                verticalArrangement = Arrangement.spacedBy(tiny)
                            ) {
                                Text(text = transition.string("from"), style = almadarType("xs"), color = if (active) palette.primary else palette.mutedForeground)
                                Text(text = "—[", style = almadarType("xs"), color = palette.mutedForeground)
                                Text(text = transition.string("event"), style = almadarType("xs").copy(fontWeight = FontWeight.Medium), color = palette.accent)
                                Text(text = "]→", style = almadarType("xs"), color = palette.mutedForeground)
                                Text(text = transition.string("to"), style = almadarType("xs"), color = if (active) palette.success else palette.foreground.copy(alpha = 0.7f))
                                if (transition.string("guardHint").isNotEmpty()) Text(text = "⚠ " + transition.string("guardHint"), style = almadarType("xs"), color = palette.warning)
                            }
                        }
                    }
                    if (outgoing.isNotEmpty()) {
                        rule()
                        note("Available now:", palette.primary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(tiny), verticalArrangement = Arrangement.spacedBy(tiny)) {
                            outgoing.forEach { chip(it, true) }
                        }
                    }
                }
            }
            else -> {
                header()
                if (outgoing.isNotEmpty()) {
                    rule()
                    note("Available actions:", palette.mutedForeground)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(tiny), verticalArrangement = Arrangement.spacedBy(tiny)) {
                        outgoing.forEach { chip(it, false) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AlmadarModalFrame(
    props: JsonElement, title: String, closeEvent: String, showClose: Boolean, look: String, cap: Dp, floor: Dp?, footer: JsonElement?,
    modifier: Modifier, onEvent: (String, JsonElement?) -> Unit, onClose: (() -> Unit)? = null, content: @Composable () -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val screen = LocalConfiguration.current.screenHeightDp
        Box(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", title)), contentAlignment = Alignment.TopCenter) {
        AlmadarPanel(
            material = true,
            modifier = (if (look == "full-screen") Modifier.fillMaxSize() else Modifier.widthIn(min = floor?.let { minOf(it, LocalConfiguration.current.screenWidthDp.dp) } ?: Dp.Unspecified, max = cap).fillMaxWidth()).almadarSurfaceEnter("modalEnter", AlmadarEnterTiming.Modal)
        ) {
            if (title.isNotEmpty() || showClose) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = almadarSpaceFor("lg"), vertical = almadarSpaceFor("md")),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title.isNotEmpty()) AlmadarText(title, modifier = Modifier.weight(1f), variant = "h4")
                    else Spacer(modifier = Modifier.weight(1f))
                    if (showClose) {
                        if (onClose != null) AlmadarLocalButton(props, "", onClose, variant = "ghost", size = "sm", icon = "x", ariaLabel = "Close modal") else AlmadarButton(props, "", closeEvent, onEvent, variant = "ghost", size = "sm", icon = "x", ariaLabel = "Close modal")
                    }
                }
                AlmadarDivider()
            }
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = (screen * 0.8f).dp).verticalScroll(rememberScrollState()).padding(almadarSpaceFor("lg")),
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
            ) {
                content()
            }
            if (footer != null && footer !is JsonNull) {
                AlmadarDivider()
                Box(modifier = Modifier.fillMaxWidth().background(palette.muted).padding(horizontal = almadarSpaceFor("lg"), vertical = almadarSpaceFor("md"))) {
                    AlmadarNode(footer, onEvent, Modifier.fillMaxWidth())
                }
            }
        }
        }
}

@Composable
internal fun almadarModalCap(look: String, size: String): Dp =
    if (look != "centered-card" || LocalConfiguration.current.screenWidthDp.dp < AlmadarBreakpoints.sm) Dp.Infinity else when (size) {
        "sm" -> AlmadarSizes.maxWMd
        "lg" -> AlmadarSizes.maxW3xl
        "xl" -> AlmadarSizes.maxW5xl
        "full" -> Dp.Infinity
        else -> AlmadarSizes.maxW2xl
    }

@Composable
internal fun almadarModalFloor(look: String, size: String): Dp? =
    if (look != "centered-card" || LocalConfiguration.current.screenWidthDp.dp < AlmadarBreakpoints.sm) null else when (size) {
        "sm" -> AlmadarSizes.minWModalSm
        "lg" -> AlmadarSizes.minWModalLg
        "xl" -> AlmadarSizes.minWModalXl
        "full" -> null
        else -> AlmadarSizes.minWModalMd
    }


@Composable
internal fun AlmadarHintText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = almadarType("xs").copy(fontStyle = FontStyle.Italic), color = LocalAlmadarTheme.current.color.mutedForeground)
}


internal class AlmadarRoute(val event: String, val payload: JsonElement?)

internal fun almadarNavigate(item: JsonObject): AlmadarRoute {
    val event = item.string("event", item.string("action"))
    if (event.isNotEmpty()) return AlmadarRoute(event, null)
    val href = item.string("href", item.string("path"))
    if (href.isEmpty()) return AlmadarRoute("", null)
    return AlmadarRoute(NAVIGATE_EVENT, almadarJson("href" to href, "url" to href))
}

internal class AlmadarDiffLine(val type: String, val before: Int?, val after: Int?, val content: String)

internal fun almadarLineDiff(before: String, after: String): List<AlmadarDiffLine> {
    val a = if (before.isEmpty()) emptyList() else before.split("\n")
    val b = if (after.isEmpty()) emptyList() else after.split("\n")
    val table = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in a.indices.reversed()) {
        for (j in b.indices.reversed()) {
            table[i][j] = if (a[i] == b[j]) table[i + 1][j + 1] + 1 else maxOf(table[i + 1][j], table[i][j + 1])
        }
    }
    val result = mutableListOf<AlmadarDiffLine>()
    var i = 0
    var j = 0
    while (i < a.size && j < b.size) {
        if (a[i] == b[j]) {
            result.add(AlmadarDiffLine("unchanged", i + 1, j + 1, a[i]))
            i++
            j++
        } else if (table[i + 1][j] >= table[i][j + 1]) {
            result.add(AlmadarDiffLine("removed", i + 1, null, a[i]))
            i++
        } else {
            result.add(AlmadarDiffLine("added", null, j + 1, b[j]))
            j++
        }
    }
    while (i < a.size) { result.add(AlmadarDiffLine("removed", i + 1, null, a[i])); i++ }
    while (j < b.size) { result.add(AlmadarDiffLine("added", null, j + 1, b[j])); j++ }
    return result
}

internal class AlmadarMdBlock(
    val kind: String,
    val level: Int,
    val text: String,
    val items: List<String>,
    val ordered: Boolean,
    val rows: List<List<String>>
)

private val almadarMdHeading = Regex("^(#{1,6})\\s+(.*)$")
private val almadarMdRule = Regex("^(-{3,}|\\*{3,}|_{3,})$")
private val almadarMdBullet = Regex("^[-*+]\\s+(.*)$")
private val almadarMdNumbered = Regex("^\\d+[.)]\\s+(.*)$")
private val almadarMdSeparator = Regex("^\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?$")

private fun almadarMdCells(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

internal fun almadarParseMarkdown(source: String): List<AlmadarMdBlock> {
    val lines = source.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<AlmadarMdBlock>()
    val paragraph = mutableListOf<String>()
    val flush = {
        if (paragraph.isNotEmpty()) {
            blocks.add(AlmadarMdBlock("paragraph", 0, paragraph.joinToString(" "), emptyList(), false, emptyList()))
            paragraph.clear()
        }
    }
    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        val heading = almadarMdHeading.matchEntire(trimmed)
        if (trimmed.isEmpty()) {
            flush()
            i++
        } else if (trimmed.startsWith("```")) {
            flush()
            val language = trimmed.removePrefix("```").trim()
            val code = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) { code.add(lines[i]); i++ }
            i++
            blocks.add(AlmadarMdBlock("code", 0, code.joinToString("\n"), listOf(language), false, emptyList()))
        } else if (heading != null) {
            flush()
            blocks.add(AlmadarMdBlock("heading", heading.groupValues[1].length, heading.groupValues[2], emptyList(), false, emptyList()))
            i++
        } else if (almadarMdRule.matches(trimmed)) {
            flush()
            blocks.add(AlmadarMdBlock("rule", 0, "", emptyList(), false, emptyList()))
            i++
        } else if (trimmed.startsWith("![") && trimmed.endsWith(")") && trimmed.contains("](")) {
            flush()
            blocks.add(AlmadarMdBlock("image", 0, trimmed, emptyList(), false, emptyList()))
            i++
        } else if (trimmed.startsWith(">")) {
            flush()
            val quote = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) { quote.add(lines[i].trim().removePrefix(">").trim()); i++ }
            blocks.add(AlmadarMdBlock("quote", 0, quote.joinToString(" "), emptyList(), false, emptyList()))
        } else if (almadarMdBullet.matches(trimmed) || almadarMdNumbered.matches(trimmed)) {
            flush()
            val ordered = almadarMdNumbered.matches(trimmed)
            val entries = mutableListOf<String>()
            while (i < lines.size) {
                val line = lines[i].trim()
                val match = (if (ordered) almadarMdNumbered else almadarMdBullet).matchEntire(line) ?: break
                entries.add(match.groupValues[1])
                i++
            }
            blocks.add(AlmadarMdBlock("list", 0, "", entries, ordered, emptyList()))
        } else if (trimmed.startsWith("|") && i + 1 < lines.size && almadarMdSeparator.matches(lines[i + 1].trim())) {
            flush()
            val rows = mutableListOf(almadarMdCells(trimmed))
            i += 2
            while (i < lines.size && lines[i].trim().startsWith("|")) { rows.add(almadarMdCells(lines[i])); i++ }
            blocks.add(AlmadarMdBlock("table", 0, "", emptyList(), false, rows))
        } else {
            paragraph.add(trimmed)
            i++
        }
    }
    flush()
    return blocks
}

private fun almadarAppendInline(builder: AnnotatedString.Builder, text: String, code: SpanStyle, link: SpanStyle) {
    val plain = StringBuilder()
    val flush = { if (plain.isNotEmpty()) { builder.append(plain.toString()); plain.clear() } }
    var i = 0
    while (i < text.length) {
        var consumed = false
        if (text.startsWith("**", i) || text.startsWith("__", i)) {
            val end = text.indexOf(text.substring(i, i + 2), i + 2)
            if (end > i + 2) {
                flush()
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                almadarAppendInline(builder, text.substring(i + 2, end), code, link)
                builder.pop()
                i = end + 2
                consumed = true
            }
        } else if (text.startsWith("~~", i)) {
            val end = text.indexOf("~~", i + 2)
            if (end > i + 2) {
                flush()
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                almadarAppendInline(builder, text.substring(i + 2, end), code, link)
                builder.pop()
                i = end + 2
                consumed = true
            }
        } else if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i + 1) {
                flush()
                builder.pushStyle(code)
                builder.append(text.substring(i + 1, end))
                builder.pop()
                i = end + 1
                consumed = true
            }
        } else if (text[i] == '*') {
            val end = text.indexOf('*', i + 1)
            if (end > i + 1) {
                flush()
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                almadarAppendInline(builder, text.substring(i + 1, end), code, link)
                builder.pop()
                i = end + 1
                consumed = true
            }
        } else if (text[i] == '[') {
            val close = text.indexOf("](", i + 1)
            val end = if (close > i) text.indexOf(')', close + 2) else -1
            if (end > close) {
                flush()
                val start = builder.length
                builder.pushStyle(link)
                almadarAppendInline(builder, text.substring(i + 1, close), code, link)
                builder.pop()
                builder.addStringAnnotation("url", text.substring(close + 2, end), start, builder.length)
                i = end + 1
                consumed = true
            }
        }
        if (!consumed) {
            plain.append(text[i])
            i++
        }
    }
    flush()
}

@Composable
internal fun AlmadarInlineText(
    text: String,
    modifier: Modifier = Modifier,
    sizeKey: String = "base",
    weight: FontWeight? = null,
    isHeading: Boolean = false,
    annotations: List<JsonObject> = emptyList(),
    onAnnotation: (String) -> Unit = {},
    onTap: (() -> Unit)? = null
) {
    val theme = LocalAlmadarTheme.current
    val palette = theme.color
    val uriHandler = LocalUriHandler.current
    val code = SpanStyle(background = palette.muted, color = palette.foreground, fontFamily = theme.typeScale.monoFamily)
    val link = SpanStyle(color = palette.primary, textDecoration = TextDecoration.Underline)
    val parsed = AnnotatedString.Builder().also { almadarAppendInline(it, text, code, link) }.toAnnotatedString()
    val styled = AnnotatedString.Builder(parsed).also { builder ->
        annotations.forEach { annotation ->
            val passage = annotation.string("text")
            val at = if (passage.isEmpty()) -1 else parsed.text.indexOf(passage)
            if (at >= 0) {
                val tint = if (annotation.string("kind") == "question") palette.warning else palette.info
                builder.addStyle(SpanStyle(background = tint.copy(alpha = 0.25f)), at, at + passage.length)
                builder.addStringAnnotation("annotation", annotation.string("id"), at, at + passage.length)
            }
        }
    }.toAnnotatedString()
    ClickableText(
        text = styled,
        style = almadarType(sizeKey).copy(color = palette.foreground, fontWeight = weight),
        modifier = if (isHeading) modifier.semantics { heading() } else modifier,
        onClick = { offset ->
            val annotated = styled.getStringAnnotations("annotation", offset, offset).firstOrNull()
            val url = styled.getStringAnnotations("url", offset, offset).firstOrNull()
            if (annotated != null) onAnnotation(annotated.item)
            else if (url != null) uriHandler.openUri(url.item)
            else onTap?.invoke()
        }
    )
}

@Composable
internal fun AlmadarNode(
    node: JsonElement?,
    onEvent: (String, JsonElement?) -> Unit,
    modifier: Modifier = Modifier,
    variant: String = "body1",
    weight: String = "none"
) {
    when (node) {
        is JsonPrimitive -> AlmadarText(node.contentOrNull ?: "", modifier, variant, weight = weight)
        is JsonObject -> RenderChild(node, modifier, onEvent)
        is JsonArray -> Column(modifier = modifier) { node.forEach { AlmadarNode(it, onEvent, Modifier, variant, weight) } }
        else -> Unit
    }
}

private fun almadarIsBinding(text: String): Boolean = text.startsWith("@")

internal fun almadarSExpr(expr: JsonElement?): String = when (expr) {
    null, is JsonNull -> "null"
    is JsonPrimitive -> if (expr.isString) {
        val text = expr.content
        if (almadarIsBinding(text)) text else "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    } else expr.content
    is JsonArray -> {
        val head = (expr.firstOrNull() as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (head != null && !almadarIsBinding(head)) "(" + (listOf(head) + expr.drop(1).map { almadarSExpr(it) }).joinToString(" ") + ")"
        else "[" + expr.joinToString(" ") { almadarSExpr(it) } + "]"
    }
    is JsonObject -> if (expr.isEmpty()) "{}" else "{ " + expr.entries.joinToString(", ") { it.key + ": " + almadarSExpr(it.value) } + " }"
}

internal class AlmadarChartSeries(
    val name: String,
    val labels: List<String>,
    val values: List<Float>,
    val color: Color,
    val dashed: Boolean
)

@Composable
internal fun AlmadarLineCanvas(
    series: List<AlmadarChartSeries>,
    low: Float,
    high: Float,
    height: Dp,
    fill: Boolean,
    gridColor: Color,
    lineWidth: Dp,
    onPick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = series.maxOfOrNull { it.values.size } ?: 0
    val span = if (high > low) high - low else 1f
    Canvas(
        modifier = modifier.fillMaxWidth().height(height).pointerInput(series, low, high) {
            detectTapGestures { offset ->
                if (count > 0) {
                    val column = if (count == 1) 0 else Math.round(offset.x / size.width * (count - 1)).coerceIn(0, count - 1)
                    var best = -1
                    var gap = Float.MAX_VALUE
                    series.forEachIndexed { index, line ->
                        val value = line.values.getOrNull(column)
                        if (value != null) {
                            val y = size.height - (value - low) / span * size.height
                            val distance = Math.abs(y - offset.y)
                            if (distance < gap) { gap = distance; best = index }
                        }
                    }
                    if (best >= 0) onPick(best, column)
                }
            }
        }
    ) {
        val stroke = lineWidth.toPx()
        for (step in 0..3) {
            val y = size.height * step / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke / 2f)
        }
        series.forEach { line ->
            val path = androidx.compose.ui.graphics.Path()
            val last = line.values.size - 1
            line.values.forEachIndexed { index, value ->
                val x = if (last <= 0) size.width / 2f else size.width * index / last
                val y = size.height - (value - low) / span * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            if (fill && last > 0) {
                val area = androidx.compose.ui.graphics.Path()
                area.addPath(path)
                area.lineTo(size.width, size.height)
                area.lineTo(0f, size.height)
                area.close()
                drawPath(area, color = line.color.copy(alpha = 0.15f))
            }
            drawPath(
                path, color = line.color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke * 1.5f,
                    pathEffect = if (line.dashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(stroke * 4f, stroke * 3f)) else null
                )
            )
            line.values.forEachIndexed { index, value ->
                val x = if (last <= 0) size.width / 2f else size.width * index / last
                val y = size.height - (value - low) / span * size.height
                drawCircle(line.color, radius = stroke * 2f, center = Offset(x, y))
            }
        }
    }
}

@Composable
internal fun AlmadarPieCanvas(
    values: List<Float>,
    colors: List<Color>,
    donut: Boolean,
    diameter: Dp,
    ringWidth: Dp,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = values.sum().let { if (it > 0f) it else 1f }
    Canvas(
        modifier = modifier.size(diameter).pointerInput(values) {
            detectTapGestures { offset ->
                val dx = offset.x - size.width / 2f
                val dy = offset.y - size.height / 2f
                var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                if (angle < 0f) angle += 360f
                var start = 0f
                values.forEachIndexed { index, value ->
                    val sweep = value / total * 360f
                    if (angle >= start && angle < start + sweep) onPick(index)
                    start += sweep
                }
            }
        }
    ) {
        var start = -90f
        values.forEachIndexed { index, value ->
            val sweep = value / total * 360f
            val color = colors[index % colors.size]
            if (donut) {
                val inset = ringWidth.toPx() / 2f
                drawArc(
                    color, start, sweep, false,
                    topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(this.size.width - inset * 2f, this.size.height - inset * 2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = ringWidth.toPx())
                )
            } else {
                drawArc(color, start, sweep, true)
            }
            start += sweep
        }
    }
}

@Composable
internal fun AlmadarScatterCanvas(
    xs: List<Float>,
    ys: List<Float>,
    radii: List<Float>,
    colors: List<Color>,
    height: Dp,
    gridColor: Color,
    lineWidth: Dp,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val left = xs.minOrNull() ?: 0f
    val right = (xs.maxOrNull() ?: 1f).let { if (it > left) it else left + 1f }
    val bottom = ys.minOrNull() ?: 0f
    val top = (ys.maxOrNull() ?: 1f).let { if (it > bottom) it else bottom + 1f }
    Canvas(
        modifier = modifier.fillMaxWidth().height(height).pointerInput(xs, ys) {
            detectTapGestures { offset ->
                var best = -1
                var gap = Float.MAX_VALUE
                xs.indices.forEach { index ->
                    val x = (xs[index] - left) / (right - left) * size.width
                    val y = size.height - (ys[index] - bottom) / (top - bottom) * size.height
                    val distance = Math.hypot((x - offset.x).toDouble(), (y - offset.y).toDouble()).toFloat()
                    if (distance < gap) { gap = distance; best = index }
                }
                if (best >= 0) onPick(best)
            }
        }
    ) {
        val stroke = lineWidth.toPx()
        drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = stroke)
        drawLine(gridColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = stroke)
        xs.indices.forEach { index ->
            val x = (xs[index] - left) / (right - left) * size.width
            val y = size.height - (ys[index] - bottom) / (top - bottom) * size.height
            drawCircle(colors[index % colors.size].copy(alpha = 0.8f), radius = radii[index], center = Offset(x, y))
        }
    }
}

internal fun almadarTimeLabel(raw: String, period: String): String {
    val instant = try {
        if (period.isNotEmpty() && raw.matches(Regex("^-?\\d+$"))) java.time.Instant.ofEpochMilli(raw.toLong())
        else if (raw.length <= 10) java.time.LocalDate.parse(raw).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        else java.time.Instant.parse(raw)
    } catch (failure: java.time.format.DateTimeParseException) {
        null
    } ?: return raw
    val date = instant.atZone(java.time.ZoneOffset.UTC).toLocalDate()
    fun pattern(text: String) = java.time.format.DateTimeFormatter.ofPattern(text, java.util.Locale.getDefault()).format(date)
    return when (period) {
        "day" -> pattern("MMM d")
        "week" -> "Week of " + pattern("MMM d")
        "quarter" -> "Q" + ((date.monthValue - 1) / 3 + 1) + " " + date.year
        "year" -> date.year.toString()
        else -> pattern("MMM yyyy")
    }
}

@Composable
internal fun AlmadarLocalInput(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    helper: String = "",
    error: String = "",
    inputType: String = "text",
    disabled: Boolean = false,
    ariaLabel: String = ""
) {
    InputRenderer(
        almadarJson(
            "value" to value, "label" to label, "placeholder" to placeholder, "helperText" to helper, "error" to error,
            "disabled" to disabled, "inputType" to inputType, "aria-label" to ariaLabel,
            "onChange" to ALMADAR_LOCAL_ACTION, "local" to true
        ),
        modifier
    ) { _, envelope -> onChange(envelope?.jsonObject("actionPayload")?.string("value", "") ?: "") }
}

@Composable
internal fun AlmadarRadio(
    parent: JsonElement,
    label: String,
    selected: Boolean,
    event: String,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    ariaLabel: String = ""
) {
    RadioRenderer(
        almadarJson(
            "label" to label, "value" to (if (selected) label else ""), "action" to event.ifEmpty { ALMADAR_LOCAL_ACTION },
            "disabled" to disabled, "aria-label" to ariaLabel, "local" to event.isEmpty(), "sourceTrait" to almadarTrait(parent)
        ),
        modifier
    ) { _, _ -> onPick() }
}

internal fun almadarParseDateTime(raw: String): java.time.LocalDateTime? {
    if (raw.isEmpty()) return null
    return try {
        java.time.OffsetDateTime.parse(raw).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime()
    } catch (offsetFailure: java.time.format.DateTimeParseException) {
        try {
            java.time.LocalDateTime.parse(raw)
        } catch (localFailure: java.time.format.DateTimeParseException) {
            try {
                java.time.LocalDate.parse(raw).atStartOfDay()
            } catch (dateFailure: java.time.format.DateTimeParseException) {
                null
            }
        }
    }
}

internal fun almadarWeekStart(date: java.time.LocalDate): java.time.LocalDate =
    date.minusDays((date.dayOfWeek.value - 1).toLong())

internal class AlmadarAnchoredPosition(private val position: String) : androidx.compose.ui.window.PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val centeredY = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
        val x = when (position) { "left" -> anchorBounds.left - popupContentSize.width; "right" -> anchorBounds.right; else -> centeredX }
        val y = when (position) { "bottom" -> anchorBounds.bottom; "left", "right" -> centeredY; else -> anchorBounds.top - popupContentSize.height }
        return IntOffset(
            x.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width)),
            y.coerceIn(0, maxOf(0, windowSize.height - popupContentSize.height))
        )
    }
}

@Composable
internal fun AlmadarColoredText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    sizeKey: String = "base",
    weight: FontWeight? = null,
    align: TextAlign? = null,
    heading: Boolean = false
) {
    val base = almadarType(sizeKey)
    val voiced = almadarHeadingStyle(base)
    val style = if (heading) (if (weight != null) voiced.copy(fontWeight = weight) else voiced) else base.copy(fontWeight = weight ?: base.fontWeight)
    Text(text = if (heading) almadarHeadingText(text) else text, modifier = modifier, color = color, style = style, textAlign = align)
}

internal enum class AlmadarCorner { None, Container, Interactive, Pill }

internal enum class AlmadarFlatCorner { TopLeft, TopRight, BottomRight, BottomLeft }

@Composable
internal fun almadarShape(corner: AlmadarCorner, inset: Dp? = null, flat: AlmadarFlatCorner? = null): Shape {
    val geometry = LocalAlmadarGeometry.current
    val base = when (corner) {
        AlmadarCorner.None -> geometry.radiusContainer * 0f
        AlmadarCorner.Container -> geometry.radiusContainer
        AlmadarCorner.Interactive -> geometry.radiusInteractive
        AlmadarCorner.Pill -> geometry.radiusPill
    }
    val kind = when (corner) {
        AlmadarCorner.None -> geometry.cornerShape
        AlmadarCorner.Container -> geometry.cornerShape
        AlmadarCorner.Interactive -> geometry.cornerShapeInteractive
        AlmadarCorner.Pill -> geometry.cornerShapePill
    }
    val radius = if (inset == null) base else (base - inset).coerceAtLeast(base * 0f)
    val radiusPx = with(LocalDensity.current) { radius.toPx() }
    val square = radius * 0f
    val topStart = if (flat == AlmadarFlatCorner.TopLeft) square else radius
    val topEnd = if (flat == AlmadarFlatCorner.TopRight) square else radius
    val bottomEnd = if (flat == AlmadarFlatCorner.BottomRight) square else radius
    val bottomStart = if (flat == AlmadarFlatCorner.BottomLeft) square else radius
    return when (kind) {
        is CssCornerShape.Round -> RoundedCornerShape(topStart = topStart, topEnd = topEnd, bottomEnd = bottomEnd, bottomStart = bottomStart)
        is CssCornerShape.Bevel -> CutCornerShape(topStart = topStart, topEnd = topEnd, bottomEnd = bottomEnd, bottomStart = bottomStart)
        else -> remember(kind, radiusPx, flat) {
            GenericShape { size, _ ->
                val r = minOf(radiusPx, size.minDimension / 2f)
                val k: Double? = when (kind) {
                    is CssCornerShape.Squircle -> 2.0
                    is CssCornerShape.Scoop -> -1.0
                    is CssCornerShape.Superellipse -> kind.exponent
                    else -> null
                }
                val steps = 24
                val local: List<Offset> = if (k == null) {
                    listOf(Offset(r, 0f), Offset(r, r), Offset(0f, r))
                } else {
                    val exponent = Math.pow(2.0, Math.abs(k))
                    (0..steps).map { i ->
                        val t = (Math.PI / 2.0) * (1.0 - i.toDouble() / steps)
                        val x = r * (1.0 - Math.pow(Math.cos(t), 2.0 / exponent)).toFloat()
                        val y = r * (1.0 - Math.pow(Math.sin(t), 2.0 / exponent)).toFloat()
                        if (k < 0.0) Offset(r - y, r - x) else Offset(x, y)
                    }
                }
                var started = false
                fun addCorner(flipX: Boolean, flipY: Boolean, reversed: Boolean, isFlat: Boolean) {
                    (if (isFlat) listOf(Offset(0f, 0f)) else if (reversed) local.asReversed() else local).forEach { p ->
                        val gx = if (flipX) size.width - p.x else p.x
                        val gy = if (flipY) size.height - p.y else p.y
                        if (started) lineTo(gx, gy) else { moveTo(gx, gy); started = true }
                    }
                }
                addCorner(flipX = false, flipY = false, reversed = true, isFlat = flat == AlmadarFlatCorner.TopLeft)
                addCorner(flipX = true, flipY = false, reversed = false, isFlat = flat == AlmadarFlatCorner.TopRight)
                addCorner(flipX = true, flipY = true, reversed = true, isFlat = flat == AlmadarFlatCorner.BottomRight)
                addCorner(flipX = false, flipY = true, reversed = false, isFlat = flat == AlmadarFlatCorner.BottomLeft)
                close()
            }
        }
    }
}

@Composable
internal fun almadarBorder(corner: AlmadarCorner, width: Dp, color: Color, hostDrawsSolid: Boolean = false, lineFrom: AlmadarCorner = corner): Modifier {
    val geometry = LocalAlmadarGeometry.current
    val style = if (lineFrom == AlmadarCorner.Interactive) geometry.borderStyleInteractive else geometry.borderStyle
    val half = almadarShape(corner, width / 2f)
    val sixth = almadarShape(corner, width / 6f)
    val fiveSixths = almadarShape(corner, width * 5f / 6f)
    val quarter = almadarShape(corner, width / 4f)
    val threeQuarters = almadarShape(corner, width * 3f / 4f)
    val layoutDirection = LocalLayoutDirection.current
    val ground = LocalAlmadarTheme.current.color.background
    val widthPx = with(LocalDensity.current) { width.toPx() }
    if (style == CssBorderStyle.None || style == CssBorderStyle.Hidden || widthPx <= 0f) return Modifier
    if (hostDrawsSolid && style == CssBorderStyle.Solid) return Modifier
    return Modifier.drawWithContent {
        drawContent()
        fun ring(shape: Shape, inset: Float): Path {
            val path = Path()
            path.addOutline(shape.createOutline(Size(size.width - 2f * inset, size.height - 2f * inset), layoutDirection, this))
            path.translate(Offset(inset, inset))
            return path
        }
        when (style) {
            CssBorderStyle.Dashed -> drawPath(ring(half, widthPx / 2f), color, style = Stroke(width = widthPx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(widthPx * 3f, widthPx * 3f))))
            CssBorderStyle.Dotted -> drawPath(ring(half, widthPx / 2f), color, style = Stroke(width = widthPx, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(widthPx * 0.01f, widthPx * 2f))))
            CssBorderStyle.Double -> {
                drawPath(ring(sixth, widthPx / 6f), color, style = Stroke(width = widthPx / 3f))
                drawPath(ring(fiveSixths, widthPx * 5f / 6f), color, style = Stroke(width = widthPx / 3f))
            }
            CssBorderStyle.Groove -> {
                drawPath(ring(quarter, widthPx / 4f), color, style = Stroke(width = widthPx / 2f))
                drawPath(ring(threeQuarters, widthPx * 3f / 4f), ground, style = Stroke(width = widthPx / 2f))
            }
            CssBorderStyle.Ridge -> {
                drawPath(ring(quarter, widthPx / 4f), ground, style = Stroke(width = widthPx / 2f))
                drawPath(ring(threeQuarters, widthPx * 3f / 4f), color, style = Stroke(width = widthPx / 2f))
            }
            CssBorderStyle.Inset -> drawPath(ring(half, widthPx / 2f), almadarSplitBrush(color, ground, size), style = Stroke(width = widthPx))
            CssBorderStyle.Outset -> drawPath(ring(half, widthPx / 2f), almadarSplitBrush(ground, color, size), style = Stroke(width = widthPx))
            else -> drawPath(ring(half, widthPx / 2f), color, style = Stroke(width = widthPx))
        }
    }
}

/** CSS inset/outset: the top-left half of the ring in `first`, the bottom-right half in `second`, split on the diagonal. */
internal fun almadarSplitBrush(first: Color, second: Color, size: Size): Brush =
    Brush.linearGradient(0f to first, 0.5f to first, 0.5f to second, 1f to second, start = Offset.Zero, end = Offset(size.width, size.height))

@Composable
internal fun almadarInteractiveShadow(source: InteractionSource, corner: AlmadarCorner): Modifier {
    val pressed by source.collectIsPressedAsState()
    val elevation = LocalAlmadarElevation.current
    val layer = if (pressed) elevation.pressedElevation else elevation.interactiveElevation
    return Modifier.shadow(layer.radius, almadarShape(corner), ambientColor = layer.color, spotColor = layer.color)
}

@Composable
internal fun almadarHeadingStyle(base: TextStyle): TextStyle {
    val typeScale = LocalAlmadarTheme.current.typeScale
    val voice = typeScale.heading
    return base.copy(
        fontFamily = typeScale.displayFamily,
        fontWeight = voice.weight,
        fontStyle = if (voice.style == CssFontSlant.Normal) FontStyle.Normal else FontStyle.Italic,
        letterSpacing = if (voice.tracking.isEm) voice.tracking.value.em else voice.tracking.value.sp,
        shadow = voice.shadow?.let { Shadow(color = it.color, offset = Offset(it.x.value, it.y.value), blurRadius = it.radius.value) }
    )
}

@Composable
internal fun almadarHeadingText(text: String): String = when (LocalAlmadarTheme.current.typeScale.heading.transform) {
    CssTextTransform.Uppercase -> text.uppercase()
    CssTextTransform.Lowercase -> text.lowercase()
    CssTextTransform.Capitalize -> text.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    CssTextTransform.None -> text
}

internal class AlmadarFxParticle(
    val palette: Int,
    val left: Float,
    val delay: Float,
    val angle: Float,
    val distance: Float,
    val rotation: Float,
    val size: Float
)

internal fun almadarFxHash(seed: String, salt: Int): Float {
    var h: Int = (2166136261L.toInt()) xor salt
    for (unit in seed) {
        h = h xor unit.code
        h *= 16777619
    }
    h = h xor (h ushr 13)
    h *= 1274126177
    h = h xor (h ushr 16)
    return ((h.toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0).toFloat()
}

internal fun almadarConfetti(count: Int, seed: String): List<AlmadarFxParticle> =
    (0 until maxOf(count, 0)).map { i ->
        AlmadarFxParticle(
            palette = (almadarFxHash(seed, i * 7 + 1) * 6f).toInt(),
            left = 30f + almadarFxHash(seed, i * 7 + 2) * 40f,
            delay = almadarFxHash(seed, i * 7 + 3) * 300f,
            angle = almadarFxHash(seed, i * 7 + 4) * 360f,
            distance = 40f + almadarFxHash(seed, i * 7 + 5) * 80f,
            rotation = almadarFxHash(seed, i * 7 + 6) * 720f - 360f,
            size = 4f + almadarFxHash(seed, i * 7 + 7) * 6f
        )
    }

internal fun almadarFxBezier(x: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    if (x <= 0f) return 0f
    if (x >= 1f) return 1f
    var low = 0f
    var high = 1f
    repeat(24) {
        val s = (low + high) / 2f
        val u = 1f - s
        val bx = 3f * u * u * s * x1 + 3f * u * s * s * x2 + s * s * s
        if (bx < x) low = s else high = s
    }
    val s = (low + high) / 2f
    val u = 1f - s
    return 3f * u * u * s * y1 + 3f * u * s * s * y2 + s * s * s
}

internal fun almadarFxEaseOut(x: Float): Float = almadarFxBezier(x, 0f, 0f, 0.58f, 1f)

internal fun almadarFxEaseInOut(x: Float): Float = almadarFxBezier(x, 0.42f, 0f, 0.58f, 1f)

internal fun almadarFxSegment(t: Float, from: Float, to: Float): Float =
    almadarFxEaseInOut(((t - from) / (to - from)).coerceIn(0f, 1f))

@Composable
internal fun almadarFxColor(palette: Int): Color {
    val colors = LocalAlmadarTheme.current.color
    return when (palette) {
        0 -> colors.primary
        1 -> colors.success
        2 -> colors.warning
        3 -> colors.error
        4 -> Color(0xFFFFD700)
        else -> Color(0xFF1E90FF)
    }
}

@Composable
internal fun AlmadarConfettiBurst(
    particles: List<AlmadarFxParticle>,
    elapsed: () -> Float,
    lifeMs: Float,
    reach: Float,
    tint: Color?,
    anchorX: Float?,
    anchorY: Float,
    minDurationMs: Float,
    modifier: Modifier = Modifier
) {
    val colors = particles.map { almadarFxColor(it.palette) }
    val corner = LocalAlmadarGeometry.current.radiusInteractive.value / 2f
    Canvas(modifier = modifier) {
        val elapsedMs = elapsed()
        particles.forEachIndexed { index, p ->
            val local = elapsedMs - p.delay
            if (local >= 0f) {
                val t = (local / maxOf(lifeMs - p.delay, minDurationMs)).coerceAtMost(1f)
                val travel = almadarFxEaseOut(t)
                val radians = p.angle * (kotlin.math.PI.toFloat() / 180f)
                val tx = kotlin.math.cos(radians) * p.distance * reach * density
                val ty = (kotlin.math.sin(radians) * p.distance * reach - 20f) * density
                val originX = if (anchorX != null) anchorX * size.width + (p.left - 50f) * 2f * density else p.left / 100f * size.width
                val originY = if (anchorX != null) anchorY * size.height - 10f * density else size.height / 2f
                val side = p.size * density
                val fade = if (t < 0.7f) 1f else 1f - almadarFxEaseOut((t - 0.7f) / 0.3f)
                val center = Offset(originX + side / 2f + tx * travel, originY + side / 2f + ty * travel)
                withTransform({
                    rotate(p.rotation * travel, center)
                    scale(1f - 0.5f * travel, center)
                }) {
                    drawRoundRect(
                        color = (tint ?: colors[index]).copy(alpha = fade),
                        topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                        size = Size(side, side),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner * density)
                    )
                }
            }
        }
    }
}

@Composable
internal fun almadarCssColor(raw: String, fallback: Color): Color {
    val colors = LocalAlmadarTheme.current.color
    val prefix = "var(--color-"
    if (raw.startsWith(prefix) && raw.endsWith(")")) {
        return when (raw.removePrefix(prefix).removeSuffix(")")) {
            "primary" -> colors.primary
            "secondary" -> colors.secondary
            "accent" -> colors.accent
            "foreground" -> colors.foreground
            "muted-foreground" -> colors.mutedForeground
            "success" -> colors.success
            "warning" -> colors.warning
            "error" -> colors.error
            "info" -> colors.info
            "border" -> colors.border
            else -> colors.primary
        }
    }
    if (raw.startsWith("#") || raw.startsWith("rgb") || raw.startsWith("hsl")) return parseCssColor(raw, fallback)
    return fallback
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.almadarDragSource(payload: String): Modifier =
    this.dragAndDropSource {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                startTransfer(DragAndDropTransferData(clipData = android.content.ClipData.newPlainText("almadar-slot-item", payload)))
            },
            onDrag = { _, _ -> }
        )
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.almadarDropTarget(enabled: Boolean, onHover: (Boolean) -> Unit, onDrop: (String) -> Unit): Modifier {
    val hover = rememberUpdatedState(onHover)
    val drop = rememberUpdatedState(onDrop)
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { hover.value(true) }
            override fun onExited(event: DragAndDropEvent) { hover.value(false) }
            override fun onEnded(event: DragAndDropEvent) { hover.value(false) }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                hover.value(false)
                val text = event.toAndroidDragEvent().clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString() ?: return false
                drop.value(text)
                return true
            }
        }
    }
    return this.dragAndDropTarget(shouldStartDragAndDrop = { enabled }, target = target)
}
internal class AlmadarMonoRun(val text: String, val color: Color? = null, val bold: Boolean = false)

@Composable
internal fun AlmadarMonoRuns(runs: List<AlmadarMonoRun>, modifier: Modifier = Modifier, size: String = "xs") {
    val theme = LocalAlmadarTheme.current
    val text = buildAnnotatedString {
        runs.forEach { run ->
            withStyle(SpanStyle(color = run.color ?: theme.color.foreground, fontWeight = if (run.bold) FontWeight.Bold else null)) { append(run.text) }
        }
    }
    Text(
        text = text,
        modifier = modifier,
        style = almadarType(size).copy(fontFamily = theme.typeScale.monoFamily)
    )
}

/**
 * The theme's resolved motion (orbital-core `resolve_motion`, the kernel snapshot's `motion`), read as
 * numbers: no CSS transform is parsed on this side. Null outside an app (snapshot tests): nothing animates.
 */
internal data class AlmadarMotionShape(val scale: Float, val translateX: Float, val translateY: Float, val inlineSigned: Boolean)

/** A surface's enter shape (`modalEnter`, `popoverEnter`, `toastEnter`, ...). */
internal fun almadarSurfaceShape(motion: JsonElement?, key: String): AlmadarMotionShape? {
    val shape = ((motion as? JsonObject)?.get("shapes") as? JsonObject)?.get(key) as? JsonObject ?: return null
    return AlmadarMotionShape(
        scale = shape.float("scale", 1f),
        translateX = shape.float("translateX", 0f),
        translateY = shape.float("translateY", 0f),
        inlineSigned = (shape["inlineSigned"] as? JsonPrimitive)?.booleanOrNull == true,
    )
}

/** The shape an `enter` keyword starts from (`lib/enter.ts`); null for `none`, an unknown keyword or outside an app. */
internal fun almadarEntryShape(motion: JsonElement?, enter: String): AlmadarMotionShape? {
    if (motion == null) return null
    return when (enter) {
        "fade" -> AlmadarMotionShape(1f, 0f, 0f, false)
        "rise" -> almadarSurfaceShape(motion, "enterRise")
        "scale" -> almadarSurfaceShape(motion, "enterScale")
        "slide" -> almadarSurfaceShape(motion, "enterSlide")
        else -> null
    }
}

/** The entry slot content plays when it changes (`--motion-enter-default`). */
internal fun almadarEntryDefault(motion: JsonElement?): String =
    ((motion as? JsonObject)?.get("entryDefault") as? JsonPrimitive)?.contentOrNull ?: "none"

/**
 * The duration and easing each entry plays with, as the web keyframes time it: `.almadar-enter-*` and
 * `animate-toast-in` are normal + emphasized, `animate-modal-in` / `animate-page-in` normal + standard,
 * `animate-popover-in` fast + standard.
 */
internal enum class AlmadarEnterTiming(val duration: String, val easing: String) {
    Element("normal", "emphasized"), Modal("normal", "standard"), Popover("fast", "standard"), Toast("normal", "emphasized"), Page("normal", "standard"),
}

/** Plays [from] to the resting composable when it enters, and again when [replayKey] changes. Off when the system animator scale is 0 (the OS reduced-motion setting). */
@Composable
internal fun Modifier.almadarEnter(from: AlmadarMotionShape?, delayMillis: Int = 0, timing: AlmadarEnterTiming = AlmadarEnterTiming.Element, replayKey: Any? = Unit): Modifier {
    if (from == null) return this
    val context = LocalContext.current
    val reduced = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    if (reduced) return this
    val scale = LocalAlmadarMotion.current
    val durationMillis = scale.durationsMs[timing.duration] ?: scale.enter.durationMs
    val easing = (scale.easings[timing.easing] ?: scale.enter.easing).toEasing()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current.density
    val progress = remember(replayKey) { Animatable(0f) }
    LaunchedEffect(replayKey) { progress.animateTo(1f, tween(durationMillis, delayMillis, easing)) }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        val s = from.scale + (1f - from.scale) * p
        scaleX = s
        scaleY = s
        val x = if (from.inlineSigned && rtl) -from.translateX else from.translateX
        translationX = x * density * (1f - p)
        translationY = from.translateY * density * (1f - p)
    }
}

/** A node's declared `enter` and `enterDelay` stagger step (`lib/enter.ts` `enterClassName`). */
@Composable
internal fun Modifier.almadarNodeEnter(props: JsonElement): Modifier {
    val enter = props.string("enter")
    if (enter.isEmpty() || enter == "none") return this
    val motion = LocalAlmadarResolvedMotion.current
    val step = props.float("enterDelay", 0f)
    val staggered = if (step >= 1f && step <= 8f && step == kotlin.math.floor(step)) step else 0f
    val staggerMs = ((motion as? JsonObject)?.get("staggerMs") as? JsonPrimitive)?.floatOrNull ?: 0f
    return almadarEnter(almadarEntryShape(motion, enter), (staggered * staggerMs).toInt())
}

/** A surface's enter shape (`modalEnter`, `toastEnter`, ...) played on its content. */
@Composable
internal fun Modifier.almadarSurfaceEnter(shape: String, timing: AlmadarEnterTiming, replayKey: Any? = Unit): Modifier =
    almadarEnter(almadarSurfaceShape(LocalAlmadarResolvedMotion.current, shape), timing = timing, replayKey = replayKey)

/**
 * A slot's theme-default entry (`lib/enter.ts` `slotPlaysDefaultEnter` + `useSlotEnterRestart`): not when
 * the root node declares its own `enter`; replayed when another trait or root pattern takes the slot.
 */
@Composable
internal fun Modifier.almadarSlotEnter(content: JsonElement?, contentKey: String): Modifier {
    if ((content as? JsonObject)?.containsKey("enter") == true) return this
    val motion = LocalAlmadarResolvedMotion.current
    return almadarEnter(almadarEntryShape(motion, almadarEntryDefault(motion)), replayKey = contentKey)
}

/** A host focusing the text field inside it (`chat-bar` `focusRequest`): every new token focuses the field. */
val LocalAlmadarFieldFocusToken = compositionLocalOf<String?> { null }

/** The `chat-bar` clear-pick event (`UI:ELEMENT_SELECTED` with `{ focus: null }` on the web). */
internal const val CHAT_ELEMENT_SELECTED_EVENT: String = "ELEMENT_SELECTED"
