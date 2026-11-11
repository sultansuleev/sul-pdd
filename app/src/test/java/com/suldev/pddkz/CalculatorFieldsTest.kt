package com.suldev.pddkz

import com.suldev.pddkz.domain.model.CalculatorInput
import com.suldev.pddkz.ui.viewmodel.CalculatorFields
import com.suldev.pddkz.ui.viewmodel.Field
import com.suldev.pddkz.ui.viewmodel.parse
import com.suldev.pddkz.ui.viewmodel.toFieldText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorFieldsTest {

    private fun fields(vararg text: Pair<Field, String>) =
        CalculatorFields().let { it.copy(text = it.text + text) }

    @Test
    fun `default fields parse to default input without errors`() {
        val parsed = CalculatorFields().parse()

        assertEquals(CalculatorInput(), parsed.input)
        assertTrue(parsed.errors.isEmpty())
        assertNull(parsed.qtyHint)
    }

    @Test
    fun `comma is accepted as decimal separator`() {
        assertEquals(1.5, fields(Field.ITEM_WEIGHT to "1,5").parse().input.itemWeightKg, 0.0)
    }

    @Test
    fun `blank number is zero without error, garbage is zero with error`() {
        val blank = fields(Field.ITEM_YUAN to "").parse()
        assertEquals(0.0, blank.input.itemYuan, 0.0)
        assertTrue(blank.errors.isEmpty())

        val garbage = fields(Field.ITEM_YUAN to "1.2.3").parse()
        assertEquals(0.0, garbage.input.itemYuan, 0.0)
        assertEquals(setOf(Field.ITEM_YUAN), garbage.errors.keys)
    }

    @Test
    fun `quantity falls back to one - hint when blank, error when invalid`() {
        val blank = fields(Field.ITEM_QTY to "").parse()
        assertEquals(1, blank.input.itemQty)
        assertTrue(blank.errors.isEmpty())
        assertEquals("Пусто — считаем 1 шт", blank.qtyHint)

        val zero = fields(Field.ITEM_QTY to "0").parse()
        assertEquals(1, zero.input.itemQty)
        assertEquals(setOf(Field.ITEM_QTY), zero.errors.keys)
        assertNull(zero.qtyHint)
    }

    @Test
    fun `blank days fall back to default captions`() {
        val input = fields(Field.KAZPOST_DAYS to " ", Field.CARGO_DAYS to "").parse().input

        assertEquals("10–18", input.kazpostDays)
        assertEquals("12–25", input.cargoDays)
    }

    @Test
    fun `whole numbers are shown without trailing zero`() {
        assertEquals("180", 180.0.toFieldText())
        assertEquals("1.2", 1.2.toFieldText())
        assertEquals("3.5", 3.5.toFieldText())
    }
}
