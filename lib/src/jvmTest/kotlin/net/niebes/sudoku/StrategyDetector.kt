package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.HouseKind
import net.niebes.sudoku.model.UnsolvedCell

/**
 * Looks for patterns belonging to techniques that are **not** implemented, and reports how many
 * eliminations each would make on a given grid.
 *
 * The point is to decide what to build next from evidence rather than from a list. A technique that
 * finds nothing on the grids where the solver is actually stuck is not worth writing, however well
 * regarded it is - and this session has produced three of those already.
 *
 * It answers "would this fire here", not "would this unblock the puzzle". The two are different and
 * confusing them is how ALS-XZ came to be recommended.
 */
internal object StrategyDetector {

    data class Finding(val strategy: String, val eliminations: Int)

    fun detect(field: Field): List<Finding> = listOf(
        Finding("grouped chains", groupedTurbotFish(field)),
        Finding("ALS chain (3 sets)", alsChain(field)),
        Finding("death blossom", deathBlossom(field)),
        Finding("franken fish", frankenFish(field)),
        Finding("Sue de Coq", sueDeCoq(field))
    )

    // ---- shared pieces -------------------------------------------------------------------------

    private class Als(val cells: List<UnsolvedCell>, val values: Candidates) {
        val positions = cells.mapTo(HashSet()) { it.position }
        fun placesFor(value: Int) = cells.filter { it.couldBe(value) }
    }

    private fun almostLockedSets(field: Field, maxSize: Int = 3): List<Als> = buildList {
        field.houses().forEach { house ->
            val open = house.unsolved()
            (1..maxSize).forEach { size ->
                if (open.size > size) combinations(open, size).forEach { cells ->
                    val values = cells.fold(Candidates.NONE) { all, cell -> all or cell.candidates }
                    if (values.size == size + 1) add(Als(cells, values))
                }
            }
        }
    }

    private fun <T> combinations(items: List<T>, size: Int): List<List<T>> =
        if (size == 0) listOf(emptyList())
        else items.indices.flatMap { i ->
            combinations(items.subList(i + 1, items.size), size - 1).map { listOf(items[i]) + it }
        }

    /** [value] can be spent in at most one of the two sets. */
    private fun restricted(field: Field, a: Als, b: Als, value: Int): Boolean {
        val here = a.placesFor(value)
        val there = b.placesFor(value)
        return here.isNotEmpty() && there.isNotEmpty() &&
            here.all { one -> there.all { field.sees(one.position, it.position) } }
    }

    private fun seeingAll(field: Field, places: List<Cell>, value: Int): List<CellPosition> =
        field.unsolved()
            .filter { cell -> cell.couldBe(value) && places.all { field.sees(cell.position, it.position) } }
            .map { it.position }

    // ---- grouped chains ------------------------------------------------------------------------

    /** A set of cells that all share some house, so a chain can treat them as one node. */
    private fun isGroup(field: Field, cells: List<Cell>): Boolean =
        cells.size == 1 || field.houses().any { house ->
            cells.all { cell -> house.cells.any { it.position == cell.position } }
        }

    /** Strong links for [value], allowing either end to be a group rather than a single cell. */
    private fun strongLinks(field: Field, value: Int): List<Pair<List<Cell>, List<Cell>>> = buildList {
        field.houses().forEach { house ->
            val places = house.candidatesFor(value)
            if (places.size !in 2..4) return@forEach
            // Every way of splitting the value's homes into two sides; each side must be a group.
            (1 until (1 shl places.size) - 1).forEach { mask ->
                val left = places.filterIndexed { i, _ -> mask shr i and 1 == 1 }
                val right = places.filterIndexed { i, _ -> mask shr i and 1 == 0 }
                if (left.size <= right.size && isGroup(field, left) && isGroup(field, right)) {
                    add(left to right)
                }
            }
        }
    }

    private fun sees(field: Field, one: List<Cell>, other: List<Cell>): Boolean =
        one.all { a -> other.all { field.sees(a.position, it.position) } }

    /** Turbot fish where the ends may be groups - the cheapest thing grouped chains buy. */
    private fun groupedTurbotFish(field: Field): Int {
        val found = mutableSetOf<Pair<CellPosition, Int>>()
        (1..SIZE).forEach { value ->
            val links = strongLinks(field, value)
            links.indices.forEach { i ->
                (i + 1 until links.size).forEach { j ->
                    listOf(
                        links[i].first to links[i].second,
                        links[i].second to links[i].first
                    ).forEach { (joinA, farA) ->
                        listOf(
                            links[j].first to links[j].second,
                            links[j].second to links[j].first
                        ).forEach inner@{ (joinB, farB) ->
                            val touched = (joinA + farA + joinB + farB).map { it.position }.toSet()
                            if (touched.size != joinA.size + farA.size + joinB.size + farB.size) return@inner
                            if (!sees(field, joinA, joinB)) return@inner
                            seeingAll(field, farA + farB, value).forEach { found += it to value }
                        }
                    }
                }
            }
        }
        return found.size
    }

    // ---- ALS chain of three --------------------------------------------------------------------

    private fun alsChain(field: Field): Int {
        val sets = almostLockedSets(field)
        val found = mutableSetOf<Pair<CellPosition, Int>>()
        sets.forEach { a ->
            sets.forEach { b ->
                if (a === b || a.positions.any { it in b.positions }) return@forEach
                val ab = (a.values and b.values).values.filter { restricted(field, a, b, it) }
                if (ab.isEmpty()) return@forEach

                sets.forEach c@{ c ->
                    if (c === a || c === b) return@c
                    if (c.positions.any { it in a.positions || it in b.positions }) return@c
                    val bc = (b.values and c.values).values.filter { restricted(field, b, c, it) }
                    if (bc.none { it !in ab }) return@c

                    (a.values and c.values).values.filter { it !in ab && it !in bc }.forEach { z ->
                        seeingAll(field, a.placesFor(z) + c.placesFor(z), z).forEach { found += it to z }
                    }
                }
            }
        }
        return found.size
    }

