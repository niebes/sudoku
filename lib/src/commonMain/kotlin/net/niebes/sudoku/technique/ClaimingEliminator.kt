package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Cell
import net.niebes.sudoku.model.Intersection

/**
 * Claiming, or locked candidates type 2: if along a line every cell that could take a value lies
 * inside one segment, the line's copy of that value is somewhere in that segment - so the value
 * cannot appear in that segment anywhere off the line.
 */
class ClaimingEliminator : LockedCandidatesEliminator(Technique.CLAIMING) {
    override fun confinedAwayFrom(intersection: Intersection): List<Cell> = intersection.lineOnly()
    override fun clear(intersection: Intersection): List<Cell> = intersection.segmentOnly()
}
