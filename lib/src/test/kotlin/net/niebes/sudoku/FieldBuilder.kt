package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.UnsolvedCell

/**
 * Builds a field cell by cell, for tests that need a specific candidate layout rather than a
 * puzzle. Every cell starts unsolved with all nine candidates; rows and columns are zero-based.
 */
internal class FieldBuilder {
    private val cells: MutableMap<CellPosition, Cell> = (0 until 81)
        .map { CellPosition.fromIndex(it) }
        .associateWithTo(LinkedHashMap()) { UnsolvedCell(it) }

    fun solved(row: Int, column: Int, value: Int) = apply {
        val position = CellPosition(row, column)
        cells[position] = SolvedCell(position, value)
    }

    fun candidates(row: Int, column: Int, vararg values: Int) = apply {
        val position = CellPosition(row, column)
        cells[position] = UnsolvedCell(position, Candidates.of(*values))
    }

    /** Restricts a whole row at once, for setting up the rest of a house quickly. */
    fun rowCandidates(row: Int, columns: IntRange, vararg values: Int) = apply {
        columns.forEach { candidates(row, it, *values) }
    }

    /** Fills every cell from a compact solution string except those left [open]. */
    fun solvedFrom(compact: String, open: Set<CellPosition>) = apply {
        compact.forEachIndexed { index, digit ->
            val position = CellPosition.fromIndex(index)
            if (position !in open) solved(position.row, position.column, digit - '0')
        }
    }

    fun build(): Field = Field(cells.values)
}

internal fun field(build: FieldBuilder.() -> Unit): Field = FieldBuilder().apply(build).build()
