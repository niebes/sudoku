package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.Intersection

/**
 * Pointing, or locked candidates type 1: if within a segment every cell that could take a value
 * lies on one line, the segment's copy of that value is somewhere on that line - so the value
 * cannot appear on that line anywhere outside the segment.
 */
class PointingEliminator : LockedCandidatesEliminator(Technique.POINTING) {
    override fun confinedAwayFrom(intersection: Intersection): List<Cell> = intersection.segmentOnly()
    override fun clear(intersection: Intersection): List<Cell> = intersection.lineOnly()
}
