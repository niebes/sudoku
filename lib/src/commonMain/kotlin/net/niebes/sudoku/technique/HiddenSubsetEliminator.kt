package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field

/**
 * Hidden subsets: [sizes] values of a house that fit in exactly that many cells. Those cells must
 * hold those values between them, so every other candidate in them goes. The dual of a naked
 * subset - in a house with n unsolved cells, a naked k-subset is a hidden (n-k)-subset - which is
 * why running both to size four reaches well past four.
 */
class HiddenSubsetEliminator(private val sizes: IntRange = 2..4) : EliminationTechnique {
    override val technique = Technique.HIDDEN_SUBSET

    override fun eliminations(field: Field): List<Elimination> = buildList {
        field.houses().forEach { house ->
            // A value already placed here is settled, and counting the cells that still list it as a
            // candidate would invent a subset out of candidates peer elimination has not caught up
            // with - claiming cells for a value that cannot go in any of them.
            val places = (1..SIZE)
                .filterNot { house.holds(it) }
                .associateWith { house.candidatesFor(it) }
                .filterValues { it.isNotEmpty() }

            sizes.forEach size@{ size ->
                // A value with more homes than the subset is wide cannot be confined to it.
                val confined = places.filterValues { it.size <= size }.keys.toList()
                if (confined.size < size) return@size

                confined.combinations(size).forEach subset@{ values ->
                    val cells = values.flatMap { places.getValue(it) }.distinctBy { it.position }
                    if (cells.size != size) return@subset

                    val keep = Candidates.of(values)
                    val members = cells.map { it.position }
                    cells.forEach { cell ->
                        val extra = cell.candidates - keep
                        if (!extra.isEmpty()) add(Elimination(technique, cell.position, extra, members))
                    }
                }
            }
        }
    }
}
