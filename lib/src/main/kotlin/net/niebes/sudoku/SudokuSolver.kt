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

    fun solve(field: Field): Field {
        var previousResult = field
        do {
            val after = iterate(previousResult)
            if (previousResult == after) return after
            else previousResult = after
        } while (previousResult == after)

        return previousResult
    }

    private fun iterate(field: Field) =
        processor.fold(field, { acc, fieldProcessor -> fieldProcessor.process(acc) }).also { println("process") }
}
