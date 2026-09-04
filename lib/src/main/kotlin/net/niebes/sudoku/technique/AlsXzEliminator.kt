package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.UnsolvedCell

/**
 * ALS-XZ. An *almost locked set* is n cells of one house holding n + 1 values: one value short of
 * being locked, so taking any single value away from it forces the rest to fill it exactly.
 *
 * Take two of them that share no cells and share two values, X and Z, where X is **restricted** -
 * every cell of one set that could be X sees every cell of the other that could be X, so X can be
 * spent in at most one of them. Whichever set goes without X is locked, and a locked set uses every
 * value it holds, so that set contains Z. Either way **Z is inside one of the two**, and any cell
 * seeing every place Z could go in both of them cannot be Z.
 *
 * **Not in the default chain.** It fires - on four of the sixty-six corpus puzzles at [maxSize] 3 -
 * but it changes nothing: the same five puzzles need search and the same seven guesses are made
 * with it and without it. Every elimination it contributes is one the chain reaches anyway, and
 * alternating inference chains bounded at nine links appear to be why. It is kept because it is
 * sound, tested and the strongest thing here that is not a chain, so a corpus of harder puzzles
 * may yet give it something to do.
 *
 * [maxSize] caps how many cells a set may have. The cost is quadratic in the number of sets found,
 * and sets of three cells multiply them several times over for eliminations the rest of the chain
 * already reaches.
 */
class AlsXzEliminator(private val maxSize: Int = 2) : EliminationTechnique {
    override val technique = Technique.ALS_XZ

    private class AlmostLockedSet(val cells: List<UnsolvedCell>, val values: Candidates) {
        val positions: Set<CellPosition> = cells.mapTo(HashSet(cells.size)) { it.position }
        fun placesFor(value: Int): List<UnsolvedCell> = cells.filter { it.couldBe(value) }
    }

    override fun eliminations(field: Field): List<Elimination> {
        val sets = almostLockedSets(field)
        // The same elimination is commonly reached by several pairs of sets - one set paired with
        // another, and again with a larger set built around it - so conclusions are collected
        // uniquely and keep the first pair found as their evidence.
        val found = LinkedHashMap<Pair<CellPosition, Candidates>, Elimination>()

        sets.indices.forEach { i ->
            (i + 1 until sets.size).forEach pair@{ j ->
                val (a, b) = sets[i] to sets[j]

                // Cheapest test first: without two values in common there is no X and no Z.
                val shared = a.values and b.values
                if (shared.size < 2) return@pair
                if (a.positions.any { it in b.positions }) return@pair

                shared.values.forEach { x ->
                    if (!restricted(field, a, b, x)) return@forEach
                    shared.values.filter { it != x }.forEach { z ->
                        clear(field, a, b, z).forEach { found.putIfAbsent(it.at to it.values, it) }
                    }
                }
            }
        }
        return found.values.toList()
    }

    /** True when [value] can be spent in at most one of the two sets. */
    private fun restricted(field: Field, a: AlmostLockedSet, b: AlmostLockedSet, value: Int): Boolean {
        val here = a.placesFor(value)
        val there = b.placesFor(value)
        return here.isNotEmpty() && there.isNotEmpty() &&
            here.all { one -> there.all { field.sees(one.position, it.position) } }
    }

    private fun clear(field: Field, a: AlmostLockedSet, b: AlmostLockedSet, z: Int): List<Elimination> {
        val places = a.placesFor(z) + b.placesFor(z)
        if (places.isEmpty()) return emptyList()

        val members = a.cells.map { it.position } + b.cells.map { it.position }
        return field.unsolved()
            .filter { cell ->
                cell.couldBe(z) && places.all { field.sees(cell.position, it.position) }
            }
            .map { Elimination(technique, it.position, Candidates.of(z), members) }
    }

    private fun almostLockedSets(field: Field): List<AlmostLockedSet> = buildList {
        field.houses().forEach { house ->
            val open = house.unsolved()
            (1..maxSize).forEach { size ->
                if (open.size <= size) return@forEach
                open.combinations(size).forEach { cells ->
                    val values = cells.fold(Candidates.NONE) { all, cell -> all or cell.candidates }
                    if (values.size == size + 1) add(AlmostLockedSet(cells, values))
                }
            }
        }
    }
}
