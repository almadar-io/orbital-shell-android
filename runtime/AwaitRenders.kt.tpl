package generated

import generated.patterns.core.KernelAwaitingTrait
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** What a slot shows while `requestTrait`, having taken `event` from one of `from` (any state when
 *  null), awaits its server round trip: the compiler's `await_render` prediction. */
@Serializable
data class AwaitRender(
    val requestTrait: String,
    val event: String,
    val from: List<String>? = null,
    @SerialName("trait") val trait: String,
    val slot: String,
    val skeleton: JsonElement,
)

object AwaitRenders {
    val all: List<AwaitRender> by lazy {
        runCatching { Json.decodeFromString(ListSerializer(AwaitRender.serializer()), JSON) }
            .onFailure { android.util.Log.e("Orbital", "await-render table failed to decode", it) }
            .getOrDefault(emptyList())
    }

    fun matching(awaiting: KernelAwaitingTrait): List<AwaitRender> = all.filter { row ->
        row.requestTrait == awaiting.trait && row.event == awaiting.event && (row.from?.contains(awaiting.from) ?: true)
    }

    private val JSON = """__ROWS__"""
}
