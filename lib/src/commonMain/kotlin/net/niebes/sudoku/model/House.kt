package net.niebes.sudoku.model

/**
 * One of the 27 constraint groups, tagged with which kind it is. Techniques above the level of a
 * single cell need to tell them apart - locked candidates relates a segment to a line, and fish
 * relate rows to columns - so a bare list of cells is not enough.
 */
data class House(
    val kind: HouseKind,
    val index: Int,
    val cells: List<Cell>
) {
    fun unsolved(): List<UnsolvedCell> = cells.filterIsInstance<UnsolvedCell>()

    /** True if [value] is already placed here, so no cell in this house may still take it. */
    fun holds(value: Int): Boolean = cells.any { it is SolvedCell && it.value == value }

    /** Unsolved cells that could still take [value]. */
    fun candidatesFor(value: Int): List<UnsolvedCell> = unsolved().filter { it.couldBe(value) }

    override fun toString(): String = "$kind $index"
}
