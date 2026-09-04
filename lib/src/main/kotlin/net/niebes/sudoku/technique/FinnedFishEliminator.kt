package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.House
import net.niebes.sudoku.model.HouseKind

/**
 * Finned and sashimi fish: a fish that would be valid but for extra candidates - the **fins** - in
 * one of its base lines.
 *
 * The argument splits. If no fin holds the value, the fish is real and clears its cover lines as
 * usual. If a fin does hold it, nothing seeing that fin can. So the eliminations that survive are
 * the fish's own, kept only where they also see **every** fin - which is why a fish with fins
 * scattered across segments yields nothing.
 *
 * The cover lines come from the base lines that are *not* finned: those must already span exactly
 * as many lines as the fish is wide, and whatever the finned line has outside them are the fins.
 * That is the standard shape, and it keeps the search small enough to sit in the chain.
 */
class FinnedFishEliminator(private val sizes: IntRange = 2..3) : EliminationTechnique {
    override val technique = Technique.FINNED_FISH

    override fun eliminations(field: Field): List<Elimination> = buildList {
        ORIENTATIONS.forEach { (baseKind, coverKind) ->
            val bases = field.houses().filter { it.kind == baseKind }
            val covers = field.houses().filter { it.kind == coverKind }.associateBy { it.index }

            (1..SIZE).forEach { value ->
                val reachable = bases
                    .filterNot { it.holds(value) }
                    .mapNotNull { base ->
                        val places = base.candidatesFor(value)
                        if (places.size < 2) null else base to places
                    }

                sizes.forEach size@{ size ->
                    if (reachable.size < size) return@size
                    reachable.combinations(size).forEach { chosen ->
                        chosen.indices.forEach { finned ->
                            addAll(fish(field, value, chosen, finned, covers, coverKind, baseKind))
                        }
                    }
                }
            }
        }
    }

    private fun fish(
        field: Field,
        value: Int,
        chosen: List<Pair<House, List<Cell>>>,
        finned: Int,
        covers: Map<Int, House>,
        coverKind: HouseKind,
        baseKind: HouseKind
    ): List<Elimination> {
        val coverLines = chosen.filterIndexed { index, _ -> index != finned }
            .flatMapTo(mutableSetOf()) { (_, places) -> places.map { it.position.lineIndex(coverKind) } }
        // The unfinned lines have to define the fish exactly; anything wider is not one.
        if (coverLines.size != chosen.size) return emptyList()
        if (coverLines.any { covers.getValue(it).holds(value) }) return emptyList()

        val (_, finnedPlaces) = chosen[finned]
        val fins = finnedPlaces.filterNot { it.position.lineIndex(coverKind) in coverLines }
        // No fins is a plain fish, which basic fish has already read.
        if (fins.isEmpty() || fins.size == finnedPlaces.size) return emptyList()

        val baseLines = chosen.mapTo(mutableSetOf()) { (base, _) -> base.index }
        return coverLines.flatMap { coverLine ->
            covers.getValue(coverLine).cells.filter { cell ->
                cell.couldBe(value) &&
                    cell.position.lineIndex(baseKind) !in baseLines &&
                    fins.all { field.sees(cell.position, it.position) }
            }
        }.map { Elimination(technique, it.position, Candidates.of(value)) }
    }

    private fun CellPosition.lineIndex(kind: HouseKind): Int =
        if (kind == HouseKind.ROW) row else column

    private companion object {
        val ORIENTATIONS = listOf(
            HouseKind.ROW to HouseKind.COLUMN,
            HouseKind.COLUMN to HouseKind.ROW
        )
    }
}
