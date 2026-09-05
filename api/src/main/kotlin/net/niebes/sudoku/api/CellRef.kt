package net.niebes.sudoku.api

import net.niebes.sudoku.model.CellPosition

/** A cell address on the wire. Rows and columns stay zero-based, exactly as the model counts. */
data class CellRef(val row: Int, val column: Int) {
    companion object {
        fun of(position: CellPosition) = CellRef(position.row, position.column)
    }
}
