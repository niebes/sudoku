package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.ConjugatePair
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.UnsolvedCell

/**
 * Turbot fish: two strong links on one value, joined by a weak link between one end of each.
 *
 * Take strong links a1-a2 and b1-b2, where a2 and b1 see each other. If a1 is not the value then a2
 * is, so b1 is not, so b2 is. Either way **a1 or b2 holds the value**, and any cell seeing both of
 * them cannot.
 *
 * Skyscraper and 2-string kite are this pattern with the links in particular places - two rows
 * sharing a column, or a row and a column meeting in a segment - so both fall out of the general
 * form rather than needing techniques of their own.
 */
class TurbotFishEliminator : EliminationTechnique {
    override val technique = Technique.TURBOT_FISH

    override fun eliminations(field: Field): List<Elimination> = buildList {
        (1..SIZE).forEach { value ->
            val links = field.conjugatePairs(value)

            // Unordered pairs: trying all four end combinations of (a, b) already covers
            // everything (b, a) would, and taking both would report every elimination twice.
            links.indices.forEach { i ->
                (i + 1 until links.size).forEach { j ->
                    val (a, b) = links[i] to links[j]

                    a.ends.forEach { joinedA ->
                        b.ends.forEach join@{ joinedB ->
                            val farA = a.other(joinedA)
                            val farB = b.other(joinedB)
                            // The four cells must be distinct, or the "pattern" is one strong link
                            // read twice and proves nothing new.
                            val corners = setOf(joinedA.position, joinedB.position, farA.position, farB.position)
                            if (corners.size != 4) return@join
                            if (!field.sees(joinedA.position, joinedB.position)) return@join

                            addAll(clear(field, value, farA, joinedA, joinedB, farB))
                        }
                    }
                }
            }
        }
    }

    /** One of [farA] and [farB] holds the value, so nothing seeing both of them can. */
    private fun clear(
        field: Field,
        value: Int,
        farA: UnsolvedCell,
        joinedA: UnsolvedCell,
        joinedB: UnsolvedCell,
        farB: UnsolvedCell
    ): List<Elimination> {
        // The chain read end to end, which is how the alternative-by-alternative argument walks it.
        val chain = listOf(farA.position, joinedA.position, joinedB.position, farB.position)
        return field.seenByBoth(farA.position, farB.position)
            .filter { it.couldBe(value) }
            .map { Elimination(technique, it.position, Candidates.of(value), chain) }
    }
}
