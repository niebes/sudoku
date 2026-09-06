package net.niebes.sudoku.model

/** Which of the nine 3x3 segments a cell falls in, addressed as a coarse row and column. */
data class SegmentPosition(
    val row: Int,
    val column: Int
) {
    companion object {
        private const val SIDE = 3

        fun of(cellPosition: CellPosition): SegmentPosition =
            SegmentPosition(cellPosition.row / SIDE, cellPosition.column / SIDE)
    }
}
