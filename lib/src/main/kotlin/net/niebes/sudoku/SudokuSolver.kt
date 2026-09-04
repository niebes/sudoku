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

    /** Runs the processor chain until an iteration no longer changes the field. */
    fun solve(field: Field): Field {
        var current = field
        while (true) {
            val next = iterate(current)
            if (next == current) return next
            current = next
        }
    }

    private fun iterate(field: Field) =
        processor.fold(field) { acc, fieldProcessor -> fieldProcessor.process(acc) }.also { println("process") }
}
