package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.DeductionListener
import net.niebes.sudoku.deduction.Placement
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.UnsolvedCell

/** Hidden singles: a candidate that fits in only one cell of a house belongs to that cell. */
class SingleCandidateMarker : FieldProcessor {
    override fun process(field: Field, deductions: DeductionListener): Field {
        val cells = field.cells.toMutableList()
        // Positions are fixed for the life of a field, so the houses can be taken once up front.
        field.houses().map { house -> house.cells.map { it.position } }.forEach { mark(cells, it, deductions) }
        return Field(cells)
    }

    /**
     * Cell state is re-read from [cells] on every lookup: narrowing one cell changes which candidates
     * are still hidden singles, so deciding against a stale snapshot lets two candidates claim the
     * same cell.
     */
    private fun mark(cells: MutableList<Cell>, house: List<CellPosition>, deductions: DeductionListener) {
        (1..SIZE).forEach candidate@{ candidate ->
            val current = house.map { cells[it.index] }
            if (current.any { it is SolvedCell && it.value == candidate }) return@candidate

            val only = current.filter { it.couldBe(candidate) }.singleOrNull() as? UnsolvedCell ?: return@candidate
            if (only.candidates.size == 1) return@candidate

            deductions.onDeduction(Placement(Technique.HIDDEN_SINGLE, only.position, candidate))
            cells[only.position.index] = UnsolvedCell(only.position, Candidates.of(candidate))
        }
    }
}
