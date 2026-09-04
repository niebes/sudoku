package net.niebes.sudoku.technique

import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.Puzzles
import net.niebes.sudoku.SudokuSolver
import net.niebes.sudoku.candidatesAt
import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.field
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class XyWingEliminatorTest {

    /**
     * Pivot (1,1) holds {5,8}. Pincer (1,6) shares its row and holds {3,5}; pincer (5,1) shares its
     * column and holds {3,8}. Whichever the pivot takes, one pincer is forced to 3.
     */
    private fun xyWingOn3() = field {
        candidates(1, 1, 5, 8)
        candidates(1, 6, 3, 5)
        candidates(5, 1, 3, 8)
    }

    @Test
    fun clearsTheSharedValueFromWhatSeesBothPincers() {
        val eliminated = XyWingEliminator().eliminations(xyWingOn3())

        assertThat(eliminated).containsExactly(
            Elimination(Technique.XY_WING, CellPosition(5, 6), Candidates.of(3))
        )
    }

    @Test
    fun staysSilentWhenAPincerDoesNotSeeThePivot() {
        val eliminated = XyWingEliminator().eliminations(field {
            candidates(1, 1, 5, 8)
            candidates(1, 6, 3, 5)
            candidates(5, 4, 3, 8)   // sees neither the pivot's row nor its column
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenThePincersDoNotShareAThirdValue() {
        val eliminated = XyWingEliminator().eliminations(field {
            candidates(1, 1, 5, 8)
            candidates(1, 6, 3, 5)
            candidates(5, 1, 4, 8)   // 4, not 3
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenAPincerRepeatsThePivotsOwnPair() {
        val eliminated = XyWingEliminator().eliminations(field {
            candidates(1, 1, 5, 8)
            candidates(1, 6, 5, 8)
            candidates(5, 1, 3, 8)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun ignoresAPivotWithMoreThanTwoCandidates() {
        // Three candidates in the pivot is an XYZ-wing, which eliminates less and is not this rule.
        val eliminated = XyWingEliminator().eliminations(field {
            candidates(1, 1, 5, 8, 3)
            candidates(1, 6, 3, 5)
            candidates(5, 1, 3, 8)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun leavesThePivotAndPincersAlone() {
        val after = XyWingEliminator().process(xyWingOn3())

        assertThat(after.candidatesAt(1, 1)).isEqualTo(Candidates.of(5, 8))
        assertThat(after.candidatesAt(1, 6)).isEqualTo(Candidates.of(3, 5))
        assertThat(after.candidatesAt(5, 1)).isEqualTo(Candidates.of(3, 8))
    }

    @Test
    fun reportsWhatItRemovedAsAnXyWing() {
        val recorder = RecordingDeductionListener()

        XyWingEliminator().process(xyWingOn3(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.XY_WING }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            XyWingEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
