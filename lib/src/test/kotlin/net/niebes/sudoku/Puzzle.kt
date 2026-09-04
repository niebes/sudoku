package net.niebes.sudoku

import net.niebes.sudoku.io.CompactFieldParser
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell

internal data class Puzzle(val givens: String, val solution: String) {
    fun field(): Field = CompactFieldParser().parse(givens)
    fun solved(): Field = CompactFieldParser().parse(solution)

    /** The value this cell takes in the one true solution. */
    fun valueAt(index: Int): Int = (solved().cells[index] as SolvedCell).value
}
