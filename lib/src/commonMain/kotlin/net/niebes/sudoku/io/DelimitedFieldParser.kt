package net.niebes.sudoku.io

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field

/**
 * Rows on their own lines, cells separated by [delimiter]. Formats differ only in how one cell
 * reads, so the row and column bookkeeping lives here once.
 */
open class DelimitedFieldParser(
    private val delimiter: Char,
    private val readCell: (String) -> Candidates
) : FieldParser {
    override fun parse(input: String): Field {
        val rows = input.trim().lines()
        require(rows.size == SIZE) { "expected $SIZE rows, got ${rows.size}" }

        return rows.flatMapIndexed { row, line ->
            val tokens = line.split(delimiter)
            require(tokens.size == SIZE) { "row $row has ${tokens.size} cells, expected $SIZE" }
            tokens.mapIndexed { column, token -> Cell.new(CellPosition(row, column), readCell(token)) }
        }.let { Field(it) }.requireConsistentGivens()
    }
}
