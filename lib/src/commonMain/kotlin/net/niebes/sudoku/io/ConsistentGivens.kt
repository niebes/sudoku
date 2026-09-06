package net.niebes.sudoku.io

import net.niebes.sudoku.model.Field

/**
 * A puzzle whose givens already conflict is bad input, not a hard puzzle, so it is rejected as it
 * is read rather than left for the solver to report as an ordinary contradiction.
 *
 * This belongs to reading, not to the model: propagation legitimately passes through inconsistent
 * intermediate states that search has to handle as values, so Field itself must not refuse them.
 */
internal fun Field.requireConsistentGivens(): Field = apply {
    val conflict = contradictionAt()
    require(conflict == null) { "$conflict repeats a value already given in its row, column or segment" }
}
