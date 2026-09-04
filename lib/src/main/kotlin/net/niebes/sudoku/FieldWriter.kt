package net.niebes.sudoku

import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.UnsolvedCell

interface FieldWriter {
    fun writeField(field: Field)
}

class SolutionWriter : FieldWriter {
    override fun writeField(field: Field) {
        val rows = field.cells.groupBy { it.position.row }
        rows.values.joinToString("\n") { cells ->
            cells
                .sortedBy { it.position.column }
                .joinToString(" | ") { it.getValue() }
        }.let { print(it) }
    }

    fun Cell.getValue(): String = when (this) {
        is SolvedCell -> value.toString()
        is UnsolvedCell -> candidates.values.joinToString(",","{","}")
    }
}
