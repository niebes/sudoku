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

}
