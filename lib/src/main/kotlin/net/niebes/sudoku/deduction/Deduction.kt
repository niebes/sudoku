package net.niebes.sudoku.deduction

import net.niebes.sudoku.model.CellPosition

/** Something a technique worked out about one cell. */
sealed interface Deduction {
    val technique: Technique
    val at: CellPosition
}
