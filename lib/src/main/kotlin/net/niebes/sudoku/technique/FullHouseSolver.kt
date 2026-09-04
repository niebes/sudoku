package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.DeductionListener
import net.niebes.sudoku.deduction.Placement
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell

/**
 * Full house: a house with one unsolved cell left takes the value missing from it.
 *
 * Strictly a special case of a naked single, but it needs no candidates at all - only the eight
 * values already there - so it is the cheapest deduction available and the one a person makes first.
 */
class FullHouseSolver : FieldProcessor {
    override fun process(field: Field, deductions: DeductionListener): Field {
        // A cell can be the last gap in two houses at once, so placements are keyed by position.
        val placements = LinkedHashMap<CellPosition, Placement>()

        field.houses().forEach { house ->
            val gap = house.unsolved().singleOrNull() ?: return@forEach

            var placed = Candidates.NONE
            house.cells.forEach { if (it is SolvedCell) placed += it.value }

            val missing = Candidates.ALL - placed
            // More than one value missing means the house repeats a value; the cell not being able
            // to take the missing one means the same thing. Either way this is not ours to report -
            // leaving the cell alone lets the contradiction surface as an exhausted cell.
            if (missing.size != 1 || !gap.couldBe(missing.single())) return@forEach

            // The eight filled cells are the whole of the argument: with them in place, only the
            // missing value fits the gap.
            val filled = house.cells.filterIsInstance<SolvedCell>().map { it.position }
            placements.putIfAbsent(gap.position, Placement(Technique.FULL_HOUSE, gap.position, missing.single(), filled))
        }
        if (placements.isEmpty()) return field

        val cells = field.cells.toMutableList()
        placements.values.forEach { placement ->
            deductions.onDeduction(placement)
            cells[placement.at.index] = SolvedCell(placement.at, placement.value)
        }
        return Field(cells)
    }
}
