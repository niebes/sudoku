package net.niebes.sudoku

import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ClaimingEliminatorTest {

    /**
     * Row 1 with 3 possible only in columns 0..2, which is segment 0. The other digits deliberately
     * reach past that segment, so 3 is the only value this row claims with.
     */
    private fun rowClaimingWith3() = field {
        rowCandidates(1, 0..2, 3, 4)
        rowCandidates(1, 3..8, 1, 2, 4)
    }

    private val insideSegmentOffTheRow = listOf(
        CellPosition(0, 0), CellPosition(0, 1), CellPosition(0, 2),
        CellPosition(2, 0), CellPosition(2, 1), CellPosition(2, 2)
    )

    private fun Field.claimingAt(value: Int): List<CellPosition> =
        ClaimingEliminator().eliminations(this)
            .filter { it.values == Candidates.of(value) }
            .map { it.at }

    @Test
    fun clearsTheSegmentOffARowThatConfinesTheValueToIt() {
        assertThat(rowClaimingWith3().claimingAt(3))
            .containsExactlyInAnyOrderElementsOf(insideSegmentOffTheRow)
    }

    @Test
    fun clearsTheSegmentOffAColumnThatConfinesTheValueToIt() {
        val eliminated = field {
            (0..2).forEach { candidates(it, 1, 3, 4) }
            (3..8).forEach { candidates(it, 1, 1, 2, 4) }
        }.claimingAt(3)

        assertThat(eliminated).containsExactlyInAnyOrder(
            CellPosition(0, 0), CellPosition(1, 0), CellPosition(2, 0),
            CellPosition(0, 2), CellPosition(1, 2), CellPosition(2, 2)
        )
    }

    @Test
    fun staysSilentWhenTheValueAlsoFitsElsewhereOnTheLine() {
        val eliminated = field {
            rowCandidates(1, 0..2, 3, 4)
            rowCandidates(1, 3..8, 1, 2, 4)
            candidates(1, 7, 1, 2, 3)   // 3 escapes segment 0
        }.claimingAt(3)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheValueIsAlreadyPlacedOnTheLine() {
        // The overlap still lists 3 because peer elimination has not caught up. Reading candidates
        // alone would conclude that segment 0 holds row 1's 3 and clear the rest of the segment -
        // wrong, since row 1 cannot hold a 3 inside this segment at all once one sits at (1,5).
        val eliminated = field {
            rowCandidates(1, 0..2, 3, 4)
            rowCandidates(1, 3..8, 1, 2, 4)
            solved(1, 5, 3)
        }.claimingAt(3)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheValueIsAlreadyPlacedInTheSegment() {
        val eliminated = field {
            rowCandidates(1, 0..2, 3, 4)
            rowCandidates(1, 3..8, 1, 2, 4)
            solved(0, 0, 3)
        }.claimingAt(3)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun leavesTheLineItselfUntouched() {
        val after = ClaimingEliminator().process(rowClaimingWith3())

        assertThat(after.candidatesAt(1, 0)).isEqualTo(Candidates.of(3, 4))
        assertThat(after.candidatesAt(1, 2)).isEqualTo(Candidates.of(3, 4))
        assertThat(after.candidatesAt(0, 0).contains(3)).isFalse()
    }

    @Test
    fun reportsWhatItRemovedAsClaiming() {
        val recorder = RecordingDeductionListener()

        ClaimingEliminator().process(rowClaimingWith3(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.CLAIMING }
        assertThat(recorder.eliminations.map { it.at })
            .containsExactlyInAnyOrderElementsOf(insideSegmentOffTheRow)
    }

    @Test
    fun isTheMirrorOfPointingAndDoesNotFireOnItsGrid() {
        // A segment confining a value to one line is pointing's pattern, not claiming's.
        val pointingGrid = field {
            rowCandidates(0, 0..2, 1, 2, 4)
            rowCandidates(2, 0..2, 1, 2, 4)
            rowCandidates(1, 0..2, 3, 4)
        }

        assertThat(PointingEliminator().eliminations(pointingGrid)).isNotEmpty()
        assertThat(rowClaimingWith3().let { PointingEliminator().eliminations(it) }).isEmpty()
        assertThat(ClaimingEliminator().eliminations(pointingGrid)).isEmpty()
    }

    @Test
    fun contributesToRealPuzzles() {
        val recorder = RecordingDeductionListener()

        SudokuSolver(recorder).solve(Puzzles.needsSubsets.field())

        assertThat(recorder.techniquesUsed()).contains(Technique.CLAIMING)
    }
}
