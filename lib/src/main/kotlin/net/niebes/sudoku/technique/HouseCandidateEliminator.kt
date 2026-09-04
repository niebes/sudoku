package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Field

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
