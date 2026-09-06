package net.niebes.sudoku.technique

import net.niebes.sudoku.Puzzles
import net.niebes.sudoku.SolveResult
import net.niebes.sudoku.Solved
import net.niebes.sudoku.Stalled
import net.niebes.sudoku.SudokuSolver
import net.niebes.sudoku.candidatesAt
import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.field
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PointingEliminatorTest {

    /**
     * Segment 0 with 3 possible only on row 1. The other digits deliberately spread across all
     * three rows, so 3 is the only value this segment points with.
     */
    private fun segmentPointingWith3() = field {
        rowCandidates(0, 0..2, 1, 2, 4)
        rowCandidates(2, 0..2, 1, 2, 4)
        rowCandidates(1, 0..2, 3, 4)
    }

    private fun Field.pointingAt(value: Int): List<CellPosition> =
        PointingEliminator().eliminations(this)
            .filter { it.values == Candidates.of(value) }
            .map { it.at }

    @Test
    fun clearsTheLineOutsideASegmentThatConfinesTheValueToOneRow() {
        // In segment 0, 3 can only go on row 1 - so row 1's 3 is inside segment 0.
        val eliminated = segmentPointingWith3().pointingAt(3)

        assertThat(eliminated).containsExactlyInAnyOrderElementsOf((3..8).map { CellPosition(1, it) })
    }

    @Test
    fun clearsTheLineOutsideASegmentThatConfinesTheValueToOneColumn() {
        val eliminated = field {
            (0..2).forEach { candidates(it, 0, 1, 2, 4) }
            (0..2).forEach { candidates(it, 2, 1, 2, 4) }
            (0..2).forEach { candidates(it, 1, 3, 4) }
        }.pointingAt(3)

        assertThat(eliminated).containsExactlyInAnyOrderElementsOf((3..8).map { CellPosition(it, 1) })
    }

    @Test
    fun staysSilentWhenTheValueAlsoFitsElsewhereInTheSegment() {
        val eliminated = field {
            rowCandidates(0, 0..2, 1, 2, 4)
            rowCandidates(2, 0..2, 1, 2, 4)
            rowCandidates(1, 0..2, 3, 4)
            candidates(0, 0, 1, 2, 3)   // 3 escapes row 1
        }.pointingAt(3)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheValueIsAlreadyPlacedInTheSegment() {
        // The overlap still lists 3 because peer elimination has not caught up. Reading candidates
        // alone would conclude that row 1 holds segment 0's 3 and clear the rest of row 1 - the
        // exact opposite of the truth, since row 1 cannot hold a 3 inside this segment at all.
        val eliminated = field {
            solved(0, 0, 3)
            candidates(0, 1, 1, 2, 4)
            candidates(0, 2, 1, 2, 4)
            rowCandidates(2, 0..2, 1, 2, 4)
            rowCandidates(1, 0..2, 3, 4)
        }.pointingAt(3)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheValueIsAlreadyPlacedOnTheLine() {
        val eliminated = field {
            rowCandidates(0, 0..2, 1, 2, 4)
            rowCandidates(2, 0..2, 1, 2, 4)
            rowCandidates(1, 0..2, 3, 4)
            solved(1, 5, 3)
        }.pointingAt(3)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun leavesTheSegmentItselfUntouched() {
        val after = PointingEliminator().process(segmentPointingWith3())

        assertThat(after.candidatesAt(1, 0)).isEqualTo(Candidates.of(3, 4))
        assertThat(after.candidatesAt(1, 2)).isEqualTo(Candidates.of(3, 4))
        assertThat(after.candidatesAt(1, 3).contains(3)).isFalse()
    }

    @Test
    fun reportsWhatItRemovedAsPointing() {
        val recorder = RecordingDeductionListener()

        PointingEliminator().process(segmentPointingWith3(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.POINTING }
        assertThat(recorder.eliminations.map { it.at })
            .containsExactlyInAnyOrderElementsOf((3..8).map { CellPosition(1, it) })
        // The evidence is the overlap: the cells the value is confined to.
        assertThat(recorder.eliminations).allMatch {
            it.because == listOf(CellPosition(1, 0), CellPosition(1, 1), CellPosition(1, 2))
        }
    }

    @Test
    fun takesAPuzzleBeyondWhatSinglesCanReach() {
        val withoutPointing = SudokuSolver(
            listOf(
                FullHouseSolver(),
                HouseCandidateEliminator(),
                SingleCandidateMarker(),
                SolveSingleCandidateTransformer()
            )
        )
        val recorder = RecordingDeductionListener()

        assertThat(withoutPointing.propagate(Puzzles.needsPointing.field()))
            .isInstanceOf(Stalled::class.java)
        assertThat(SudokuSolver(recorder).propagate(Puzzles.needsPointing.field()))
            .isInstanceOf(Solved::class.java)
        assertThat(recorder.techniquesUsed()).contains(Technique.POINTING)
    }
}
