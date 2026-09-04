package net.niebes.sudoku.model

import net.niebes.sudoku.model.CellPosition.Companion.SIZE

/**
 * A 9x9 grid stored in row-major order, so a cell's slot is [CellPosition.index] and lookup is an
 * array read rather than a scan of all 81 cells. House membership and the 20 peers of each position
 * are the same for every field, so they are computed once and shared.
 *
 * The constructor normalises whatever order it is handed, which keeps [equals] canonical.
 */
class Field(cells: Collection<Cell>) {
    val cells: List<Cell> = rowMajor(cells)

    fun cellAt(position: CellPosition): Cell = cells[position.index]

    fun getRow(row: Int): List<Cell> = ROWS[row].cellsOf()
    fun getColumn(column: Int): List<Cell> = COLUMNS[column].cellsOf()
    fun getSegment(segmentPosition: CellPosition.SegmentPosition): List<Cell> =
        SEGMENTS[segmentPosition.row * 3 + segmentPosition.column].cellsOf()

    /** The 27 constraint groups: nine rows, nine columns, nine segments. */
    fun houses(): List<List<Cell>> = HOUSES.map { it.cellsOf() }

    /** The three houses containing [position]: its row, its column and its segment. */
    fun housesOf(position: CellPosition): List<List<Cell>> = HOUSES_OF[position.index].map { it.cellsOf() }

    /** Values already placed in any house of [position], and so unavailable to it. */
    fun solvedPeers(position: CellPosition): Candidates {
        var placed = Candidates.NONE
        for (index in PEERS[position.index]) {
            val peer = cells[index]
            if (peer is SolvedCell) placed += peer.value
        }
        return placed
    }

    /** Returns a copy with [position] fixed to [value]. The model is immutable, so search needs no undo. */
    fun assign(position: CellPosition, value: Int): Field =
        Field(cells.toMutableList().also { it[position.index] = SolvedCell(position, value) })

    fun unsolved(): List<UnsolvedCell> = cells.filterIsInstance<UnsolvedCell>()

    fun isSolved(): Boolean = cells.all { it is SolvedCell }

    /**
     * Position proving the field cannot be completed, or null if it is still consistent. Both a cell
     * with no candidates left and two cells holding the same value in one house count: propagation is
     * incomplete, so it can place a duplicate rather than exhaust a cell, and search must catch either.
     */
    fun contradictionAt(): CellPosition? =
        cells.filterIsInstance<UnsolvedCell>().firstOrNull { it.candidates.isEmpty() }?.position
            ?: duplicateValueAt()

    private fun duplicateValueAt(): CellPosition? {
        for (house in HOUSES) {
            var seen = Candidates.NONE
            for (index in house) {
                val cell = cells[index]
                if (cell is SolvedCell) {
                    if (seen.contains(cell.value)) return cell.position
                    seen += cell.value
                }
            }
        }
        return null
    }

    private fun IntArray.cellsOf(): List<Cell> = map { cells[it] }

    override fun equals(other: Any?): Boolean = this === other || (other is Field && cells == other.cells)
    override fun hashCode(): Int = cells.hashCode()
    override fun toString(): String = "Field(cells=$cells)"

    companion object {
        private const val CELL_COUNT = SIZE * SIZE

        private val ROWS: List<IntArray> = (0 until SIZE).map { row -> IntArray(SIZE) { row * SIZE + it } }
        private val COLUMNS: List<IntArray> = (0 until SIZE).map { column -> IntArray(SIZE) { it * SIZE + column } }
        private val SEGMENTS: List<IntArray> = (0 until SIZE).map { segment ->
            val firstRow = segment / 3 * 3
            val firstColumn = segment % 3 * 3
            IntArray(SIZE) { (firstRow + it / 3) * SIZE + firstColumn + it % 3 }
        }
        private val HOUSES: List<IntArray> = ROWS + COLUMNS + SEGMENTS
        private val HOUSES_OF: List<List<IntArray>> =
            (0 until CELL_COUNT).map { index -> HOUSES.filter { index in it } }
        private val PEERS: List<IntArray> = (0 until CELL_COUNT).map { index ->
            HOUSES_OF[index].flatMap { it.asIterable() }.filter { it != index }.distinct().toIntArray()
        }

        private fun rowMajor(cells: Collection<Cell>): List<Cell> {
            require(cells.size == CELL_COUNT) { "a field holds $CELL_COUNT cells, got ${cells.size}" }
            val ordered = arrayOfNulls<Cell>(CELL_COUNT)
            cells.forEach { cell ->
                require(ordered[cell.position.index] == null) { "duplicate cell at ${cell.position}" }
                ordered[cell.position.index] = cell
            }
            @Suppress("UNCHECKED_CAST")
            return (ordered as Array<Cell>).asList()
        }
    }
}
