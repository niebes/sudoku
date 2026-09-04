package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell
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
