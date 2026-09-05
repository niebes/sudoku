package net.niebes.sudoku.deduction

import net.niebes.sudoku.model.CellPosition

/**
 * Something a technique worked out about one cell.
 *
 * [because] is the evidence: the cells whose state justifies the conclusion, in the order the
 * technique's argument visits them - a chain reads end to end, a wing pivot-first. It exists so a
 * consumer can show *why* a deduction holds on this grid rather than recite what the technique is
 * in general. Empty means the reason is not a set of cells: a naked single is justified by the
 * cell's own candidates, a guess by nothing at all.
 */
sealed interface Deduction {
    val technique: Technique
    val at: CellPosition
    val because: List<CellPosition>
}
