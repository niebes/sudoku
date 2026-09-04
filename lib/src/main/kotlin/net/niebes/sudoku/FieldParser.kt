package net.niebes.sudoku

import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field

interface FieldParser {
    fun parse(input: String): Field
}

class CsvFieldParser : FieldParser {
    override fun parse(input: String): Field {
        val rows = input.split("\n")
        val cells = rows.mapIndexed { row, rowString ->
            rowString.split(",").mapIndexed { column, cell ->
                Cell.new(CellPosition(row, column), cell.toIntOrNull())
            }
        }.flatten()
        return Field(cells)
    }
}

class PipeFieldParser : FieldParser {
    override fun parse(input: String): Field {
        val split = input.split("\n")
        val cells = split.mapIndexed { row, rowString ->
            rowString.split("|").mapIndexed { column, cell ->
                Cell.new(CellPosition(row, column), cell.tryParseCandidates())
            }
        }.flatten()
        return Field(cells)
    }

    private fun String.tryParseCandidates(): Set<Int> =
        filter { "0123456789,".indexOf(it) > -1 }
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .toSet()
}
