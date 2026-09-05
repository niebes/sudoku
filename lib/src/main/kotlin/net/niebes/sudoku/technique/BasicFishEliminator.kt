package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.House
import net.niebes.sudoku.model.HouseKind

/**
 * Basic fish: [sizes] lines in which a value is confined to the same number of crossing lines.
 *
 * Each base line needs the value somewhere, and every place it could go lies in one of the cover
 * lines. That is n values to distribute over n cover lines, so each cover line takes exactly one -
 * and no cell of a cover line outside the base can hold it. X-Wing is the size-two case, then
 * swordfish and jellyfish; a base line needs only two places, not the full width.
 *
 * Unlike subsets, this is the first technique that cares which kind of house it is looking at:
 * base and cover lines must be of opposite orientation, which is why houses carry their kind.
 */
class BasicFishEliminator(private val sizes: IntRange = 2..4) : EliminationTechnique {
    override val technique = Technique.BASIC_FISH

    override fun eliminations(field: Field): List<Elimination> = buildList {
        ORIENTATIONS.forEach { (baseKind, coverKind) ->
            val bases = field.houses().filter { it.kind == baseKind }
            val covers = field.houses().filter { it.kind == coverKind }.associateBy { it.index }

            (1..SIZE).forEach { value ->
                addAll(fish(value, bases, covers, coverKind, baseKind))
            }
        }
    }

    private fun fish(
        value: Int,
        bases: List<House>,
        covers: Map<Int, House>,
        coverKind: HouseKind,
        baseKind: HouseKind
    ): List<Elimination> = buildList {
        // A line that already holds the value needs no place for it, and reading its candidates
        // would build a fish out of a line whose value is settled.
        val reachable = bases
            .filterNot { it.holds(value) }
            .mapNotNull { base ->
                val places = base.candidatesFor(value).mapTo(mutableSetOf()) { it.position.lineIndex(coverKind) }
                if (places.size < 2) null else base to places
            }

        sizes.forEach size@{ size ->
            // A base line with more places than the fish is wide cannot belong to it.
            val usable = reachable.filter { it.second.size <= size }
            if (usable.size < size) return@size

            usable.combinations(size).forEach shape@{ chosen ->
                val coverLines = chosen.flatMapTo(mutableSetOf()) { it.second }
                if (coverLines.size != size) return@shape
                // The same objection as above, from the other side: a cover line already holding the
                // value cannot also be taking one from the base lines.
                if (coverLines.any { covers.getValue(it).holds(value) }) return@shape

                val baseLines = chosen.mapTo(mutableSetOf()) { it.first.index }
                // The evidence is every place the base lines leave for the value - the corners of
                // the fish, which between them claim one copy per cover line.
                val corners = chosen.flatMap { (base, _) -> base.candidatesFor(value).map { it.position } }
                coverLines.forEach { coverLine ->
                    covers.getValue(coverLine).cells
                        .filter { it.couldBe(value) && it.position.lineIndex(baseKind) !in baseLines }
                        .forEach { add(Elimination(technique, it.position, Candidates.of(value), corners)) }
                }
            }
        }
    }

    private fun CellPosition.lineIndex(kind: HouseKind): Int =
        if (kind == HouseKind.ROW) row else column

    private companion object {
        /** Rows covered by columns, and the transpose. Segments never take part in a basic fish. */
        val ORIENTATIONS = listOf(
            HouseKind.ROW to HouseKind.COLUMN,
            HouseKind.COLUMN to HouseKind.ROW
        )
    }
}
