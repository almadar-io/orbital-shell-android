package generated.patterns.core

object VerificationIds {
    const val ACTION_OVERFLOW: String = "{{actionOverflow}}"

    fun action(eventKey: String): String = "{{actionPrefix}}" + eventKey

{{gestures}}    /** One identifier for an element that dispatches several actions (a tap and a gesture). */
    fun ids(vararg ids: String): String = ids.filter { it.isNotEmpty() }.joinToString("{{separator}}")

    fun pattern(name: String): String = "{{patternPrefix}}" + name

    fun row(id: String): String = "{{rowPrefix}}" + id

    fun field(name: String): String = "{{fieldPrefix}}" + name

    fun slot(name: String): String = "{{slotPrefix}}" + name
}
