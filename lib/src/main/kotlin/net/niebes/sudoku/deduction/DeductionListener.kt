package net.niebes.sudoku.deduction

/**
 * Where techniques report what they worked out. Solving stays free of I/O: the caller decides
 * whether deductions are printed, asserted on in a test, or counted to rate a puzzle's difficulty.
 */
fun interface DeductionListener {
    fun onDeduction(deduction: Deduction)

    companion object {
        val IGNORE = DeductionListener { }
    }
}
