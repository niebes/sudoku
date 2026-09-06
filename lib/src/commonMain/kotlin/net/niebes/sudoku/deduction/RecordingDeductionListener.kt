package net.niebes.sudoku.deduction

/** Collects every deduction, including those from search branches later abandoned. */
class RecordingDeductionListener : DeductionListener {
    private val recorded = mutableListOf<Deduction>()

    val deductions: List<Deduction> get() = recorded
    val placements: List<Placement> get() = recorded.filterIsInstance<Placement>()
    val eliminations: List<Elimination> get() = recorded.filterIsInstance<Elimination>()

    /** Number of assumptions made, i.e. how far the puzzle outruns the techniques in the chain. */
    val guesses: Int get() = recorded.count { it.technique == Technique.GUESS }

    fun techniquesUsed(): Set<Technique> = recorded.mapTo(LinkedHashSet()) { it.technique }

    override fun onDeduction(deduction: Deduction) {
        recorded += deduction
    }
}
