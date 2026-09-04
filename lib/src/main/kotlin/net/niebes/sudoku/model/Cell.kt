package net.niebes.sudoku.model

/** One square of the grid: either [SolvedCell] with a value, or [UnsolvedCell] with candidates. */
sealed interface Cell {
    val position: CellPosition

    /** True when this cell is unsolved and [value] is still one of its candidates. */
    fun couldBe(value: Int): Boolean

    companion object {
        /** No candidates means "nothing known about this cell", so it starts with all of them. */
        fun new(position: CellPosition, candidates: Candidates): Cell = when (candidates.size) {
            0 -> UnsolvedCell(position)
            1 -> SolvedCell(position, candidates.single())
            else -> UnsolvedCell(position, candidates)
        }
    }
}
