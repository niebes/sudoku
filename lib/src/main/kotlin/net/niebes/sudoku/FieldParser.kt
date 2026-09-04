package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field

interface FieldParser {
    fun parse(input: String): Field
}

/**
 * Rows on their own lines, cells separated by [delimiter]. Formats differ only in how one cell
 * reads, so the row and column bookkeeping lives here once.
 */
open class DelimitedFieldParser(
    private val delimiter: Char,
    private val readCell: (String) -> Candidates
) : FieldParser {
    override fun parse(input: String): Field {
        val rows = input.trim().lines()
        require(rows.size == SIZE) { "expected $SIZE rows, got ${rows.size}" }

        return rows.flatMapIndexed { row, line ->
            val tokens = line.split(delimiter)
            require(tokens.size == SIZE) { "row $row has ${tokens.size} cells, expected $SIZE" }
            tokens.mapIndexed { column, token -> Cell.new(CellPosition(row, column), readCell(token)) }
        }.let { Field(it) }.requireConsistentGivens()
    }
}

/** One value per cell, blank for unknown: `4,,,8,,3,,,1`. */
class CsvFieldParser : DelimitedFieldParser(',', { token ->
    token.trim().toIntOrNull()?.let { Candidates.of(it) } ?: Candidates.NONE
})

/** Like [CsvFieldParser], but a cell may also carry a candidate list: `4||1,2,7|8`. */
class PipeFieldParser : DelimitedFieldParser('|', { token ->
    Candidates.of(token.filter { it.isDigit() || it == ',' }.split(',').mapNotNull(String::toIntOrNull))
})

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

/**
 * A puzzle whose givens already conflict is bad input, not a hard puzzle, so it is rejected here
 * rather than left for the solver to report as an ordinary contradiction.
 */
private fun Field.requireConsistentGivens(): Field = apply {
    val conflict = contradictionAt()
    require(conflict == null) { "$conflict repeats a value already given in its row, column or segment" }
}
