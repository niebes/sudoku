package net.niebes.sudoku

import net.niebes.sudoku.model.Field

/** Every cell carries a value, and no house repeats one. */
data class Solved(
    override val field: Field
) : SolveResult
