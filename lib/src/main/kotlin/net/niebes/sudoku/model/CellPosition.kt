package net.niebes.sudoku.model

/** Where a cell sits in the grid. Rows and columns are zero-based. */
data class CellPosition(
    val row: Int,
    val column: Int
) {
    /** Offset into a row-major grid, so cells can be looked up without scanning. */
    val index: Int get() = row * SIZE + column

    val segment: SegmentPosition by lazy { SegmentPosition.of(this) }

    companion object {
        const val SIZE = 9

        fun fromIndex(index: Int) = CellPosition(index / SIZE, index % SIZE)
    }
}
