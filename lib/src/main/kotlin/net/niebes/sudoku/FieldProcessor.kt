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
        val cells = field.cells.associateByTo(LinkedHashMap()) { it.position }

        markHiddenSingles(cells) { it.position.row }
        markHiddenSingles(cells) { it.position.column }
        markHiddenSingles(cells) { it.position.segment }
        return Field(cells.values.toSet())
    }

    /**
     * A candidate that can go in only one cell of a house belongs there. Cell state is re-read from
     * [cells] on every lookup: narrowing one cell changes which candidates are still hidden singles,
     * so deciding against a stale snapshot lets two candidates claim the same cell.
     */
    private fun markHiddenSingles(cells: MutableMap<CellPosition, Cell>, house: (Cell) -> Any) {
        cells.values.groupBy(house).values.forEach { members ->
            val positions = members.map { it.position }
            (1..9).forEach candidate@{ candidate ->
                val current = positions.map { cells.getValue(it) }
                if (current.any { it is SolvedCell && it.value == candidate }) return@candidate

                val holders = current.filterIsInstance<UnsolvedCell>()
                    .filter { candidate in it.candidates.values }
                val only = holders.singleOrNull() ?: return@candidate
                if (only.candidates.values.size == 1) return@candidate

                println("mark only occurrence of $candidate in ${only.position}")
                cells[only.position] = UnsolvedCell(only.position, Candidates(setOf(candidate)))
            }
        }
    }
}


// remove solutions from candidates of row, column, segment
// remove candidates when only number in segment
// solve single candidate cell, fail single candidate
