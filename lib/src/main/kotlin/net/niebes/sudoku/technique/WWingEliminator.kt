package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.ConjugatePair
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.UnsolvedCell

/**
 * W-wing: two cells holding the same pair `{x,y}` that do not see each other, joined by a strong
 * link on x whose two ends see one of them each.
 *
 * One end of the link is x. Whichever it is, the cell it sees cannot be x and is therefore y - so
 * one of the pair is y, and nothing seeing both of them can be.
 *
 * The pair is what carries the deduction here, not a pivot: the link only has to reach them, and
 * its ends take no part in the elimination.
 */
class WWingEliminator : EliminationTechnique {
    override val technique = Technique.W_WING

    override fun eliminations(field: Field): List<Elimination> = buildList {
        val bivalue = field.unsolved().filter { it.candidates.size == 2 }

        bivalue.indices.forEach { i ->
            (i + 1 until bivalue.size).forEach pair@{ j ->
                val (a, b) = bivalue[i] to bivalue[j]
                if (a.candidates != b.candidates) return@pair
                // Cells that see each other are an ordinary naked pair, not this.
                if (field.sees(a.position, b.position)) return@pair

                a.candidates.values.forEach { linked ->
                    val forced = (a.candidates - linked).single()
                    if (field.conjugatePairs(linked).any { it.joins(field, a, b) }) {
                        field.seenByBoth(a.position, b.position)
                            .filter { it.couldBe(forced) }
                            .forEach { add(Elimination(technique, it.position, Candidates.of(forced))) }
                    }
                }
            }
        }
    }

    /**
     * True when the link reaches one of the two cells with each end. An end that *is* one of the
     * cells fails on its own account, since a cell does not see itself.
     */
    private fun ConjugatePair.joins(field: Field, a: UnsolvedCell, b: UnsolvedCell): Boolean =
        (field.sees(first.position, a.position) && field.sees(second.position, b.position)) ||
            (field.sees(second.position, a.position) && field.sees(first.position, b.position))
}
