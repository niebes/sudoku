package net.niebes.sudoku

import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.UnsolvedCell

class SudokuSolver(
    private val processor: List<FieldProcessor>,
    private val deductions: DeductionListener = DeductionListener.IGNORE
) {
    constructor(deductions: DeductionListener = DeductionListener.IGNORE) : this(listOf(
        HouseCandidateEliminator(),
        SingleCandidateMarker(),
        SolveSingleCandidateTransformer()
    ), deductions)

    fun solve(field: Field): SolveResult = search(field)

    /**
     * Propagates, then guesses when propagation stalls. Fields are immutable, so an assumption is
     * just another field and a wrong branch needs no rollback.
     */
    fun search(field: Field): SolveResult {
        val propagated = propagate(field)
        if (propagated !is SolveResult.Stalled) return propagated

        val pivot = propagated.field.pivot() ?: return SolveResult.Solved(propagated.field)
        pivot.candidates.values.forEach { candidate ->
            deductions.onDeduction(Deduction(Technique.GUESS, pivot.position, candidate))
            val attempt = search(propagated.field.assign(pivot.position, candidate))
            if (attempt is SolveResult.Solved) return attempt
        }
        return SolveResult.Contradiction(propagated.field, pivot.position)
    }

    /** Runs the processor chain until it solves the field, contradicts itself, or stops making progress. */
    fun propagate(field: Field): SolveResult {
        var current = field
        while (true) {
            current.contradictionAt()?.let { return SolveResult.Contradiction(current, it) }
            if (current.isSolved()) return SolveResult.Solved(current)

            val next = iterate(current)
            if (next == current) return SolveResult.Stalled(current)
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
        processor.fold(field) { acc, fieldProcessor -> fieldProcessor.process(acc, deductions) }
}
