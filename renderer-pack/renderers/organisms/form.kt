package generated.patterns.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.json.*

// tier: organisms — a LazyColumn/LazyVerticalGrid fragment

@Composable
fun FormRenderer(
    props: JsonElement,
    modifier: Modifier,
    onEvent: (String, JsonElement?) -> Unit
) {
    val root = props as? JsonObject
    val overrides = props.array("fieldOverrides").mapNotNull { it as? JsonObject }
    val fields = props.array("fields").map { field ->
        val obj = field as? JsonObject
        val name = obj?.string("name", obj.string("field")).orEmpty()
        val override = overrides.firstOrNull { it.string("name") == name }
        if (obj == null || override == null) field else obj.merging(override.filterKeys { it == "label" || it == "placeholder" || it == "hint" })
    }
    val initial = JsonObject(((root?.get("entity") as? JsonObject) ?: JsonObject(emptyMap())) + (props.jsonObject("initialData") ?: JsonObject(emptyMap())))
    FormSectionRenderer(
        props.merging(
            mapOf(
                "fields" to JsonArray(fields),
                "initialData" to initial,
                "submitEvent" to JsonPrimitive(props.string("submitEvent", "SAVE")),
                "cancelEvent" to JsonPrimitive(props.string("cancelEvent", "CANCEL"))
            )
        ),
        modifier,
        onEvent
    )
}
