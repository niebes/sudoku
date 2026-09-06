package net.niebes.sudoku

import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field

/** [at] proves the field cannot be completed: it ran out of candidates, or it repeats a value. */
data class Contradiction(
    override val field: Field,
    val at: CellPosition
) : SolveResult
