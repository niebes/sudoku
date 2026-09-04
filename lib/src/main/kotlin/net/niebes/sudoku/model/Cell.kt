package net.niebes.sudoku.model

sealed interface Cell {
    val position: CellPosition

    companion object {
        /** No candidates means "nothing known about this cell", so it starts with all of them. */
        fun new(position: CellPosition, candidates: Candidates): Cell = when (candidates.size) {
            0 -> UnsolvedCell(position)
            1 -> SolvedCell(position, candidates.single())
            else -> UnsolvedCell(position, candidates)
        }
    }
}

data class SolvedCell(
    override val position: CellPosition,
    val value: Int
) : Cell {
    override fun toString(): String = value.toString()
}

data class UnsolvedCell(
    override val position: CellPosition,
    val candidates: Candidates = Candidates.ALL
) : Cell {
    override fun toString(): String = candidates.toString()
}
