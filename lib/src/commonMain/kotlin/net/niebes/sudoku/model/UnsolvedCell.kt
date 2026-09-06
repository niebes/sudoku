package net.niebes.sudoku.model

/** A cell narrowed to [candidates] but not yet settled. */
data class UnsolvedCell(
    override val position: CellPosition,
    val candidates: Candidates = Candidates.ALL
) : Cell {
    override fun couldBe(value: Int): Boolean = candidates.contains(value)

    override fun toString(): String = candidates.toString()
}
