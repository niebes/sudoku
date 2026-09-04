package net.niebes.sudoku.model

data class Field(
    val cells: Set<Cell>
) {
    init {
        val position = cells.map { it.position }
        require(position.groupBy { it.row }.size == 9)
        require(position.minBy { it.row }?.row == 0)
        require(position.maxBy { it.row }?.row == 8)
        require(position.groupBy { it.column }.size == 9)
        require(position.minBy { it.column }?.column == 0)
        require(position.maxBy { it.column }?.column == 8)
    }

    fun getRow(row: Int): Set<Cell> = cells.filter { it.position.row == row }.toSet()
    fun getColumn(column: Int): Set<Cell> = cells.filter { it.position.column == column }.toSet()
    fun getSegment(segmentPosition: CellPosition.SegmentPosition): Set<Cell> = cells.filter { it.position.segment == segmentPosition }.toSet()

    /** Returns a copy with [position] fixed to [value]. The model is immutable, so search needs no undo. */
    fun assign(position: CellPosition, value: Int): Field =
        Field(cells.mapTo(LinkedHashSet(cells.size)) { if (it.position == position) SolvedCell(position, value) else it })

    fun unsolved(): List<UnsolvedCell> = cells.filterIsInstance<UnsolvedCell>()

    fun isSolved(): Boolean = cells.all { it is SolvedCell }

    /** The 27 constraint groups: nine rows, nine columns, nine segments. */
    fun houses(): List<Set<Cell>> =
        (0..8).map { getRow(it) } +
            (0..8).map { getColumn(it) } +
            (0..2).flatMap { r -> (0..2).map { c -> getSegment(CellPosition.SegmentPosition(r, c)) } }

    /** The three houses containing [position]: its row, its column and its segment. */
    fun housesOf(position: CellPosition): List<Set<Cell>> =
        listOf(getRow(position.row), getColumn(position.column), getSegment(position.segment))

    /**
     * Position proving the field cannot be completed, or null if it is still consistent. Both a cell
     * with no candidates left and two cells holding the same value in one house count: propagation is
     * incomplete, so it can place a duplicate rather than exhaust a cell, and search must catch either.
     */
    fun contradictionAt(): CellPosition? =
        unsolved().firstOrNull { it.candidates.values.isEmpty() }?.position ?: duplicateValueAt()

    private fun duplicateValueAt(): CellPosition? {
        houses().forEach { house ->
            val seen = HashMap<Int, CellPosition>()
            house.filterIsInstance<SolvedCell>().forEach { cell ->
                if (seen.put(cell.value, cell.position) != null) return cell.position
            }
        }
        return null
    }
}
