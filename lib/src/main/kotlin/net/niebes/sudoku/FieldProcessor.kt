package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.UnsolvedCell

interface FieldProcessor {
    fun process(field: Field): Field
}

interface UnsolvedCellFieldProcessor : FieldProcessor {
    fun processUnsolved(field: Field, cell: UnsolvedCell): UnsolvedCell
    override fun process(field: Field): Field = field.cells.map { cell ->
        when (cell) {
            is SolvedCell -> cell
            is UnsolvedCell -> processUnsolved(field, cell)
        }
    }.let { Field(it.toSet()) }
}


class SolveSingleCandidateTransformer : FieldProcessor {
    override fun process(field: Field): Field = field.cells.map { cell ->
        when (cell) {
            is SolvedCell -> cell
            is UnsolvedCell -> when (cell.candidates.values.size) {
                0 -> throw IllegalStateException("sudoku unsolvable")
                1 -> {
                    println("solved ${cell.position} to ${cell.candidates.values.first()}")
                    SolvedCell(cell.position, cell.candidates.values.first())
                }
                else -> cell
            }
        }
    }.let { Field(it.toSet()) }
}

class RowCandidateEliminator : UnsolvedCellFieldProcessor {
    override fun processUnsolved(field: Field, cell: UnsolvedCell): UnsolvedCell {
        var processed = cell
        field.getRow(cell.position.row)
            .filterIsInstance(SolvedCell::class.java)
            .map {
                processed = processed.removeCandidate(it.value)
            }
        return processed
    }
}

class ColumnCandidateEliminator : UnsolvedCellFieldProcessor {
    override fun processUnsolved(field: Field, cell: UnsolvedCell): UnsolvedCell {
        var processed = cell

        field.getColumn(cell.position.column)
            .filterIsInstance(SolvedCell::class.java)
            .map {
                processed = processed.removeCandidate(it.value)
            }
        return processed
    }
}

class SegmentCandidateEliminator : UnsolvedCellFieldProcessor {
    override fun processUnsolved(field: Field, cell: UnsolvedCell): UnsolvedCell {
        var processed = cell
        field.getSegment(cell.position.segment)
            .filterIsInstance(SolvedCell::class.java)
            .map {
                processed = processed.removeCandidate(it.value)
            }
        return processed
    }
}

class SingleCandidateMarker : FieldProcessor {
    override fun process(field: Field): Field {
        val cellsMap = field.cells.associateBy { it.position }.toMutableMap()

        replace(cellsMap) { it.position.row }
        replace(cellsMap) { it.position.column }
        replace(cellsMap) { it.position.segment }
        return Field(cellsMap.values.toSet())
    }

    private fun replace(cellsMap: MutableMap<CellPosition, Cell>, function: (Cell) -> Any) {
        cellsMap.values.groupBy(function)
                .forEach { row ->
                    row.value
                        .filterIsInstance(UnsolvedCell::class.java)
                        .flatMap { it.candidates.values }
                        .groupingBy { it }
                        .eachCount().filter { it.value == 1 }
                        .keys
                        .forEach { candidate ->

                            row.value
                                .filterIsInstance(UnsolvedCell::class.java)
                                .first { it.candidates.values.contains(candidate) }.run {
                                    if (this.candidates.values.size == 1) return
                                    println("mark only occurency of $candidate in ${this.position}")
                                    cellsMap[this.position] = UnsolvedCell(this.position, Candidates(setOf(candidate)))
                                }
                        }
                }
    }

}


// remove solutions from candidates of row, column, segment
// remove candidates when only number in segment
// solve single candidate cell, fail single candidate
