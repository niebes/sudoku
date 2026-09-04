package net.niebes.sudoku.deduction

/**
 * Collects the deductions that survive: when search abandons a branch, everything recorded since
 * that branch's guess is discarded, so what remains is the one line of reasoning that actually
 * reaches the result. Guesses on that line stay in the trace - they are honest steps, made without
 * a reason and vindicated by where they led.
 *
 * [RecordingDeductionListener] is the one to use when abandoned work is the point, e.g. for
 * counting how hard search had to look.
 */
class SuccessfulLineRecordingListener : DeductionListener {
    private val recorded = mutableListOf<Deduction>()
    private val branchStarts = ArrayDeque<Int>()

    val deductions: List<Deduction> get() = recorded
    val placements: List<Placement> get() = recorded.filterIsInstance<Placement>()
    val eliminations: List<Elimination> get() = recorded.filterIsInstance<Elimination>()

    /** Assumptions on the successful line, i.e. how many lucky picks the techniques still needed. */
    val guesses: Int get() = recorded.count { it.technique == Technique.GUESS }

    fun techniquesUsed(): Set<Technique> = recorded.mapTo(LinkedHashSet()) { it.technique }

    override fun onDeduction(deduction: Deduction) {
        if (deduction.technique == Technique.GUESS) branchStarts.addLast(recorded.size)
        recorded += deduction
    }

    override fun onBranchAbandoned() {
        recorded.subList(branchStarts.removeLast(), recorded.size).clear()
    }
}
