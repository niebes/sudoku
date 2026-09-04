package net.niebes.sudoku.model

enum class HouseKind { ROW, COLUMN, SEGMENT }

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
    fun candidatesFor(value: Int): List<UnsolvedCell> =
        unsolved().filter { it.candidates.contains(value) }

    override fun toString(): String = "$kind $index"
}

/**
 * The three cells a segment shares with a row or column. A digit confined to one side of the overlap
 * is excluded from the other, which is the whole of locked candidates.
 */
data class Intersection(
    val segment: House,
    val line: House,
    val cells: List<Cell>
) {
    /** Cells of the segment that lie outside the overlap. */
    fun segmentOnly(): List<Cell> = outside(segment)

    /** Cells of the line that lie outside the overlap. */
    fun lineOnly(): List<Cell> = outside(line)

    private fun outside(house: House): List<Cell> {
        val shared = cells.mapTo(HashSet(cells.size)) { it.position }
        return house.cells.filterNot { it.position in shared }
    }

    override fun toString(): String = "$segment / $line"
}
