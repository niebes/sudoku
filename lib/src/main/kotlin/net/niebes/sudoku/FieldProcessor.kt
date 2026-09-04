package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.Intersection
import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.UnsolvedCell

interface FieldProcessor {
    fun process(field: Field, deductions: DeductionListener = DeductionListener.IGNORE): Field
}

/**
 * A technique that only removes candidates.
 *
 * [eliminations] reads the field it is handed and nothing else, and the whole batch is applied
 * afterwards. That is deliberate: a technique that eliminated as it went would go on to reason
 * about a grid its own earlier steps had already changed, and every family of technique above
 * singles is vulnerable to that - two candidates claiming one cell, or a digit looking confined to
 * a house because a cell that was just solved dropped out of the list being scanned.
 */
interface EliminationTechnique : FieldProcessor {
    val technique: Technique

    /** Must depend only on [field]. Overlapping or repeated eliminations are harmless. */
    fun eliminations(field: Field): List<Elimination>

    override fun process(field: Field, deductions: DeductionListener): Field {
        val found = eliminations(field)
        if (found.isEmpty()) return field

        val cells = field.cells.toMutableList()
        found.forEach { elimination ->
            val cell = cells[elimination.at.index]
            if (cell !is UnsolvedCell) return@forEach

            val removable = cell.candidates and elimination.values
            if (removable.isEmpty()) return@forEach

            deductions.onDeduction(Elimination(technique, elimination.at, removable))
            cells[elimination.at.index] = UnsolvedCell(cell.position, cell.candidates - removable)
        }
        return Field(cells)
    }
}

/**
 * Full house: a house with one unsolved cell left takes the value missing from it.
 *
 * Strictly a special case of a naked single, but it needs no candidates at all - only the eight
 * values already there - so it is the cheapest deduction available and the one a person makes first.
 */
class FullHouseSolver : FieldProcessor {
    override fun process(field: Field, deductions: DeductionListener): Field {
        // A cell can be the last gap in two houses at once, so placements are keyed by position.
        val placements = LinkedHashMap<CellPosition, Int>()

        field.houses().forEach { house ->
            val gap = house.unsolved().singleOrNull() ?: return@forEach

            var placed = Candidates.NONE
            house.cells.forEach { if (it is SolvedCell) placed += it.value }

            val missing = Candidates.ALL - placed
            // More than one value missing means the house repeats a value; the cell not being able
            // to take the missing one means the same thing. Either way this is not ours to report -
            // leaving the cell alone lets the contradiction surface as an exhausted cell.
            if (missing.size != 1 || !gap.candidates.contains(missing.single())) return@forEach

            placements.putIfAbsent(gap.position, missing.single())
        }
        if (placements.isEmpty()) return field

        val cells = field.cells.toMutableList()
        placements.forEach { (position, value) ->
            deductions.onDeduction(Placement(Technique.FULL_HOUSE, position, value))
            cells[position.index] = SolvedCell(position, value)
        }
        return Field(cells)
    }
}

/**
 * A value already placed in a cell's row, column or segment cannot be a candidate for that cell.
 * Row, column and segment are the same constraint - a house - so this is one technique, not three.
 */
class HouseCandidateEliminator : EliminationTechnique {
    override val technique = Technique.PEER_ELIMINATION

    override fun eliminations(field: Field): List<Elimination> = field.unsolved().mapNotNull { cell ->
        val placed = field.solvedPeers(cell.position) and cell.candidates
        if (placed.isEmpty()) null else Elimination(technique, cell.position, placed)
    }
}

/**
 * Pointing, or locked candidates type 1: if within a segment every cell that could take a value
 * lies on one line, the segment's copy of that value is somewhere on that line - so the value
 * cannot appear on that line anywhere outside the segment.
 */
class PointingEliminator : EliminationTechnique {
    override val technique = Technique.POINTING

    override fun eliminations(field: Field): List<Elimination> =
        lockedCandidates(field, technique, Intersection::segmentOnly, Intersection::lineOnly)
}

/**
 * Claiming, or locked candidates type 2: if along a line every cell that could take a value lies
 * inside one segment, the line's copy of that value is somewhere in that segment - so the value
 * cannot appear in that segment anywhere off the line.
 */
class ClaimingEliminator : EliminationTechnique {
    override val technique = Technique.CLAIMING

    override fun eliminations(field: Field): List<Elimination> =
        lockedCandidates(field, technique, Intersection::lineOnly, Intersection::segmentOnly)
}

