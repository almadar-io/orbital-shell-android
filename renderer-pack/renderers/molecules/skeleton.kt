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

// tier: molecules — a grouped container styled by tokens only

@Composable
fun SkeletonRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val density = LocalAlmadarDensity.current
    val specNode = (props as? JsonObject)?.get("spec")
    val shimmer = palette.muted
    val line = almadarSpaceFor("md")
    val title = almadarSpaceFor("lg")
    val gap = almadarSpaceFor("sm")
    val framed = Modifier.border(LocalAlmadarGeometry.current.borderStandard, palette.border, MaterialTheme.shapes.medium)
    val bar: @Composable (Modifier) -> Unit = { m -> Box(modifier = m.clip(MaterialTheme.shapes.small).background(shimmer)) }
    val pill: @Composable (Modifier) -> Unit = { m -> Box(modifier = m.clip(MaterialTheme.shapes.large).background(shimmer)) }
    val card: @Composable (Modifier) -> Unit = { m ->
        Column(modifier = m.then(framed).padding(density.cardPaddingMd), verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                pill(Modifier.size(density.buttonHeightMd))
                Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                    bar(Modifier.width(density.buttonHeightLg * 3).height(title))
                    bar(Modifier.width(density.buttonHeightLg * 4).height(line))
                }
            }
            bar(Modifier.fillMaxWidth().height(line))
            bar(Modifier.fillMaxWidth(0.75f).height(line))
        }
    }
    @Composable
    fun shape(variant: String, source: JsonElement) {
        val rows = source.int("rows", when (variant) { "table", "list" -> 5; "grid", "detail" -> 6; else -> 3 })
        val columns = source.int("columns", 4)
        val fields = source.int("fields", 4)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
            when (variant) {
                "header" -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        bar(Modifier.width(density.buttonHeightLg * 4).height(title))
                        bar(Modifier.width(density.buttonHeightLg * 5).height(line))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        bar(Modifier.width(density.buttonHeightLg * 2).height(density.buttonHeightMd))
                        bar(Modifier.width(density.buttonHeightLg * 3).height(density.buttonHeightMd))
                    }
                }
                "table" -> Column(modifier = framed) {
                    Row(modifier = Modifier.fillMaxWidth().background(shimmer.copy(alpha = 0.3f)).padding(gap)) {
                        repeat(columns) { bar(Modifier.weight(1f).padding(horizontal = gap).height(line)) }
                    }
                    repeat(rows) {
                        Row(modifier = Modifier.fillMaxWidth().padding(gap)) {
                            repeat(columns) { bar(Modifier.weight(1f).padding(horizontal = gap).height(line)) }
                        }
                    }
                }
                "form" -> Column(modifier = Modifier.padding(density.cardPaddingLg), verticalArrangement = Arrangement.spacedBy(density.sectionGap)) {
                    bar(Modifier.width(density.buttonHeightLg * 3).height(title))
                    repeat(fields) {
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            bar(Modifier.width(density.buttonHeightLg * 2).height(line))
                            bar(Modifier.fillMaxWidth().height(density.inputHeightMd))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.End)) {
                        bar(Modifier.width(density.buttonHeightLg * 2).height(density.buttonHeightMd))
                        bar(Modifier.width(density.buttonHeightLg * 2).height(density.buttonHeightMd))
                    }
                }
                "card" -> card(Modifier.fillMaxWidth())
                "list" -> Column(modifier = framed) {
                    repeat(rows) {
                        Row(modifier = Modifier.fillMaxWidth().padding(gap), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                            pill(Modifier.size(density.buttonHeightMd))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                                bar(Modifier.fillMaxWidth(0.4f).height(line))
                                bar(Modifier.fillMaxWidth(0.7f).height(line))
                            }
                            pill(Modifier.width(density.buttonHeightLg * 2).height(title))
                        }
                    }
                }
                "grid" -> repeat((rows + 1) / 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        card(Modifier.weight(1f))
                        card(Modifier.weight(1f))
                    }
                }
                "stats" -> Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(columns) {
                        Column(modifier = Modifier.weight(1f).then(framed).padding(density.cardPaddingMd), verticalArrangement = Arrangement.spacedBy(gap)) {
                            bar(Modifier.fillMaxWidth(0.5f).height(line))
                            bar(Modifier.fillMaxWidth(0.7f).height(title))
                        }
                    }
                }
                "detail" -> Column(modifier = framed.padding(density.cardPaddingLg), verticalArrangement = Arrangement.spacedBy(density.sectionGap)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        bar(Modifier.width(density.buttonHeightLg * 4).height(title))
                        bar(Modifier.width(density.buttonHeightLg * 2).height(density.buttonHeightMd))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(3) { bar(Modifier.weight(1f).height(density.buttonHeightLg * 2)) }
                    }
                    repeat(rows) {
                        Column(verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs"))) {
                            bar(Modifier.fillMaxWidth(0.3f).height(line))
                            bar(Modifier.fillMaxWidth(0.6f).height(line))
                        }
                    }
                }
                "none" -> Unit
                else -> repeat(rows) { index ->
                    bar(Modifier.fillMaxWidth(if (index == rows - 1) 0.66f else 1f).height(line))
                }
            }
        }
    }
    @Composable
    fun node(spec: JsonElement) {
        val stack = (spec as? JsonObject)?.get("stack") as? JsonArray
        if (spec is JsonPrimitive) {
            shape(spec.content, JsonObject(emptyMap()))
        } else if (stack != null) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("lg"))) {
                stack.forEach { node(it) }
            }
        } else if (spec is JsonObject) {
            val inner = (spec["shape"] as? JsonObject) ?: spec
            shape(inner.string("variant", "text"), inner)
        }
    }
    if ((specNode as? JsonPrimitive)?.contentOrNull != "none") {
        Box(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label", "Loading"))) {
            if (specNode == null) shape(props.string("variant", "text"), props) else node(specNode)
        }
    }
}
