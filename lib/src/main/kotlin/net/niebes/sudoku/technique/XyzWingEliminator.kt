package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Field

/**
 * XYZ-wing: a pivot holding `{x,y,z}` and two pincers holding `{x,z}` and `{y,z}`, each seeing the
 * pivot.
 *
 * Between them the three cells must use up x, y and z, and each of them could be the z. So z is
 * somewhere in the three, and only a cell that sees **all three** can be ruled out - unlike an
 * XY-wing, where the pivot cannot be z and seeing the two pincers is enough.
 */
class XyzWingEliminator : EliminationTechnique {
    override val technique = Technique.XYZ_WING

    override fun eliminations(field: Field): List<Elimination> = buildList {
        val bivalue = field.unsolved().filter { it.candidates.size == 2 }

        field.unsolved().filter { it.candidates.size == 3 }.forEach { pivot ->
            // A pincer has to fit inside the pivot's candidates, or the three cells cannot share out
            // exactly those three values.
            val pincers = bivalue.filter {
                field.sees(pivot.position, it.position) && (it.candidates - pivot.candidates).isEmpty()
            }

            pincers.indices.forEach { i ->
                (i + 1 until pincers.size).forEach pair@{ j ->
                    val (a, b) = pincers[i] to pincers[j]
                    if ((a.candidates or b.candidates) != pivot.candidates) return@pair

                    val shared = a.candidates and b.candidates
                    if (shared.size != 1) return@pair
                    val z = shared.single()

                    val wing = listOf(pivot.position, a.position, b.position)
                    field.seenByBoth(a.position, b.position)
                        .filter { it.couldBe(z) && field.sees(it.position, pivot.position) }
                        .forEach { add(Elimination(technique, it.position, Candidates.of(z), wing)) }
                }
            }
        }
    }
}
