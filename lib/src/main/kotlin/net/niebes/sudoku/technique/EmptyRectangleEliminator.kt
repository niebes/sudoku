package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.ConjugatePair
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.House
import net.niebes.sudoku.model.HouseKind

/**
 * Empty rectangle: a segment whose candidates for a value all fit inside one of its rows plus one
 * of its columns, leaving the opposite 2x2 corner empty of that value.
 *
 * That shape is a strong link between the two arms: if the segment's value is not on the row arm it
 * is on the column arm, and the other way round. Joined to an ordinary strong link it pins down one
 * cell.
 *
 * With the rectangle on row `r` and column `c` of segment `B`, and a strong link in a column whose
 * ends are `(r, x)` outside `B` and `(R, x)`, the cell `(R, c)` cannot hold the value. Suppose it
 * did: column `c` would have its value at row `R`, so `B`'s value could not be on the column arm
 * and would have to be on the row arm, putting row `r`'s value inside `B` - so `(r, x)` is not the
 * value, the link forces `(R, x)` to be it, and row `R` holds it twice. The transpose applies with
 * rows and columns swapped.
 */
class EmptyRectangleEliminator : EliminationTechnique {
    override val technique = Technique.EMPTY_RECTANGLE

    override fun eliminations(field: Field): List<Elimination> = buildList {
        val segments = field.houses().filter { it.kind == HouseKind.SEGMENT }

        (1..SIZE).forEach { value ->
            val links = field.conjugatePairs(value)
            if (links.isEmpty()) return@forEach

            segments.forEach { segment ->
                rectanglesIn(segment, value).forEach { (corner, places) ->
                    links.forEach { link -> addAll(clear(field, value, corner, places, link)) }
                }
            }
        }
    }

    /**
     * Every (row, column) of [segment] whose cross covers all of the segment's places for [value],
     * paired with those places - they are the evidence an elimination cites.
     */
    private fun rectanglesIn(segment: House, value: Int): List<Pair<CellPosition, List<CellPosition>>> {
        if (segment.holds(value)) return emptyList()
        val places = segment.candidatesFor(value).map { it.position }
        if (places.size < 2) return emptyList()

        val rows = segment.cells.mapTo(sortedSetOf()) { it.position.row }
        val columns = segment.cells.mapTo(sortedSetOf()) { it.position.column }

        return rows.flatMap { row -> columns.map { column -> CellPosition(row, column) } }
            .filter { corner ->
                places.all { it.row == corner.row || it.column == corner.column } &&
                    // Both arms must be in use. All on one line is locked candidates, which says
                    // more than this does and has already said it.
                    places.any { it.row != corner.row } && places.any { it.column != corner.column }
            }
            .map { it to places }
    }

    private fun clear(
        field: Field,
        value: Int,
        corner: CellPosition,
        places: List<CellPosition>,
        link: ConjugatePair
    ): List<Elimination> = link.ends.mapNotNull { end ->
        val far = link.other(end)
        val onSegment = { position: CellPosition -> position.segment == corner.segment }
        if (onSegment(end.position)) return@mapNotNull null

        val target = when {
            // A link down a column, entering on the rectangle's row.
            end.position.column == far.position.column && end.position.row == corner.row ->
                CellPosition(far.position.row, corner.column)

            // A link along a row, entering on the rectangle's column.
            end.position.row == far.position.row && end.position.column == corner.column ->
                CellPosition(corner.row, far.position.column)

            else -> null
        } ?: return@mapNotNull null

        if (onSegment(target)) return@mapNotNull null
        if (!field.cellAt(target).couldBe(value)) return@mapNotNull null
        // The rectangle's places, then the link from the end that enters it to the end it forces.
        Elimination(technique, target, Candidates.of(value), places + listOf(end.position, far.position))
    }
}
