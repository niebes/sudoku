package net.niebes.sudoku

import net.niebes.sudoku.model.Field

/**
 * Outcome of a solve attempt. A contradiction is an ordinary value rather than an exception because
 * search treats it as the expected result of a wrong assumption, not as an error.
 */
sealed interface SolveResult {
    val field: Field
}
