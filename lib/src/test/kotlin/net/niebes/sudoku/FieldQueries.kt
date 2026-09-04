package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.UnsolvedCell

internal fun Field.candidatesAt(row: Int, column: Int): Candidates =
    when (val cell = cellAt(CellPosition(row, column))) {
        is UnsolvedCell -> cell.candidates
        is SolvedCell -> Candidates.of(cell.value)
    }

internal fun Field.valueAt(row: Int, column: Int): Int? =
    (cellAt(CellPosition(row, column)) as? SolvedCell)?.value
