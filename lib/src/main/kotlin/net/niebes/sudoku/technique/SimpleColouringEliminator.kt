package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field

/**
 * Simple colouring: follow one value's strong links and two-colour what they connect. The links
 * alternate, so within a connected group exactly one colour is the truth.
 *
 * That gives two rules:
 *
 * - **Colour trap** - a cell outside the group seeing both colours cannot hold the value, since
 *   whichever colour wins, one of the cells it sees has it.
 * - **Colour wrap** - if two cells of the *same* colour share a house, that colour would put the
 *   value twice in one house, so it is false and every cell wearing it loses the value.
 *
 * A group whose links cannot be two-coloured is skipped: an odd cycle means the grid is already
 * contradictory, which is not this technique's to report.
 */
class SimpleColouringEliminator : EliminationTechnique {
    override val technique = Technique.SIMPLE_COLOURING

    override fun eliminations(field: Field): List<Elimination> = buildList {
        (1..SIZE).forEach { value ->
            val neighbours = HashMap<CellPosition, MutableSet<CellPosition>>()
            field.conjugatePairs(value).forEach { link ->
                neighbours.getOrPut(link.first.position) { mutableSetOf() } += link.second.position
                neighbours.getOrPut(link.second.position) { mutableSetOf() } += link.first.position
            }

            val seen = mutableSetOf<CellPosition>()
            neighbours.keys.forEach { start ->
                if (start in seen) return@forEach
                val colours = twoColour(start, neighbours) ?: return@forEach
                seen += colours.keys

                val sides = colours.keys.partition { colours.getValue(it) }
                addAll(wrap(field, value, sides))
                addAll(trap(field, value, sides, colours.keys))
            }
        }
    }

    /** Colours a connected group, or null if its links contain an odd cycle. */
    private fun twoColour(
        start: CellPosition,
        neighbours: Map<CellPosition, Set<CellPosition>>
    ): Map<CellPosition, Boolean>? {
        val colours = mutableMapOf(start to true)
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val here = queue.removeFirst()
            neighbours.getValue(here).forEach { next ->
                when (colours[next]) {
                    null -> {
                        colours[next] = !colours.getValue(here)
                        queue += next
                    }
                    colours.getValue(here) -> return null
                    else -> Unit
                }
            }
        }
        return colours
    }

    private fun wrap(
        field: Field,
        value: Int,
        sides: Pair<List<CellPosition>, List<CellPosition>>
    ): List<Elimination> = sides.toList()
        .filter { side -> side.any { a -> side.any { b -> field.sees(a, b) } } }
        .flatten()
        .map { Elimination(technique, it, Candidates.of(value)) }

    private fun trap(
        field: Field,
        value: Int,
        sides: Pair<List<CellPosition>, List<CellPosition>>,
        group: Set<CellPosition>
    ): List<Elimination> {
        val (a, b) = sides
        if (a.isEmpty() || b.isEmpty()) return emptyList()

        return field.unsolved()
            .filter { it.position !in group && it.couldBe(value) }
            .filter { cell -> a.any { field.sees(cell.position, it) } && b.any { field.sees(cell.position, it) } }
            .map { Elimination(technique, it.position, Candidates.of(value)) }
    }
}
