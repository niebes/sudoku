package net.niebes.sudoku.technique

import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.HardPuzzles
import net.niebes.sudoku.Puzzles
import net.niebes.sudoku.SudokuSolver
import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.field
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TurbotFishEliminatorTest {

    private fun Field.turbotOn(value: Int): List<CellPosition> =
        TurbotFishEliminator().eliminations(this)
            .filter { it.values == Candidates.of(value) }
            .map { it.at }

    /**
     * A skyscraper: rows 1 and 4 each have exactly two homes for 9, sharing column 1. Column 1 can
     * hold one 9, so at least one of the far ends - (1,7) or (4,8) - is the 9.
     */
    private fun skyscraperOn9() = field {
        (0..8).forEach { column ->
            if (column == 1 || column == 7) candidates(1, column, 9, 8) else candidates(1, column, 1, 2, 3)
            if (column == 1 || column == 8) candidates(4, column, 9, 8) else candidates(4, column, 1, 2, 3)
        }
    }

    @Test
    fun skyscraperClearsWhatSeesBothFarEnds() {
        val eliminated = skyscraperOn9().turbotOn(9)

        assertThat(eliminated).containsExactlyInAnyOrder(
            CellPosition(0, 8), CellPosition(2, 8),   // segment with (1,7), column with (4,8)
            CellPosition(3, 7), CellPosition(5, 7)    // segment with (4,8), column with (1,7)
        )
        // The evidence walks the chain end to end: far end, the two joined ends, the other far end.
        assertThat(TurbotFishEliminator().eliminations(skyscraperOn9()).filter { it.values == Candidates.of(9) })
            .allMatch {
                it.because == listOf(
                    CellPosition(1, 7), CellPosition(1, 1), CellPosition(4, 1), CellPosition(4, 8)
                )
            }
    }

    @Test
    fun twoStringKiteClearsTheCellWhereTheFarEndsCross() {
        // Row 1 holds 4 only at columns 0 and 7; column 2 only at rows 0 and 6. The near ends
        // (1,0) and (0,2) share segment 0, so one of (1,7) and (6,2) is the 4.
        val eliminated = field {
            (0..8).forEach { column ->
                if (column == 0 || column == 7) candidates(1, column, 4, 5) else candidates(1, column, 1, 2, 3)
            }
            (0..8).forEach { row ->
                if (row == 0 || row == 6) candidates(row, 2, 4, 5) else candidates(row, 2, 1, 2, 3)
            }
        }.turbotOn(4)

        assertThat(eliminated).containsExactly(CellPosition(6, 7))
    }

    @Test
    fun staysSilentWhenTheTwoStrongLinksAreNotJoined() {
        // Both rows have exactly two homes for 4, but no end of one sees any end of the other.
        val eliminated = field {
            (0..8).forEach { column ->
                if (column == 0 || column == 7) candidates(1, column, 4, 5) else candidates(1, column, 1, 2, 3)
                if (column == 3 || column == 5) candidates(4, column, 4, 5) else candidates(4, column, 1, 2, 3)
            }
        }.turbotOn(4)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenOnlyOneStrongLinkExists() {
        val eliminated = field {
            (0..8).forEach { column ->
                if (column == 0 || column == 7) candidates(1, column, 4, 5) else candidates(1, column, 1, 2, 3)
            }
        }.turbotOn(4)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun neverEliminatesFromTheEndsItReasonedAbout() {
        val after = TurbotFishEliminator().process(skyscraperOn9())

        listOf(CellPosition(1, 1), CellPosition(1, 7), CellPosition(4, 1), CellPosition(4, 8))
            .forEach { assertThat((after.cellAt(it)).couldBe(9)).describedAs("$it").isTrue() }
    }

    @Test
    fun reportsWhatItRemovedAsATurbotFish() {
        val recorder = RecordingDeductionListener()

        TurbotFishEliminator().process(skyscraperOn9(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.TURBOT_FISH }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            TurbotFishEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
