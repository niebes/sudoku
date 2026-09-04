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

    /** Offset into a row-major grid, so cells can be looked up without scanning. */
    val index: Int get() = row * SIZE + column

    val segment: SegmentPosition by lazy { SegmentPosition.fromCellPosition(this) }

    companion object {
        const val SIZE = 9

        fun fromIndex(index: Int) = CellPosition(index / SIZE, index % SIZE)
    }
}
