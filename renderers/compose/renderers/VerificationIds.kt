package generated.patterns.core

object VerificationIds {
    const val ACTION_OVERFLOW: String = "action-overflow"

    fun action(eventKey: String): String = "action-" + eventKey

    fun longPress(eventKey: String): String = "action-long-press:" + eventKey

    fun swipeLeft(eventKey: String): String = "action-swipe-left:" + eventKey

    fun swipeRight(eventKey: String): String = "action-swipe-right:" + eventKey

    /** One identifier for an element that dispatches several actions (a tap and a gesture). */
    fun ids(vararg ids: String): String = ids.filter { it.isNotEmpty() }.joinToString(" ")

    fun pattern(name: String): String = "pattern-" + name

    fun row(id: String): String = "row-" + id

    fun field(name: String): String = "field-" + name

    fun slot(name: String): String = "slot-" + name
}
