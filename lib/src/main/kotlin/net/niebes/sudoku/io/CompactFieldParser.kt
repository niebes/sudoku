package net.niebes.sudoku.io

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field

/**
 * The single-line 81-character format published puzzle sets use, where any non-digit is an unknown:
 * `53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79`.
 */
class CompactFieldParser : FieldParser {
    override fun parse(input: String): Field {
        val values = input.filterNot(Char::isWhitespace)
        require(values.length == SIZE * SIZE) { "expected ${SIZE * SIZE} cells, got ${values.length}" }

        return values.mapIndexed { index, value ->
            Cell.new(
                CellPosition.fromIndex(index),
                if (value in '1'..'9') Candidates.of(value - '0') else Candidates.NONE
            )
        }.let { Field(it) }.requireConsistentGivens()
    }
}
