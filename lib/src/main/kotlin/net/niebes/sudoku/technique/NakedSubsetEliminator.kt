package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Field

/**
 * Naked subsets: [sizes] cells of a house whose candidates together come to exactly that many
 * values. Those values are spoken for, so no other cell of the house can take any of them. A naked
 * pair is the size-two case; no cell need hold every value of the subset.
 *
 * Eliminations go to every house all the subset's cells share, not only the one being scanned - a
 * pair sitting in both a row and a segment clears both, which is what is otherwise called a locked
 * pair.
 */
class NakedSubsetEliminator(private val sizes: IntRange = 2..4) : EliminationTechnique {
    override val technique = Technique.NAKED_SUBSET

    override fun eliminations(field: Field): List<Elimination> = buildList {
        field.houses().forEach { house ->
            sizes.forEach size@{ size ->
                val unsolved = house.unsolved()
                // A subset as wide as the house's remaining cells leaves nothing to eliminate from.
                if (unsolved.size <= size) return@size
                // A cell with more candidates than the subset is wide cannot be part of it.
                val open = unsolved.filter { it.candidates.size in 1..size }

                open.combinations(size).forEach subset@{ subset ->
                    val values = subset.fold(Candidates.NONE) { union, cell -> union or cell.candidates }
                    if (values.size != size) return@subset

                    val members = subset.map { it.position }
                    val taken = members.toHashSet()
                    field.housesOf(subset.first().position)
                        .filter { shared -> shared.cells.mapTo(HashSet()) { it.position }.containsAll(taken) }
                        .forEach { shared ->
                            shared.unsolved()
                                .filter { it.position !in taken }
                                .forEach { add(Elimination(technique, it.position, values, members)) }
                        }
                }
            }
        }
    }
}
