package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer

private const val BRANCHING_ADD_EVENT: String = "ADD_RULE"
private const val BRANCHING_DELETE_EVENT: String = "DELETE_RULE"
private const val BRANCHING_VIEW_EDIT_EVENT: String = "VIEW_EDIT"
private const val BRANCHING_VIEW_GRAPH_EVENT: String = "VIEW_GRAPH"
private const val BRANCHING_SUBMIT: String = "local-submit"
private const val BRANCHING_END: String = "end-of-survey"
private const val GRAPH_NODE_WIDTH: Float = 180f
private const val GRAPH_NODE_HEIGHT: Float = 56f
private const val GRAPH_GAP_Y: Float = 80f
private const val GRAPH_PADDING: Float = 32f
private const val GRAPH_LABEL_ROOM: Float = 220f

private fun branchingBroken(rule: JsonObject, questions: List<JsonObject>): Boolean {
    val sourceExists = questions.any { it.string("id") == rule.string("sourceQuestionId") }
    val target = rule.string("targetQuestionId")
    val targetExists = target == BRANCHING_END || questions.any { it.string("id") == target }
    return !sourceExists || !targetExists
}

private fun branchingOptions(pairs: List<Pair<String, String>>): JsonArray =
    JsonArray(pairs.map { almadarJson("value" to it.first, "label" to it.second) })

private fun branchingChips(rule: JsonObject): List<String> = almadarStrings(rule["value"])

private fun branchingRuleLabel(rule: JsonObject): String {
    val chips = branchingChips(rule)
    val operator = rule.string("operator")
    return if (operator == "in" && rule["value"] is JsonArray) "in [" + chips.joinToString(", ") + "]"
    else operator + " " + (if (rule["value"] is JsonArray) chips.joinToString(", ") else rule.string("value"))
}

