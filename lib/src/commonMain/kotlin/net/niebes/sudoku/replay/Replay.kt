package net.niebes.sudoku.replay

/** The steps a trace replays to, and the compact grid they end on - partial if the trace was. */
data class Replay(val steps: List<Step>, val grid: String)
