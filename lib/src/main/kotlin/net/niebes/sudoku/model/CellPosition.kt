package net.niebes.sudoku.model

data class CellPosition(
    val row: Int,
    val column: Int
) {
    data class SegmentPosition(
        val row: Int,
        val column: Int
    ) {
        companion object {
            private const val maxRows = 3
            private const val maxColumns = 3

            fun fromCellPosition(cellPosition: CellPosition): SegmentPosition =
                SegmentPosition(cellPosition.row / maxRows, cellPosition.column / maxColumns)
        }
    }

    val segment: SegmentPosition by lazy { SegmentPosition.fromCellPosition(this) }
}
