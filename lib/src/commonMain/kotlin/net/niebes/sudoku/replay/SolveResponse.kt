package net.niebes.sudoku.replay

/**
 * The solve and the reasoning behind it. [grid] is where the steps actually end - identical to
 * [solution] when solved, honestly partial when stalled. [guesses] counts the steps that were
 * assumptions rather than deductions; they also appear in [steps] under their own kind.
 */
data class SolveResponse(
    val outcome: Outcome,
    val givens: String? = null,
    val solution: String? = null,
    val grid: String? = null,
    val steps: List<Step> = emptyList(),
    val guesses: Int = 0,
    val message: String? = null
)
