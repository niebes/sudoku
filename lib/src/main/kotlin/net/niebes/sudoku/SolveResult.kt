package net.niebes.sudoku

import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field

/**
 * Outcome of a solve attempt. A contradiction is an ordinary value rather than an exception
 * because search treats it as the expected result of a wrong assumption, not as an error.
 */
sealed interface SolveResult {
    val field: Field

    /** Every cell carries a value. */
    data class Solved(override val field: Field) : SolveResult

    /** No technique in the chain can make further progress; [field] is partially solved. */
    data class Stalled(override val field: Field) : SolveResult

    /** [at] ran out of candidates, so the field as given cannot be completed. */
    data class Contradiction(override val field: Field, val at: CellPosition) : SolveResult
}
