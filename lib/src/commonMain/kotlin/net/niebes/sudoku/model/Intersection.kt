package net.niebes.sudoku.model

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
