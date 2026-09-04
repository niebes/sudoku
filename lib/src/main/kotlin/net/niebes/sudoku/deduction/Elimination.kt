package net.niebes.sudoku.deduction

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition

/** Values were proven not to belong in a cell. */
data class Elimination(
    override val technique: Technique,
    override val at: CellPosition,
    val values: Candidates
) : Deduction {
    override fun toString(): String = "$technique: $at cannot be $values"
}
