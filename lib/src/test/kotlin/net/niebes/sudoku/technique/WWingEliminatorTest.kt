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

internal class WWingEliminatorTest {

    /**
     * (0,0) and (4,4) both hold {3,6} and do not see each other. Row 2 holds 3 only at columns 0
     * and 4, and those two ends see one of the pair each - so one of the pair is forced to 6.
     */
    private fun wWingOn6() = field {
        candidates(0, 0, 3, 6)
        candidates(4, 4, 3, 6)
        (0..8).forEach { column ->
            if (column == 0 || column == 4) candidates(2, column, 3, 5) else candidates(2, column, 1, 2, 7)
        }
    }

    @Test
    fun clearsTheOtherValueFromWhatSeesBothCells() {
        val eliminated = WWingEliminator().eliminations(wWingOn6())

        assertThat(eliminated).containsExactlyInAnyOrder(
            Elimination(Technique.W_WING, CellPosition(0, 4), Candidates.of(6)),
            Elimination(Technique.W_WING, CellPosition(4, 0), Candidates.of(6))
        )
    }

    @Test
    fun staysSilentWhenTheTwoCellsSeeEachOther() {
        val eliminated = WWingEliminator().eliminations(field {
            candidates(0, 0, 3, 6)
            candidates(0, 4, 3, 6)   // same row as the first
            (0..8).forEach { column ->
                if (column == 0 || column == 4) candidates(2, column, 3, 5) else candidates(2, column, 1, 2, 7)
            }
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenNoStrongLinkJoinsThem() {
        val eliminated = WWingEliminator().eliminations(field {
            candidates(0, 0, 3, 6)
            candidates(4, 4, 3, 6)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheStrongLinkReachesOnlyOneOfThem() {
        val eliminated = WWingEliminator().eliminations(field {
            candidates(0, 0, 3, 6)
            candidates(4, 4, 3, 6)
            // Both ends of the link sit in column 0, so neither reaches (4,4).
            (0..8).forEach { column ->
                if (column == 0 || column == 1) candidates(2, column, 3, 5) else candidates(2, column, 1, 2, 7)
            }
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheTwoCellsHoldDifferentPairs() {
        val eliminated = WWingEliminator().eliminations(field {
            candidates(0, 0, 3, 6)
            candidates(4, 4, 3, 9)
            (0..8).forEach { column ->
                if (column == 0 || column == 4) candidates(2, column, 3, 5) else candidates(2, column, 1, 2, 7)
            }
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun leavesThePairAndTheLinkAlone() {
        val after = WWingEliminator().process(wWingOn6())

        assertThat(after.candidatesAt(0, 0)).isEqualTo(Candidates.of(3, 6))
        assertThat(after.candidatesAt(4, 4)).isEqualTo(Candidates.of(3, 6))
        assertThat(after.candidatesAt(2, 0)).isEqualTo(Candidates.of(3, 5))
        assertThat(after.candidatesAt(2, 4)).isEqualTo(Candidates.of(3, 5))
    }

    @Test
    fun reportsWhatItRemovedAsAWWing() {
        val recorder = RecordingDeductionListener()

        WWingEliminator().process(wWingOn6(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.W_WING }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            WWingEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
