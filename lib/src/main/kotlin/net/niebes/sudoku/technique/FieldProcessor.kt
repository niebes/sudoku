package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.DeductionListener
import net.niebes.sudoku.model.Field

/** One step of the solving chain: reads a field, returns what it could work out about it. */
interface FieldProcessor {
    fun process(field: Field, deductions: DeductionListener = DeductionListener.IGNORE): Field
}
