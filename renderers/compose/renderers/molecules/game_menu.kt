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

// tier: molecules — a grouped container styled by tokens only

import androidx.compose.foundation.background

@Composable
fun GameMenuRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val inOverlay = LocalAlmadarGameOverlay.current
    val title = props.string("title", "Main Menu")
    val subtitle = props.string("subtitle")
    val declared = (root?.get("options") as? JsonArray)?.takeIf { it.isNotEmpty() } ?: (root?.get("menuItems") as? JsonArray)?.takeIf { it.isNotEmpty() }
    val options = declared?.mapNotNull { it as? JsonObject } ?: listOf(
        almadarJson("label" to "New Game", "event" to "NEW_GAME", "variant" to "primary"),
        almadarJson("label" to "Continue", "event" to "CONTINUE", "variant" to "secondary"),
        almadarJson("label" to "Settings", "event" to "SETTINGS", "variant" to "ghost")
    )
    val logo = props.jsonObject("logo")?.let { it.string("url", it.string("src")) } ?: ""
    val surface = if (inOverlay) modifier else modifier.fillMaxSize().background(LocalAlmadarTheme.current.color.background).padding(almadarSpaceFor("lg"))
    Box(modifier = surface.almadarLabel(props.string("aria-label")), contentAlignment = Alignment.Center) {
        AlmadarPanel(modifier = Modifier.widthIn(max = almadarSpaceFor("2xl") * 9).fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(almadarSpaceFor("lg")),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("lg"))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("sm"))) {
                    if (logo.isNotEmpty()) {
                        ImageRenderer(almadarJson("src" to logo, "alt" to title, "fit" to "contain"), Modifier.height(almadarSpaceFor("2xl") * 2), AlmadarNoEvent)
                    }
                    AlmadarText(title, variant = "h1", align = "center")
                    if (subtitle.isNotEmpty()) AlmadarText(subtitle.uppercase(), variant = "body1", color = "muted", align = "center")
                }
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(almadarSpaceFor("md"))) {
                    options.forEachIndexed { index, option ->
                        val event = option.string("event")
                        val target = option.string("navigatesTo")
                        val route = if (event.isNotEmpty()) event else if (target.isNotEmpty()) NAVIGATE_EVENT else ""
                        val payload = if (event.isNotEmpty()) almadarJson("option" to option) else almadarJson("url" to target, "option" to option)
                        ChoiceButtonRenderer(
                            almadarJson(
                                "text" to option.string("label"), "action" to route, "index" to index + 1,
                                "disabled" to option.bool("disabled"), "payload" to payload, "sourceTrait" to almadarTrait(props)
                            ),
                            Modifier,
                            { name, envelope ->
                                onEvent(name, eventEnvelope(props, envelope?.jsonObject("actionPayload")))
                                if (event.isNotEmpty() && target.isNotEmpty()) {
                                    onEvent(NAVIGATE_EVENT, eventEnvelope(props, almadarJson("url" to target, "option" to option)))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
