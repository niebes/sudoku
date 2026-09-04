package net.niebes.sudoku

import net.niebes.sudoku.deduction.DeductionListener
import net.niebes.sudoku.deduction.Placement
import net.niebes.sudoku.deduction.PrintingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.UnsolvedCell
import net.niebes.sudoku.technique.BasicFishEliminator
import net.niebes.sudoku.technique.ClaimingEliminator
import net.niebes.sudoku.technique.FieldProcessor
import net.niebes.sudoku.technique.FullHouseSolver
import net.niebes.sudoku.technique.HiddenSubsetEliminator
import net.niebes.sudoku.technique.HouseCandidateEliminator
import net.niebes.sudoku.technique.NakedSubsetEliminator
import net.niebes.sudoku.technique.PointingEliminator
import net.niebes.sudoku.technique.SingleCandidateMarker
import net.niebes.sudoku.technique.SolveSingleCandidateTransformer
import net.niebes.sudoku.technique.TurbotFishEliminator
import net.niebes.sudoku.technique.XyWingEliminator
import net.niebes.sudoku.technique.XyzWingEliminator

/**
 * Applies its techniques in order, cheapest level first, until the field stops changing.
 *
 * The default chain runs levels one to three: singles, then locked candidates, then subsets. Every
 * technique below the singles is there to create work for them - only singles place values.
 */
class SudokuSolver(
    val processors: List<FieldProcessor>,
    private val deductions: DeductionListener = DeductionListener.IGNORE
) {
    constructor(deductions: DeductionListener = DeductionListener.IGNORE) : this(listOf(
        FullHouseSolver(),
        HouseCandidateEliminator(),
        SingleCandidateMarker(),
        SolveSingleCandidateTransformer(),
        PointingEliminator(),
        ClaimingEliminator(),
        NakedSubsetEliminator(),
        HiddenSubsetEliminator(),
        BasicFishEliminator(),
        TurbotFishEliminator(),
        XyWingEliminator(),
        XyzWingEliminator()
    ), deductions)

    fun solve(field: Field): SolveResult = search(field)

    /**
     * Propagates, then guesses when propagation stalls. Fields are immutable, so an assumption is
     * just another field and a wrong branch needs no rollback.
     */
    fun search(field: Field): SolveResult {
        val propagated = propagate(field)
        if (propagated !is Stalled) return propagated

        val pivot = propagated.field.pivot() ?: return Solved(propagated.field)
        pivot.candidates.values.forEach { candidate ->
            deductions.onDeduction(Placement(Technique.GUESS, pivot.position, candidate))
            val attempt = search(propagated.field.assign(pivot.position, candidate))
            if (attempt is Solved) return attempt
        }
        return Contradiction(propagated.field, pivot.position)
    }

    /** Runs the processor chain until it solves the field, contradicts itself, or stops making progress. */
    fun propagate(field: Field): SolveResult {
        var current = field
        while (true) {
            current.contradictionAt()?.let { return Contradiction(current, it) }
            if (current.isSolved()) return Solved(current)

            val next = iterate(current)
            if (next == current) return Stalled(current)
            current = next
        }
    }

    /**
     * Minimum-remaining-values: branching on the most constrained cell keeps the search tree small.
     * Ties break on position so a given puzzle always explores the same tree.
     */
    private fun Field.pivot(): UnsolvedCell? = unsolved()
        .minWithOrNull(compareBy({ it.candidates.size }, { it.position.row }, { it.position.column }))

    private fun iterate(field: Field) =
        processors.fold(field) { acc, fieldProcessor -> fieldProcessor.process(acc, deductions) }
}
