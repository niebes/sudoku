package net.niebes.sudoku.technique

import net.niebes.sudoku.candidatesAt
import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.field
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HouseCandidateEliminatorTest {

    @Test
    fun removesValuesAlreadyPlacedInTheRowColumnOrSegment() {
        val after = HouseCandidateEliminator().process(field {
            solved(0, 8, 3)   // same row
            solved(8, 0, 5)   // same column
            solved(1, 1, 7)   // same segment
        })

        assertThat(after.candidatesAt(0, 0)).isEqualTo(Candidates.of(1, 2, 4, 6, 8, 9))
    }

    @Test
    fun reportsEachRemovalAgainstTheCellItAffected() {
        val recorder = RecordingDeductionListener()

        HouseCandidateEliminator().process(field {
            solved(0, 0, 4)
            candidates(0, 1, 4, 8)
            rowCandidates(0, 2..8, 8, 9)
        }, recorder)

        // The solved peer that holds the 4 is the whole of the argument.
        assertThat(recorder.eliminations).contains(
            Elimination(Technique.PEER_ELIMINATION, CellPosition(0, 1), Candidates.of(4),
                because = listOf(CellPosition(0, 0)))
        )
        assertThat(recorder.deductions).allMatch { it.technique == Technique.PEER_ELIMINATION }
    }
}
