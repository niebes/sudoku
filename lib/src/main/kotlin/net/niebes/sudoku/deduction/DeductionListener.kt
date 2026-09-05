package net.niebes.sudoku.deduction

/**
 * Where techniques report what they worked out. Solving stays free of I/O: the caller decides
 * whether deductions are printed, asserted on in a test, or counted to rate a puzzle's difficulty.
 */
fun interface DeductionListener {
    fun onDeduction(deduction: Deduction)

    /**
     * Search withdrew its most recent guess: every deduction since that guess - the guess included -
     * described a grid that turned out impossible, not the puzzle. A listener that replays or
     * teaches from the trace must discard that stretch; one that only counts work may ignore this.
     */
    fun onBranchAbandoned() {}

    companion object {
        val IGNORE = DeductionListener { }
    }
}
