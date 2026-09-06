package net.niebes.sudoku.deduction

import net.niebes.sudoku.model.CellPosition

/** A value was proven to belong in a cell. */
data class Placement(
    override val technique: Technique,
    override val at: CellPosition,
    val value: Int,
    override val because: List<CellPosition> = emptyList()
) : Deduction {
    override fun toString(): String = "$technique: $at = $value"
}
