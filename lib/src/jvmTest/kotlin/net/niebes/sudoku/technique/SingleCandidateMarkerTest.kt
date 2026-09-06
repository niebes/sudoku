package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Placement
import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.field
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.candidatesAt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SingleCandidateMarkerTest {

    /** Row 0 with 3 possible only at (0,0): everywhere else the row is down to {1,2}. */
    private fun rowWithSingle3() = field {
        candidates(0, 0, 3, 4)
        rowCandidates(0, 1..8, 1, 2)
    }

    @Test
    fun narrowsTheCellAValueFitsNowhereElseInTheHouse() {
        val after = SingleCandidateMarker().process(rowWithSingle3())

        assertThat(after.candidatesAt(0, 0)).isEqualTo(Candidates.of(3))
    }

    @Test
    fun reportsTheRestOfTheHouseAsTheEvidence() {
        // The single is hidden precisely because of the other eight cells: none of them can take
        // the 3, and a student shown those cells can check that for themselves.
        val recorder = RecordingDeductionListener()

        SingleCandidateMarker().process(rowWithSingle3(), recorder)

        assertThat(recorder.placements).singleElement()
            .isEqualTo(Placement(Technique.HIDDEN_SINGLE, CellPosition(0, 0), 3,
                because = (1..8).map { CellPosition(0, it) }))
    }
}
