package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.DeductionListener
import net.niebes.sudoku.deduction.Placement
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.UnsolvedCell

/** Naked singles: a cell with one candidate left is solved. */
class SolveSingleCandidateTransformer : FieldProcessor {
    override fun process(field: Field, deductions: DeductionListener): Field = Field(field.cells.map { cell ->
        when (cell) {
            is SolvedCell -> cell
            is UnsolvedCell -> when (cell.candidates.size) {
                // A cell with no candidates is left as-is; the solver reports it as a contradiction.
                0 -> cell
                1 -> {
                    val value = cell.candidates.single()
                    deductions.onDeduction(Placement(Technique.NAKED_SINGLE, cell.position, value))
                    SolvedCell(cell.position, value)
                }
                else -> cell
            }
        }
    })
}
