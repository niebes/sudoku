package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Field

/**
 * XY-wing: a pivot holding `{x,y}` and two pincers holding `{x,z}` and `{y,z}`, each seeing the
 * pivot.
 *
 * The pivot takes x or y. If it takes x the first pincer cannot, so that pincer is z; if it takes y
 * the second pincer is z. One of the two pincers holds z either way, so nothing seeing both of them
 * can.
 *
 * Note what is not required: the pincers need not see each other, and the pivot need not share a
 * house with the cells that end up losing z.
 */
class XyWingEliminator : EliminationTechnique {
    override val technique = Technique.XY_WING

    override fun eliminations(field: Field): List<Elimination> = buildList {
        val bivalue = field.unsolved().filter { it.candidates.size == 2 }

        bivalue.forEach { pivot ->
            val x = pivot.candidates.values.first()
            val y = pivot.candidates.values.last()
            val reachable = bivalue.filter { field.sees(pivot.position, it.position) }

            reachable.forEach pincer@{ pincer ->
                // The pincer must share exactly one value with the pivot, so that the pivot taking
                // that value forces the pincer onto its other one.
                if (!pincer.candidates.contains(x) || pincer.candidates == pivot.candidates) return@pincer
                val z = (pincer.candidates - x).single()

                reachable.filter { it.candidates == Candidates.of(y, z) }.forEach { opposite ->
                    field.seenByBoth(pincer.position, opposite.position)
                        .filter { it.couldBe(z) }
                        .forEach { add(Elimination(technique, it.position, Candidates.of(z))) }
                }
            }
        }
    }
}
