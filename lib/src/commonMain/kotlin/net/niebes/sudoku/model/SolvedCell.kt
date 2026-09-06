package net.niebes.sudoku.model

/** A cell whose value is settled. */
data class SolvedCell(
    override val position: CellPosition,
    val value: Int
) : Cell {
    override fun couldBe(value: Int): Boolean = false

    override fun toString(): String = value.toString()
}
