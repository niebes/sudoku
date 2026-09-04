package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.UnsolvedCell

interface FieldProcessor {
    fun process(field: Field): Field
}

interface UnsolvedCellFieldProcessor : FieldProcessor {
    fun processUnsolved(field: Field, cell: UnsolvedCell): UnsolvedCell
    override fun process(field: Field): Field = Field(field.cells.map { cell ->
        when (cell) {
            is SolvedCell -> cell
            is UnsolvedCell -> processUnsolved(field, cell)
        }
    })
}

/**
 * A value already placed in a cell's row, column or segment cannot be a candidate for that cell.
 * Row, column and segment are the same constraint - a house - so this is one technique, not three.
 */
class HouseCandidateEliminator : UnsolvedCellFieldProcessor {
    override fun processUnsolved(field: Field, cell: UnsolvedCell): UnsolvedCell =
        UnsolvedCell(cell.position, cell.candidates - field.solvedPeers(cell.position))
}

/** Hidden singles: a candidate that fits in only one cell of a house belongs to that cell. */
class SingleCandidateMarker : FieldProcessor {
    override fun process(field: Field): Field {
        val cells = field.cells.toMutableList()
        // Positions are fixed for the life of a field, so the houses can be taken once up front.
        field.houses().map { house -> house.map { it.position } }.forEach { mark(cells, it) }
        return Field(cells)
    }

    /**
     * Cell state is re-read from [cells] on every lookup: narrowing one cell changes which candidates
     * are still hidden singles, so deciding against a stale snapshot lets two candidates claim the
     * same cell.
     */
    private fun mark(cells: MutableList<Cell>, house: List<CellPosition>) {
        (1..9).forEach candidate@{ candidate ->
            val current = house.map { cells[it.index] }
            if (current.any { it is SolvedCell && it.value == candidate }) return@candidate

            val only = current.filterIsInstance<UnsolvedCell>()
                .filter { it.candidates.contains(candidate) }
                .singleOrNull() ?: return@candidate
            if (only.candidates.size == 1) return@candidate

            println("mark only occurrence of $candidate in ${only.position}")
            cells[only.position.index] = UnsolvedCell(only.position, Candidates.of(candidate))
        }
    }
}

/** Naked singles: a cell with one candidate left is solved. */
class SolveSingleCandidateTransformer : FieldProcessor {
    override fun process(field: Field): Field = Field(field.cells.map { cell ->
        when (cell) {
            is SolvedCell -> cell
            is UnsolvedCell -> when (cell.candidates.size) {
                // A cell with no candidates is left as-is; the solver reports it as a contradiction.
                0 -> cell
                1 -> {
                    println("solved ${cell.position} to ${cell.candidates.single()}")
                    SolvedCell(cell.position, cell.candidates.single())
                }
                else -> cell
            }
        }
    })
}
