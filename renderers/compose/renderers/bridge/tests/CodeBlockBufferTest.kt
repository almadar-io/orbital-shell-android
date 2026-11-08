package generated.patterns.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeBlockBufferTest {
    private fun obj(text: String): JsonObject = Json.parseToJsonElement(text) as JsonObject
    private fun paint(line: String, row: Int, cursor: BufferCursor?, caret: String = "block", selection: BufferSelection? = null, decorations: List<BufferDecoration> = emptyList()) =
        bufferLinePaint(line, row, cursor, caret, selection, decorations)

    @Test
    fun buffer_mode_is_off_without_a_cursor_object() {
        assertFalse(bufferModeOn(obj("""{"type":"code-block","code":"a"}""")))
        assertFalse(bufferModeOn(obj("""{"type":"code-block","code":"a","cursor":null}""")))
        assertTrue(bufferModeOn(obj("""{"type":"code-block","code":"a","cursor":{"row":0,"col":0}}""")))
    }

    @Test
    fun block_caret_covers_the_character_at_col() {
        val cells = paint("cde", 1, BufferCursor(1, 2))
        assertEquals(listOf(false, false, true, false), cells.map { it.bgRole == "foreground" })
        assertEquals("background", cells[2].fgRole)
        assertTrue(paint("ab", 0, BufferCursor(1, 2)).none { it.bgRole == "foreground" })
    }

    @Test
    fun bar_caret_sits_before_the_character_and_underline_marks_it() {
        val bar = paint("cde", 1, BufferCursor(1, 2), "bar")
        assertEquals(2, bar.indexOfFirst { it.barBefore })
        assertTrue(bar.none { it.bgRole == "foreground" })
        val underline = paint("cde", 1, BufferCursor(1, 2), "underline")
        assertEquals(2, underline.indexOfFirst { it.underlineCaret })
    }

    @Test
    fun caret_at_line_end_paints_a_space_width_block_after_the_last_character() {
        val cells = paint("ab", 0, BufferCursor(0, 2))
        assertEquals(3, cells.size)
        assertEquals("foreground", cells[2].bgRole)
        assertEquals(listOf("a", "b"), bufferCells("ab"))
    }

    @Test
    fun linewise_selection_paints_both_lines_and_charwise_paints_bc() {
        val linewise = BufferSelection(0, 3, 1, 0, true)
        assertTrue(paint("ab", 0, null, selection = linewise).all { it.bgRole == "primary" })
        assertTrue(paint("cd", 1, null, selection = linewise).all { it.bgRole == "primary" })
        assertTrue(paint("ef", 2, null, selection = linewise).none { it.bgRole == "primary" })
        val charwise = BufferSelection(0, 1, 0, 2, false)
        assertEquals(listOf(false, true, true, false, false), paint("abcd", 0, null, selection = charwise).map { it.bgRole == "primary" })
        val reversed = BufferSelection(0, 2, 0, 1, false)
        assertEquals(listOf(false, true, true, false, false), paint("abcd", 0, null, selection = reversed).map { it.bgRole == "primary" })
    }

    @Test
    fun syntax_tones_color_the_text_and_annotation_tones_paint_a_background() {
        val keyword = paint("fun x", 0, null, decorations = listOf(BufferDecoration("k", 0, 0, 0, 3, "keyword", null)))
        assertEquals(listOf("primary", "primary", "primary", null, null, null), keyword.map { it.fgRole })
        assertTrue(keyword.all { it.bgRole == null })
        val review = paint("fun x", 0, null, decorations = listOf(BufferDecoration("r", 0, 0, 0, 3, "review", null)))
        assertEquals(listOf("info", "info", "info", null, null, null), review.map { it.bgRole })
        assertTrue(review.all { it.fgRole == null })
    }

    @Test
    fun an_unknown_tone_paints_nothing_and_later_decorations_win() {
        val none = paint("abc", 0, null, decorations = listOf(BufferDecoration("u", 0, 0, 0, 3, "mystery", null)))
        assertTrue(none.all { it.fgRole == null && it.bgRole == null })
        val over = paint("abc", 0, null, decorations = listOf(
            BufferDecoration("a", 0, 0, 0, 3, "keyword", null),
            BufferDecoration("b", 0, 1, 0, 2, "string", null)
        ))
        assertEquals(listOf("primary", "success", "primary", null), over.map { it.fgRole })
    }

    @Test
    fun a_decoration_spanning_rows_paints_each_row_by_its_own_range() {
        val deco = BufferDecoration("m", 0, 2, 2, 1, "comment", null)
        assertEquals(listOf(null, null, "mutedForeground"), paint("abc", 0, null, decorations = listOf(deco)).take(3).map { it.fgRole })
        assertTrue(paint("xyz", 1, null, decorations = listOf(deco)).take(3).all { it.fgRole == "mutedForeground" })
        assertEquals(listOf("mutedForeground", null), paint("qr", 2, null, decorations = listOf(deco)).take(2).map { it.fgRole })
    }

    @Test
    fun relative_gutter_shows_distance_and_the_absolute_cursor_row() {
        assertEquals(listOf("2", "1", "3", "1", "2"), bufferGutter(5, 2, true))
        assertEquals(listOf("1", "2", "3"), bufferGutter(3, 1, false))
    }

    @Test
    fun columns_count_unicode_scalar_values() {
        assertEquals(3, bufferCells("a😀b").size)
        val cells = paint("a😀b", 0, BufferCursor(0, 2))
        assertEquals("foreground", cells[2].bgRole)
    }

    @Test
    fun hit_testing_clamps_to_the_line_and_the_last_row() {
        assertEquals(1, bufferColAt(12f, 10f, 0f, 5))
        assertEquals(5, bufferColAt(900f, 10f, 0f, 5))
        assertEquals(0, bufferColAt(3f, 10f, 40f, 5))
        val visible = listOf(Triple(0, 0, 20), Triple(1, 20, 20))
        assertEquals(1, bufferRowAt(25f, visible))
        assertEquals(1, bufferRowAt(500f, visible))
        assertEquals(0, bufferRowAt(-4f, visible))
    }

    @Test
    fun scroll_target_keeps_the_cursor_row_visible() {
        assertNull(bufferScrollTarget(2, 8, 5))
        assertEquals(1, bufferScrollTarget(2, 8, 1))
        assertEquals(4, bufferScrollTarget(2, 8, 10))
    }

    @Test
    fun parses_selection_decorations_and_popover() {
        val root = obj("""{"selection":{"fromRow":0,"fromCol":1,"toRow":2,"toCol":3,"linewise":true},"decorations":[{"id":"d","fromRow":0,"fromCol":0,"toRow":0,"toCol":2,"tone":"error","label":"bad"}],"popover":{"fromRow":1,"fromCol":0,"toRow":1,"toCol":2,"title":"T","sections":[{"heading":"H","lines":["l1","l2"]}],"busy":true}}""")
        assertEquals(BufferSelection(0, 1, 2, 3, true), bufferSelection(root))
        assertEquals("bad", bufferDecorations(root).single().label)
        val popover = bufferPopover(root)!!
        assertEquals("T", popover.title)
        assertEquals(listOf("l1", "l2"), popover.sections.single().second)
        assertTrue(popover.busy)
        assertNull(bufferPopover(obj("""{"popover":null}""")))
        assertNull(bufferSelection(obj("""{"selection":null}""")))
    }

    @Test
    fun key_payload_has_the_web_shape() {
        val payload = bufferKeyPayload("e", "ArrowLeft", "ArrowLeft", ctrl = true, alt = false, shift = false, meta = false, repeat = true)
        assertEquals("""{"editorId":"e","key":"ArrowLeft","code":"ArrowLeft","ctrl":true,"alt":false,"shift":false,"meta":false,"repeat":true}""", payload.toString())
        assertEquals("""{"editorId":"e","row":1,"col":1}""", bufferPointerPayload("e", 1, 1).toString())
    }
}
