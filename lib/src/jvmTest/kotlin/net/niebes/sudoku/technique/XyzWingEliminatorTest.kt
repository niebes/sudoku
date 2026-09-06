package net.niebes.sudoku.technique

import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.HardPuzzles
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

internal class XyzWingEliminatorTest {

    /**
     * Pivot (1,1) holds {1,4,7}; pincer (1,2) holds {1,7} and shares its row and segment, pincer
     * (5,1) holds {4,7} and shares its column. All three could be 7, so only cells seeing all three
     * lose it.
     */
    private fun xyzWingOn7() = field {
        candidates(1, 1, 1, 4, 7)
        candidates(1, 2, 1, 7)
        candidates(5, 1, 4, 7)
    }

    @Test
    fun clearsTheSharedValueFromWhatSeesAllThreeCells() {
        val eliminated = XyzWingEliminator().eliminations(xyzWingOn7())

        // Pivot first, then the pincers: the order the argument reads in.
        val wing = listOf(CellPosition(1, 1), CellPosition(1, 2), CellPosition(5, 1))
        assertThat(eliminated).containsExactlyInAnyOrder(
            Elimination(Technique.XYZ_WING, CellPosition(0, 1), Candidates.of(7), wing),
            Elimination(Technique.XYZ_WING, CellPosition(2, 1), Candidates.of(7), wing)
        )
    }

    @Test
    fun sparesCellsThatSeeBothPincersButNotThePivot() {
        // The difference from an XY-wing: the pivot can be the value too, so seeing the pincers is
        // not enough. (3,2) sees both pincers and keeps its 7.
        val after = XyzWingEliminator().process(xyzWingOn7())

        assertThat(after.candidatesAt(3, 2).contains(7)).isTrue()
        assertThat(after.candidatesAt(4, 2).contains(7)).isTrue()
        assertThat(after.candidatesAt(5, 2).contains(7)).isTrue()
    }

    @Test
    fun staysSilentWhenThePincersDoNotCoverThePivotsCandidates() {
        val eliminated = XyzWingEliminator().eliminations(field {
            candidates(1, 1, 1, 4, 7)
            candidates(1, 2, 1, 7)
            candidates(5, 1, 1, 7)   // together only {1,7}, so 4 has nowhere to go
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenAPincerDoesNotSeeThePivot() {
        val eliminated = XyzWingEliminator().eliminations(field {
            candidates(1, 1, 1, 4, 7)
            candidates(1, 2, 1, 7)
            candidates(5, 4, 4, 7)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun ignoresATwoCandidatePivot() {
        // That is an XY-wing, which eliminates more and is a different rule.
        val eliminated = XyzWingEliminator().eliminations(field {
            candidates(1, 1, 4, 7)
            candidates(1, 2, 1, 7)
            candidates(5, 1, 4, 7)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun leavesThePivotAndPincersAlone() {
        val after = XyzWingEliminator().process(xyzWingOn7())

        assertThat(after.candidatesAt(1, 1)).isEqualTo(Candidates.of(1, 4, 7))
        assertThat(after.candidatesAt(1, 2)).isEqualTo(Candidates.of(1, 7))
        assertThat(after.candidatesAt(5, 1)).isEqualTo(Candidates.of(4, 7))
    }

    @Test
    fun reportsWhatItRemovedAsAnXyzWing() {
        val recorder = RecordingDeductionListener()

        XyzWingEliminator().process(xyzWingOn7(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.XYZ_WING }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            XyzWingEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
