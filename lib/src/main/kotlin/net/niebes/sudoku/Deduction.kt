package net.niebes.sudoku

import net.niebes.sudoku.model.CellPosition

enum class Technique {
    /** The cell had exactly one candidate left. */
    NAKED_SINGLE,

    /** The value fitted in only one cell of a house. */
    HIDDEN_SINGLE,

    /** Propagation stalled, so the value is an assumption search may withdraw. */
    GUESS
}

data class Deduction(
    val technique: Technique,
    val at: CellPosition,
    val value: Int
)

/**
 * Where processors report what they worked out. Solving stays free of I/O: the caller decides
 * whether deductions are printed, asserted on in a test, or counted to rate a puzzle's difficulty.
 */
fun interface DeductionListener {
    fun onDeduction(deduction: Deduction)

    companion object {
        val IGNORE = DeductionListener { }
    }
}

/** Collects every deduction, including those from search branches later abandoned. */
class RecordingDeductionListener : DeductionListener {
    private val recorded = mutableListOf<Deduction>()

    val deductions: List<Deduction> get() = recorded

    /** Number of assumptions made, i.e. how far the puzzle outruns the techniques in the chain. */
    val guesses: Int get() = recorded.count { it.technique == Technique.GUESS }

    override fun onDeduction(deduction: Deduction) {
        recorded += deduction
    }
}

/** Prints deductions as they happen; the previous built-in behaviour, now opt-in. */
class PrintingDeductionListener : DeductionListener {
    override fun onDeduction(deduction: Deduction) {
        println("${deduction.technique}: ${deduction.at} = ${deduction.value}")
    }
}
