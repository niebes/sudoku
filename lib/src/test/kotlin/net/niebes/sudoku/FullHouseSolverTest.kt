package net.niebes.sudoku

import net.niebes.sudoku.deduction.Placement
import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.CellPosition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class FullHouseSolverTest {

    /** Eight of the nine cells of row 0, leaving column 8 as the only gap. Missing value: 6. */
    private fun rowWithOneGap() = field {
        solved(0, 0, 4); solved(0, 1, 1); solved(0, 2, 7)
        solved(0, 3, 9); solved(0, 4, 2); solved(0, 5, 8)
        solved(0, 6, 5); solved(0, 7, 3)
    }

    @Test
    fun placesTheValueMissingFromARow() {
        val after = FullHouseSolver().process(rowWithOneGap())

        assertThat(after.valueAt(0, 8)).isEqualTo(6)
    }

    @Test
    fun placesTheValueMissingFromAColumn() {
        val after = FullHouseSolver().process(field {
            solved(0, 3, 4); solved(1, 3, 1); solved(2, 3, 7)
            solved(3, 3, 9); solved(4, 3, 2); solved(5, 3, 8)
            solved(6, 3, 5); solved(7, 3, 3)
        })

        assertThat(after.valueAt(8, 3)).isEqualTo(6)
    }

    @Test
    fun placesTheValueMissingFromASegment() {
        val after = FullHouseSolver().process(field {
            solved(0, 0, 4); solved(0, 1, 1); solved(0, 2, 7)
            solved(1, 0, 9); solved(1, 1, 2); solved(1, 2, 8)
            solved(2, 0, 5); solved(2, 1, 3)
        })

        assertThat(after.valueAt(2, 2)).isEqualTo(6)
    }

    @Test
    fun reportsThePlacementAsAFullHouse() {
        val recorder = RecordingDeductionListener()

        FullHouseSolver().process(rowWithOneGap(), recorder)

        assertThat(recorder.placements).singleElement()
            .isEqualTo(Placement(Technique.FULL_HOUSE, CellPosition(0, 8), 6))
    }

    @Test
    fun placesOnceWhenACellIsTheLastGapInTwoHousesAtOnce() {
        val recorder = RecordingDeductionListener()
        // r8c8 is the only gap in both row 8 and column 8.
        val after = FullHouseSolver().process(field {
            solved(8, 0, 4); solved(8, 1, 1); solved(8, 2, 7)
            solved(8, 3, 9); solved(8, 4, 2); solved(8, 5, 8)
            solved(8, 6, 5); solved(8, 7, 3)
            solved(0, 8, 4); solved(1, 8, 1); solved(2, 8, 7)
            solved(3, 8, 9); solved(4, 8, 2); solved(5, 8, 8)
            solved(6, 8, 5); solved(7, 8, 3)
        }, recorder)

        assertThat(after.valueAt(8, 8)).isEqualTo(6)
        assertThat(recorder.placements).hasSize(1)
    }

    @Test
    fun doesNothingWhenAHouseHasMoreThanOneGap() {
        val recorder = RecordingDeductionListener()
        val before = field {
            solved(0, 0, 4); solved(0, 1, 1); solved(0, 2, 7)
            solved(0, 3, 9); solved(0, 4, 2); solved(0, 5, 8)
            solved(0, 6, 5)
        }

        val after = FullHouseSolver().process(before, recorder)

        assertThat(after).isEqualTo(before)
        assertThat(recorder.deductions).isEmpty()
    }

    @Test
    fun leavesTheGapAloneWhenItCannotTakeTheMissingValue() {
        // The row needs a 6 but the cell cannot hold one: the grid is already broken, and saying so
        // is the contradiction check's job, not this technique's.
        val recorder = RecordingDeductionListener()
        val before = field {
            solved(0, 0, 4); solved(0, 1, 1); solved(0, 2, 7)
            solved(0, 3, 9); solved(0, 4, 2); solved(0, 5, 8)
            solved(0, 6, 5); solved(0, 7, 3)
            candidates(0, 8, 1, 2)
        }

        val after = FullHouseSolver().process(before, recorder)

        assertThat(after).isEqualTo(before)
        assertThat(recorder.deductions).isEmpty()
    }

    @Test
    fun contributesToRealPuzzles() {
        val recorder = RecordingDeductionListener()

        SudokuSolver(recorder).solve(Puzzles.classic.field())

        assertThat(recorder.techniquesUsed()).contains(Technique.FULL_HOUSE)
    }
}