@Composable
private fun BranchingGraph(questions: List<JsonObject>, rules: List<JsonObject>, endLabel: String, modifier: Modifier) {
    val palette = LocalAlmadarTheme.current.color
    val measurer = rememberTextMeasurer()
    val nodes = questions.map { Triple(it.string("id"), it.string("label"), false) } + Triple(BRANCHING_END, endLabel, true)
    val top = nodes.mapIndexed { index, node -> node.first to (GRAPH_PADDING + index * (GRAPH_NODE_HEIGHT + GRAPH_GAP_Y)) }.toMap()
    val width = GRAPH_NODE_WIDTH + GRAPH_PADDING * 2f + GRAPH_LABEL_ROOM
    val height = GRAPH_PADDING * 2f + nodes.size * (GRAPH_NODE_HEIGHT + GRAPH_GAP_Y)
    val mono = LocalAlmadarTheme.current.typeScale.monoFamily
    Box(modifier = modifier.horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(width.dp, height.dp)) {
            val pxPerDp = this.density
            withTransform({ scale(pxPerDp, pxPerDp, androidx.compose.ui.geometry.Offset.Zero) }) {
                rules.forEachIndexed { index, rule ->
                    val fromY = top[rule.string("sourceQuestionId")]
                    val toY = top[rule.string("targetQuestionId")]
                    if (fromY != null && toY != null) {
                        val startX = GRAPH_PADDING + GRAPH_NODE_WIDTH
                        val startY = fromY + GRAPH_NODE_HEIGHT / 2f
                        val endY = toY + GRAPH_NODE_HEIGHT / 2f
                        val cx = startX + 40f + (index % 4) * 30f
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(startX, startY)
                            cubicTo(cx, startY, cx, endY, startX, endY)
                        }
                        drawPath(path, color = palette.mutedForeground, style = Stroke(width = 1.5f))
                        val arrow = androidx.compose.ui.graphics.Path().apply {
                            moveTo(startX, endY)
                            lineTo(startX + 8f, endY - 4f)
                            lineTo(startX + 8f, endY + 4f)
                            close()
                        }
                        drawPath(arrow, color = palette.mutedForeground)
                        val text = branchingRuleLabel(rule)
                        val labelY = (startY + endY) / 2f
                        val boxWidth = minOf(160f, text.length * 7f + 12f)
                        drawRoundRect(palette.card, androidx.compose.ui.geometry.Offset(cx - 4f, labelY - 10f), androidx.compose.ui.geometry.Size(boxWidth, 20f), androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                        drawRoundRect(palette.border, androidx.compose.ui.geometry.Offset(cx - 4f, labelY - 10f), androidx.compose.ui.geometry.Size(boxWidth, 20f), androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(width = 1f))
                        val layout = measurer.measure(
                            text, androidx.compose.ui.text.TextStyle(fontSize = 11f.toSp(), fontFamily = mono, color = palette.foreground),
                            overflow = TextOverflow.Ellipsis, maxLines = 1,
                            constraints = androidx.compose.ui.unit.Constraints(maxWidth = ((boxWidth - 8f) * pxPerDp).toInt())
                        )
                        drawText(layout, topLeft = androidx.compose.ui.geometry.Offset(cx + 4f, labelY - layout.size.height / pxPerDp / 2f))
                    }
                }
                nodes.forEach { node ->
                    val y = top[node.first] ?: 0f
                    val corner = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    val origin = androidx.compose.ui.geometry.Offset(GRAPH_PADDING, y)
                    val extent = androidx.compose.ui.geometry.Size(GRAPH_NODE_WIDTH, GRAPH_NODE_HEIGHT)
                    drawRoundRect(if (node.third) palette.surface else palette.card, origin, extent, corner)
                    drawRoundRect(if (node.third) palette.error else palette.border, origin, extent, corner, style = Stroke(width = if (node.third) 2f else 1.5f))
                    val layout = measurer.measure(
                        node.second, androidx.compose.ui.text.TextStyle(fontSize = 13f.toSp(), color = palette.foreground, fontWeight = FontWeight.SemiBold),
                        overflow = TextOverflow.Ellipsis, maxLines = 1,
                        constraints = androidx.compose.ui.unit.Constraints(maxWidth = ((GRAPH_NODE_WIDTH - 16f) * pxPerDp).toInt())
                    )
                    drawText(
                        layout,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            GRAPH_PADDING + GRAPH_NODE_WIDTH / 2f - layout.size.width / pxPerDp / 2f,
                            y + GRAPH_NODE_HEIGHT / 2f - layout.size.height / pxPerDp / 2f
                        )
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BranchingLogicBuilderRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val palette = LocalAlmadarTheme.current.color
    val questions = props.array("questions").mapNotNull { it as? JsonObject }
    val incoming = props.array("rules").mapNotNull { it as? JsonObject }
    var rules by remember(incoming) { mutableStateOf(incoming) }
    var view by remember { mutableStateOf("edit") }
    val readOnly = props.bool("readOnly")
    val rulesEvent = props.string("rulesChangeEvent")
    val rulesCallback = props.string("onRulesChange")
    val brokenCount = rules.count { branchingBroken(it, questions) }
    val update = { next: List<JsonObject> ->
        rules = next
        if (rulesCallback.isNotEmpty()) onEvent(rulesCallback, eventEnvelope(props, almadarJson("rules" to JsonArray(next))))
        if (rulesEvent.isNotEmpty()) onEvent(rulesEvent, eventEnvelope(props, almadarJson("rules" to JsonArray(next))))
    }
    val pick = { envelope: JsonElement? -> envelope?.jsonObject("actionPayload")?.string("value", "") ?: "" }
    val withValue = { rule: JsonObject, key: String, value: JsonElement -> JsonObject(rule + (key to value)) }
    val choose: @Composable (List<Pair<String, String>>, String, String, String, Dp, (String) -> Unit) -> Unit = { options, current, placeholder, error, extent, assign ->
        if (readOnly) {
            AlmadarText(options.firstOrNull { it.first == current }?.second ?: current, Modifier.width(extent), variant = "body2")
        } else {
            SelectRenderer(
                almadarJson(
                    "options" to branchingOptions(options), "value" to current, "placeholder" to placeholder,
                    "error" to error, "onChange" to ALMADAR_LOCAL_ACTION, "local" to true
                ),
                Modifier.width(extent)
            ) { _, envelope -> assign(pick(envelope)) }
        }
    }
    val operators = listOf("equals" to "equals", "not-equals" to "does not equal", "contains" to "contains", "in" to "is one of")
    Column(modifier = modifier.fillMaxWidth().almadarLabel(props.string("aria-label")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarGlyph("git-branch", size = "md")
                AlmadarText("Branching logic", variant = "subheading", weight = "semibold")
                AlmadarBadge(rules.size.toString() + (if (rules.size == 1) " rule" else " rules"), variant = "neutral")
                if (brokenCount > 0) AlmadarBadge(brokenCount.toString() + " broken", variant = "error")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                AlmadarButton(
                    props, "Rules", BRANCHING_VIEW_EDIT_EVENT, { name, _ -> view = "edit"; onEvent(name, eventEnvelope(props)) },
                    variant = if (view == "edit") "primary" else "ghost", icon = "pencil", size = "sm"
                )
                AlmadarButton(
                    props, "Logic graph", BRANCHING_VIEW_GRAPH_EVENT, { name, _ -> view = "graph"; onEvent(name, eventEnvelope(props)) },
                    variant = if (view == "graph") "primary" else "ghost", icon = "eye", size = "sm"
                )
            }
        }
        if (view == "graph") {
            BranchingGraph(questions, rules, "End of survey", Modifier.fillMaxWidth())
        } else {
            if (rules.isEmpty()) {
                AlmadarPanel(modifier = Modifier.fillMaxWidth()) {
                    AlmadarText(
                        if (questions.isEmpty()) "Add questions before building branching rules." else "No rules yet. Add a rule to define branching logic.",
                        Modifier.fillMaxWidth().padding(almadarSpaceFor("lg")), color = "muted", align = "center"
                    )
                }
            }
            rules.forEach { rule ->
                val broken = branchingBroken(rule, questions)
                val source = questions.firstOrNull { it.string("id") == rule.string("sourceQuestionId") }
                val valueOptions = almadarStrings(source?.get("optionValues")).map { it to it }
                val chips = branchingChips(rule)
                val multi = rule.string("operator") == "in"
                val questionOptions = questions.map { it.string("id") to it.string("label") }
                AlmadarPanel(modifier = Modifier.fillMaxWidth().then(if (broken) Modifier.border(LocalAlmadarGeometry.current.borderStandard, palette.error) else Modifier)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("sm")), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm")),
                            verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))
                        ) {
                            AlmadarText("If", variant = "label", weight = "semibold")
                            choose(questionOptions, rule.string("sourceQuestionId"), "Select question", if (broken) "Broken reference" else "", AlmadarSizes.w40) { picked ->
                                update(rules.map { if (it === rule) withValue(rule, "sourceQuestionId", JsonPrimitive(picked)) else it })
                            }
                            choose(operators, rule.string("operator"), "", "", AlmadarSizes.w32) { picked ->
                                val value: JsonElement = when {
                                    picked == "in" && rule["value"] !is JsonArray -> JsonArray(listOfNotNull(rule.string("value").takeIf { it.isNotEmpty() }?.let { JsonPrimitive(it) }))
                                    picked != "in" && rule["value"] is JsonArray -> JsonPrimitive(chips.firstOrNull() ?: "")
                                    else -> rule["value"] ?: JsonPrimitive("")
                                }
                                update(rules.map { if (it === rule) JsonObject(rule + mapOf("operator" to JsonPrimitive(picked), "value" to value)) else it })
                            }
                            if (multi) {
                                chips.forEach { chip ->
                                    FilterPillRenderer(
                                        almadarJson(
                                            "label" to chip, "variant" to "primary", "size" to "sm", "removable" to !readOnly,
                                            "removeEvent" to ALMADAR_LOCAL_ACTION, "local" to true
                                        ),
                                        Modifier
                                    ) { _, _ -> update(rules.map { if (it === rule) withValue(rule, "value", JsonArray((chips - chip).map { c -> JsonPrimitive(c) })) else it }) }
                                }
                                if (!readOnly) {
                                    if (valueOptions.isNotEmpty()) {
                                        choose(valueOptions.filter { it.first !in chips }, "", "Add value", "", AlmadarSizes.w32) { picked ->
                                            if (picked.isNotEmpty() && picked !in chips) {
                                                update(rules.map { if (it === rule) withValue(rule, "value", JsonArray((chips + picked).map { c -> JsonPrimitive(c) })) else it })
                                            }
                                        }
                                    } else {
                                        key(chips.size) {
                                            InputRenderer(
                                                almadarJson("value" to "", "placeholder" to "Type value, press Enter", "onChange" to ALMADAR_LOCAL_ACTION, "action" to BRANCHING_SUBMIT, "local" to true),
                                                Modifier.width(AlmadarSizes.w40)
                                            ) { event, envelope ->
                                                val typed = pick(envelope).trim()
                                                if (event == BRANCHING_SUBMIT && typed.isNotEmpty() && typed !in chips) {
                                                    update(rules.map { if (it === rule) withValue(rule, "value", JsonArray((chips + typed).map { c -> JsonPrimitive(c) })) else it })
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (valueOptions.isNotEmpty()) {
                                choose(valueOptions, rule.string("value"), "Select value", "", AlmadarSizes.w32) { picked ->
                                    update(rules.map { if (it === rule) withValue(rule, "value", JsonPrimitive(picked)) else it })
                                }
                            } else if (readOnly) {
                                AlmadarText(rule.string("value"), variant = "body2")
                            } else {
                                InputRenderer(
                                    almadarJson("value" to rule.string("value"), "placeholder" to "Value", "onChange" to ALMADAR_LOCAL_ACTION, "local" to true),
                                    Modifier.width(AlmadarSizes.w32)
                                ) { _, envelope -> update(rules.map { if (it === rule) withValue(rule, "value", JsonPrimitive(pick(envelope))) else it }) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(almadarSpaceFor("xs")), verticalAlignment = Alignment.CenterVertically) {
                                AlmadarGlyph("arrow-right", size = "sm")
                                AlmadarText("go to", variant = "label", weight = "semibold")
                            }
                            choose(
                                questionOptions + (BRANCHING_END to "End of survey"), rule.string("targetQuestionId"), "Select target",
                                if (broken && rule.string("targetQuestionId") != BRANCHING_END) "Broken reference" else "", AlmadarSizes.w40
                            ) { picked -> update(rules.map { if (it === rule) withValue(rule, "targetQuestionId", JsonPrimitive(picked)) else it }) }
                            if (!readOnly) {
                                AlmadarButton(
                                    props, "", BRANCHING_DELETE_EVENT,
                                    { name, _ ->
                                        update(rules.filter { it !== rule })
                                        onEvent(name, eventEnvelope(props, almadarJson("ruleId" to rule.string("id"))))
                                    },
                                    payload = almadarJson("ruleId" to rule.string("id")), variant = "ghost", icon = "trash", size = "sm", ariaLabel = "Delete rule"
                                )
                            }
                        }
                        if (broken) AlmadarBadge("Broken reference", variant = "error")
                    }
                }
            }
            if (!readOnly) {
                AlmadarButton(
                    props, "Add rule", BRANCHING_ADD_EVENT,
                    { name, _ ->
                        val first = questions.firstOrNull()
                        if (first != null) {
                            update(
                                rules + almadarJson(
                                    "id" to ("rule-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 6)),
                                    "sourceQuestionId" to first.string("id"), "operator" to "equals",
                                    "value" to almadarStrings(first["optionValues"]).firstOrNull().orEmpty(),
                                    "targetQuestionId" to (questions.getOrNull(1)?.string("id") ?: BRANCHING_END)
                                )
                            )
                        }
                        onEvent(name, eventEnvelope(props))
                    },
                    modifier = Modifier.align(Alignment.Start), variant = "secondary", icon = "plus", size = "sm", disabled = questions.isEmpty()
                )
            }
        }
    }
}