/**
 * The shape both directions of locked candidates share: a value that cannot appear on one side of
 * a segment-line overlap must lie in the overlap, and so leaves the other side. Pointing and
 * claiming differ only in which side is which.
 */
private fun lockedCandidates(
    field: Field,
    technique: Technique,
    confinedAwayFrom: (Intersection) -> List<Cell>,
    clear: (Intersection) -> List<Cell>
): List<Elimination> = buildList {
    field.intersections().forEach { intersection ->
        (1..SIZE).forEach value@{ value ->
            // A value already placed in either house makes the question moot, and answering it from
            // candidate state alone would be wrong: cells can still carry a candidate that peer
            // elimination has not caught up with, which would make the value look confined to the
            // overlap when it is in fact settled elsewhere.
            if (intersection.segment.holds(value) || intersection.line.holds(value)) return@value
            if (intersection.cells.none { it.couldBe(value) }) return@value
            if (confinedAwayFrom(intersection).any { it.couldBe(value) }) return@value

            clear(intersection)
                .filter { it.couldBe(value) }
                .forEach { add(Elimination(technique, it.position, Candidates.of(value))) }
        }
    }
}

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

                    val taken = subset.mapTo(HashSet(size)) { it.position }
                    field.housesOf(subset.first().position)
                        .filter { shared -> shared.cells.mapTo(HashSet()) { it.position }.containsAll(taken) }
                        .forEach { shared ->
                            shared.unsolved()
                                .filter { it.position !in taken }
                                .forEach { add(Elimination(technique, it.position, values)) }
                        }
                }
            }
        }
    }
}

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
                    cells.forEach { cell ->
                        val extra = cell.candidates - keep
                        if (!extra.isEmpty()) add(Elimination(technique, cell.position, extra))
                    }
                }
            }
        }
    }
}

/** Hidden singles: a candidate that fits in only one cell of a house belongs to that cell. */
class SingleCandidateMarker : FieldProcessor {
    override fun process(field: Field, deductions: DeductionListener): Field {
        val cells = field.cells.toMutableList()
        // Positions are fixed for the life of a field, so the houses can be taken once up front.
        field.houses().map { house -> house.cells.map { it.position } }.forEach { mark(cells, it, deductions) }
        return Field(cells)
    }

    /**
     * Cell state is re-read from [cells] on every lookup: narrowing one cell changes which candidates
     * are still hidden singles, so deciding against a stale snapshot lets two candidates claim the
     * same cell.
     */
    private fun mark(cells: MutableList<Cell>, house: List<CellPosition>, deductions: DeductionListener) {
        (1..9).forEach candidate@{ candidate ->
            val current = house.map { cells[it.index] }
            if (current.any { it is SolvedCell && it.value == candidate }) return@candidate

            val only = current.filterIsInstance<UnsolvedCell>()
                .filter { it.candidates.contains(candidate) }
                .singleOrNull() ?: return@candidate
            if (only.candidates.size == 1) return@candidate

            deductions.onDeduction(Placement(Technique.HIDDEN_SINGLE, only.position, candidate))
            cells[only.position.index] = UnsolvedCell(only.position, Candidates.of(candidate))
        }
    }
}

/** Naked singles: a cell with one candidate left is solved. */
class SolveSingleCandidateTransformer : FieldProcessor {
    override fun process(field: Field, deductions: DeductionListener): Field = Field(field.cells.map { cell ->
        when (cell) {
            is SolvedCell -> cell
            is UnsolvedCell -> when (cell.candidates.size) {
                // A cell with no candidates is left as-is; the solver reports it as a contradiction.
                0 -> cell
                1 -> {
                    val value = cell.candidates.single()
                    deductions.onDeduction(Placement(Technique.NAKED_SINGLE, cell.position, value))
                    SolvedCell(cell.position, value)
                }
                else -> cell
            }
        }
    })
}

/** True when this cell is unsolved and [value] is still one of its candidates. */
private fun Cell.couldBe(value: Int): Boolean = this is UnsolvedCell && candidates.contains(value)

/** Every way of choosing [size] elements, order irrelevant. Lazy: most subsets are rejected at once. */
private fun <T> List<T>.combinations(size: Int): Sequence<List<T>> = sequence {
    if (size == 0) {
        yield(emptyList())
        return@sequence
    }
    for (index in indices) {
        if (this@combinations.size - index < size) break
        for (rest in subList(index + 1, this@combinations.size).combinations(size - 1)) {
            yield(listOf(this@combinations[index]) + rest)
        }
    }
}
