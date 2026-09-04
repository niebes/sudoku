package net.niebes.sudoku

import net.niebes.sudoku.model.Field

class SudokuSolver(
    private val processor: List<FieldProcessor>
) {
    constructor() : this(listOf(
        RowCandidateEliminator(),
        ColumnCandidateEliminator(),
        SegmentCandidateEliminator(),
        SingleCandidateMarker(),
        SolveSingleCandidateTransformer()
    ))

    fun solve(field: Field): SolveResult = propagate(field)

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

    private fun iterate(field: Field) =
        processor.fold(field) { acc, fieldProcessor -> fieldProcessor.process(acc) }.also { println("process") }
}
