package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition

enum class Technique {
    /** A house had one unsolved cell left, so the missing value goes there. */
    FULL_HOUSE,

    /** The cell had exactly one candidate left. */
    NAKED_SINGLE,

    /** The value fitted in only one cell of a house. */
    HIDDEN_SINGLE,

    /** A value already placed in a house cannot be a candidate elsewhere in it. */
    PEER_ELIMINATION,

    /** Within a segment the value fitted only on one line, so it leaves the rest of that line. */
    POINTING,

    /** Along a line the value fitted only in one segment, so it leaves the rest of that segment. */
    CLAIMING,

    /** Propagation stalled, so the value is an assumption search may withdraw. */
    GUESS
}

/** Something a technique worked out about one cell. */
sealed interface Deduction {
    val technique: Technique
    val at: CellPosition
}

/** A value was proven to belong in a cell. */
data class Placement(
    override val technique: Technique,
    override val at: CellPosition,
    val value: Int
) : Deduction {
    override fun toString(): String = "$technique: $at = $value"
}

/** Values were proven not to belong in a cell. */
data class Elimination(
    override val technique: Technique,
    override val at: CellPosition,
    val values: Candidates
) : Deduction {
    override fun toString(): String = "$technique: $at cannot be $values"
}

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

/** Prints deductions as they happen. */
class PrintingDeductionListener : DeductionListener {
    override fun onDeduction(deduction: Deduction) = println(deduction)
}
