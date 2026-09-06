package net.niebes.sudoku.io

import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell

/** The inverse of [CompactFieldParser]: solved cells as their digit, unsolved ones as a dot. */
class CompactFieldWriter : FieldWriter {
    override fun render(field: Field): String =
        field.cells.joinToString("") { if (it is SolvedCell) it.value.toString() else "." }
}
