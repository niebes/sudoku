package net.niebes.sudoku.api

/**
 * A puzzle in the 81-character compact format, any non-digit an unknown. [allowGuessing] false
 * stops at what the technique chain alone can deduce, rather than letting search finish the grid.
 */
data class SolveRequest(val puzzle: String, val allowGuessing: Boolean = true)
