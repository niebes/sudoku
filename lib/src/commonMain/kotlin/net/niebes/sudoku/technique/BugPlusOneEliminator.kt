package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.UnsolvedCell

/**
 * BUG+1, from *bivalue universal grave*: a grid where every unsolved cell is down to a pair and
 * every value sits in exactly two cells of every house it still appears in.
 *
 * Such a grid has an even number of solutions - the two occurrences of each value can always be
 * swapped - so a puzzle with exactly one can never reach it. If one cell has a third candidate and
 * taking that candidate away would leave a grave, then the cell not holding it is impossible: **it
 * holds it**, and the other two go.
 *
 * Each of the three candidates is tried in turn rather than counted, because "the value that
 * appears three times" is only true when the value already had two homes in the house; a value
 * whose only home is this cell leaves a grave when removed just the same.
 *
 * Assumes a proper puzzle, for the reasons set out on [UniqueRectangleEliminator].
 */
class BugPlusOneEliminator : EliminationTechnique {
    override val technique = Technique.BUG_PLUS_ONE

    override fun eliminations(field: Field): List<Elimination> {
        val odd = field.unsolved().filter { it.candidates.size != 2 }
        val extra = odd.singleOrNull() ?: return emptyList()
        if (extra.candidates.size != 3) return emptyList()

        val answer = extra.candidates.values.singleOrNull { leavesAGrave(field, extra, it) }
            ?: return emptyList()

        return listOf(Elimination(technique, extra.position, extra.candidates - answer))
    }

    /** Would the grid be a grave with [candidate] taken away from [extra]? */
    private fun leavesAGrave(field: Field, extra: UnsolvedCell, candidate: Int): Boolean =
        field.houses().all { house ->
            (1..SIZE).all { value ->
                val homes = house.unsolved().count { cell ->
                    val candidates =
                        if (cell.position == extra.position) cell.candidates - candidate else cell.candidates
                    candidates.contains(value)
                }
                homes == 0 || homes == 2
            }
        }
}
