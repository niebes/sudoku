package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.DeductionListener
import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.UnsolvedCell

/**
 * A technique that only removes candidates.
 *
 * [eliminations] reads the field it is handed and nothing else, and the whole batch is applied
 * afterwards. That is deliberate: a technique that eliminated as it went would go on to reason
 * about a grid its own earlier steps had already changed, and every family of technique above
 * singles is vulnerable to that - two candidates claiming one cell, or a digit looking confined to
 * a house because a cell that was just solved dropped out of the list being scanned.
 */
interface EliminationTechnique : FieldProcessor {
    val technique: Technique

    /** Must depend only on [field]. Overlapping or repeated eliminations are harmless. */
    fun eliminations(field: Field): List<Elimination>

    override fun process(field: Field, deductions: DeductionListener): Field {
        val found = eliminations(field)
        if (found.isEmpty()) return field

        val cells = field.cells.toMutableList()
        found.forEach { elimination ->
            val cell = cells[elimination.at.index]
            if (cell !is UnsolvedCell) return@forEach

            val removable = cell.candidates and elimination.values
            if (removable.isEmpty()) return@forEach

            deductions.onDeduction(Elimination(technique, elimination.at, removable, elimination.because))
            cells[elimination.at.index] = UnsolvedCell(cell.position, cell.candidates - removable)
        }
        return Field(cells)
    }
}