    // ---- death blossom -------------------------------------------------------------------------

    private fun deathBlossom(field: Field): Int {
        val sets = almostLockedSets(field)
        val found = mutableSetOf<Pair<CellPosition, Int>>()

        field.unsolved().filter { it.candidates.size in 2..3 }.forEach { stem ->
            val petals = stem.candidates.values.map { value ->
                sets.filter { als ->
                    stem.position !in als.positions && als.values.contains(value) &&
                        als.placesFor(value).all { field.sees(stem.position, it.position) }
                }
            }
            if (petals.any { it.isEmpty() }) return@forEach

            petals.first().forEach { first ->
                val chosen = mutableListOf(first)
                petals.drop(1).forEach { options ->
                    options.firstOrNull { it.positions.none { p -> chosen.any { c -> p in c.positions } } }
                        ?.let { chosen += it }
                }
                if (chosen.size != petals.size) return@forEach

                val shared = chosen.fold(Candidates.ALL) { all, als -> all and als.values }
                shared.values.filter { !stem.couldBe(it) }.forEach { z ->
                    seeingAll(field, chosen.flatMap { it.placesFor(z) }, z).forEach { found += it to z }
                }
            }
        }
        return found.size
    }

    // ---- franken fish --------------------------------------------------------------------------

    /** A fish whose base or cover sets may include a segment, not only lines. */
    private fun frankenFish(field: Field): Int {
        val found = mutableSetOf<Pair<CellPosition, Int>>()
        val houses = field.houses()

        (1..SIZE).forEach { value ->
            // Base sets need few homes for the value; cover sets are only there to cover, so
            // filtering them by how many candidates they hold leaves nothing able to do the job.
            val bases = houses.map { it to it.candidatesFor(value) }.filter { it.second.size in 2..4 }
            val covers = houses.map { it to it.candidatesFor(value) }.filter { it.second.isNotEmpty() }

            combinations(bases, 2).forEach base@{ base ->
                val basePlaces = base.flatMap { (_, places) -> places.map { it.position } }
                if (basePlaces.toSet().size != basePlaces.size) return@base

                combinations(covers, 2).forEach cover@{ cover ->
                    if (cover.any { (house, _) -> base.any { it.first === house } }) return@cover
                    val covered = cover.flatMap { (_, places) -> places.map { it.position } }
                    if (covered.toSet().size != covered.size) return@cover
                    // Only interesting when a segment takes part; otherwise it is a basic fish.
                    if ((base + cover).none { (house, _) -> house.kind == HouseKind.SEGMENT }) return@cover
                    if (!basePlaces.all { it in covered }) return@cover

                    covered.filterNot { it in basePlaces.toSet() }.forEach { found += it to value }
                }
            }
        }
        return found.size
    }

    // ---- Sue de Coq ----------------------------------------------------------------------------

    /**
     * A segment-line overlap holding more values than it has cells, completed by cells from the
     * line and from the segment so that the whole set is locked: as many cells as values.
     *
     * A value only the line's cells can take is then confined to the line, one only the segment's
     * cells can take is confined to the segment, and one neither can take must sit in the overlap
     * and so leaves both.
     */
    private fun sueDeCoq(field: Field): Int {
        val found = mutableSetOf<Pair<CellPosition, Int>>()

        field.intersections().forEach { intersection ->
            val open = intersection.cells.filterIsInstance<UnsolvedCell>()
            val lineOnly = intersection.lineOnly().filterIsInstance<UnsolvedCell>()
            val segmentOnly = intersection.segmentOnly().filterIsInstance<UnsolvedCell>()

            (2..open.size).forEach { size ->
                combinations(open, size).forEach core@{ core ->
                    val values = core.fold(Candidates.NONE) { all, cell -> all or cell.candidates }
                    val extra = values.size - size
                    // Fewer than two values spare and there is nothing for outside cells to take.
                    if (extra !in 2..3) return@core

                    val fromLine = lineOnly.filter { (it.candidates - values).isEmpty() }
                    val fromSegment = segmentOnly.filter { (it.candidates - values).isEmpty() }

                    (0..extra).forEach { take ->
                        combinations(fromLine, take).forEach { line ->
                            combinations(fromSegment, extra - take).forEach pick@{ segment ->
                                val lineValues = line.fold(Candidates.NONE) { all, c -> all or c.candidates }
                                val segmentValues = segment.fold(Candidates.NONE) { all, c -> all or c.candidates }
                                // The two wings must not compete for the same value, or the set is
                                // not locked and none of this follows.
                                if (!(lineValues and segmentValues).isEmpty()) return@pick

                                val used = core.map { it.position }.toSet() +
                                    line.map { it.position } + segment.map { it.position }

                                values.values.forEach { value ->
                                    val inLine = lineValues.contains(value)
                                    val inSegment = segmentValues.contains(value)
                                    if (inLine || !inSegment) {
                                        intersection.line.unsolved()
                                            .filter { it.position !in used && it.couldBe(value) }
                                            .forEach { found += it.position to value }
                                    }
                                    if (inSegment || !inLine) {
                                        intersection.segment.unsolved()
                                            .filter { it.position !in used && it.couldBe(value) }
                                            .forEach { found += it.position to value }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return found.size
    }
}
