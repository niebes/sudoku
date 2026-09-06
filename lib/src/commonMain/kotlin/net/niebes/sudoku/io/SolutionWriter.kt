package net.niebes.sudoku.io

import net.niebes.sudoku.Solved
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field

/** Solved cells as their value, unsolved ones as their remaining candidates. */
class SolutionWriter : FieldWriter {
    override fun render(field: Field): String = field.cells
        .chunked(CellPosition.SIZE) { row -> row.joinToString(" | ") }
        .joinToString("\n")
}
