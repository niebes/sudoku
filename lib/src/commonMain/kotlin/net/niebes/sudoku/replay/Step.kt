package net.niebes.sudoku.replay

/**
 * One deduction, ready to show: what happened, where, the cells that justify it, and a sentence
 * saying why - in terms of this grid, not of the technique in general.
 */
data class Step(
    val technique: String,
    val kind: StepKind,
    val at: CellRef,
    val value: Int? = null,
    val values: List<Int> = emptyList(),
    val because: List<CellRef> = emptyList(),
    val explanation: String
)
