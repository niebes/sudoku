package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.CellPosition.Companion.SIZE
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.Intersection

/**
 * The shape both directions of locked candidates share: a value that cannot appear on one side of a
 * segment-line overlap must lie in the overlap, and so leaves the other side. Pointing and claiming
 * differ only in which side is which.
 */
abstract class LockedCandidatesEliminator(
    final override val technique: Technique
) : EliminationTechnique {

    /** The side the value must be absent from for it to be confined to the overlap. */
    protected abstract fun confinedAwayFrom(intersection: Intersection): List<Cell>

    /** The side the value is then cleared from. */
    protected abstract fun clear(intersection: Intersection): List<Cell>

    final override fun eliminations(field: Field): List<Elimination> = buildList {
        field.intersections().forEach { intersection ->
            (1..SIZE).forEach value@{ value ->
                // A value already placed in either house makes the question moot, and answering it
                // from candidate state alone would be wrong: cells can still carry a candidate that
                // peer elimination has not caught up with, which would make the value look confined
                // to the overlap when it is in fact settled elsewhere.
                if (intersection.segment.holds(value) || intersection.line.holds(value)) return@value
                if (intersection.cells.none { it.couldBe(value) }) return@value
                if (confinedAwayFrom(intersection).any { it.couldBe(value) }) return@value

                clear(intersection)
                    .filter { it.couldBe(value) }
                    .forEach { add(Elimination(technique, it.position, Candidates.of(value))) }
            }
        }
    }
}
