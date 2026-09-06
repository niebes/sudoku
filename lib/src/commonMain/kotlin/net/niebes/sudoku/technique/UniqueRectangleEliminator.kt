package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.UnsolvedCell

/**
 * Unique rectangle, type 1: four cells across two rows, two columns and exactly two segments, three
 * of them holding the identical pair.
 *
 * Were the fourth to hold only that pair as well, the two values could be swapped diagonally and
 * every solution would come with a twin. A puzzle with one solution therefore cannot have that
 * shape, so the fourth corner holds neither of them.
 *
 * **This is the only technique here that reasons from the puzzle rather than from the grid.** It
 * assumes the input has exactly one solution. That assumption survives search - assigning a value
 * can only reduce how many solutions remain, never raise it - but it does not survive a caller
 * handing the solver an improper puzzle, where this may eliminate a value one of the several
 * solutions needed. The parsers reject inconsistent givens; they do not check uniqueness.
 */
class UniqueRectangleEliminator : EliminationTechnique {
    override val technique = Technique.UNIQUE_RECTANGLE

    override fun eliminations(field: Field): List<Elimination> = buildList {
        (0 until SIZE).forEach { topRow ->
            (topRow + 1 until SIZE).forEach { bottomRow ->
                (0 until SIZE).forEach { leftColumn ->
                    (leftColumn + 1 until SIZE).forEach { rightColumn ->
                        rectangle(field, topRow, bottomRow, leftColumn, rightColumn)?.let { add(it) }
                    }
                }
            }
        }
    }

    private fun rectangle(
        field: Field,
        topRow: Int,
        bottomRow: Int,
        leftColumn: Int,
        rightColumn: Int
    ): Elimination? {
        val corners = listOf(
            CellPosition(topRow, leftColumn), CellPosition(topRow, rightColumn),
            CellPosition(bottomRow, leftColumn), CellPosition(bottomRow, rightColumn)
        )
        // Spanning more than two segments breaks the swap: the pair would have to move between four
        // segments at once, which no longer keeps every house valid.
        if (corners.mapTo(mutableSetOf()) { it.segment }.size != 2) return null

        val cells = corners.map { field.cellAt(it) as? UnsolvedCell ?: return null }
        val pair = cells.map { it.candidates }.groupingBy { it }.eachCount()
            .filterValues { it == 3 }.keys.singleOrNull() ?: return null
        if (pair.size != 2) return null

        val odd = cells.single { it.candidates != pair }
        // The odd corner has to contain the pair and something more. Holding the pair exactly is the
        // deadly pattern itself - an improper puzzle, with nothing here to remove.
        if (!(pair - odd.candidates).isEmpty() || odd.candidates == pair) return null

        val bare = cells.filter { it.candidates == pair }.map { it.position }
        return Elimination(technique, odd.position, pair, bare)
    }
}
