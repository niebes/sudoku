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

internal class FinnedFishEliminatorTest {

    private fun Field.finnedOn(value: Int): List<CellPosition> =
        FinnedFishEliminator().eliminations(this)
            .filter { it.values == Candidates.of(value) }
            .map { it.at }

    /**
     * Rows 1 and 5 would be an X-wing on columns 2 and 6, but row 5 has a third 5 at column 7 -
     * the fin. Either the fin holds the 5, or the X-wing is real; the cells that lose their 5 are
     * the ones both conclusions reach.
     */
    private fun finnedXWingOn5() = field {
        (0..8).forEach { column ->
            if (column == 2 || column == 6) candidates(1, column, 5, 4) else candidates(1, column, 1, 2, 3)
            if (column == 2 || column == 6 || column == 7) candidates(5, column, 5, 4)
            else candidates(5, column, 1, 2, 3)
        }
    }

    @Test
    fun clearsOnlyWhatTheFishAndTheFinBothReach() {
        val eliminated = finnedXWingOn5().finnedOn(5)

        // Both sit in a cover column and share the fin's segment.
        assertThat(eliminated).containsExactlyInAnyOrder(CellPosition(3, 6), CellPosition(4, 6))
        // The evidence is every place the base rows leave for the value, fin included.
        assertThat(FinnedFishEliminator().eliminations(finnedXWingOn5())
            .filter { it.values == Candidates.of(5) })
            .allMatch {
                it.because == listOf(
                    CellPosition(1, 2), CellPosition(1, 6),
                    CellPosition(5, 2), CellPosition(5, 6), CellPosition(5, 7)
                )
            }
    }

    @Test
    fun sparesCoverCellsThatDoNotSeeTheFin() {
        val after = FinnedFishEliminator().process(finnedXWingOn5())

        // A plain X-wing would have taken the whole of both columns; the fin holds it back.
        listOf(CellPosition(0, 2), CellPosition(3, 2), CellPosition(0, 6), CellPosition(8, 6))
            .forEach { assertThat(after.cellAt(it).couldBe(5)).describedAs("$it").isTrue() }
    }

    @Test
    fun staysSilentOnAFishWithNoFinAtAll() {
        // A plain X-wing is basic fish's to report, not this one's.
        val eliminated = field {
            listOf(1, 4).forEach { row ->
                (0..8).forEach { column ->
                    if (column == 2 || column == 6) candidates(row, column, 4, 5)
                    else candidates(row, column, 1, 2, 3)
                }
            }
        }.finnedOn(4)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheFinnedLineSpreadsTooWide() {
        val eliminated = field {
            (0..8).forEach { column ->
                if (column == 2 || column == 6) candidates(1, column, 5, 4) else candidates(1, column, 1, 2, 3)
                // Two fins in different segments leave nothing that sees them all.
                if (column == 2 || column == 6 || column == 0 || column == 7) candidates(5, column, 5, 4)
                else candidates(5, column, 1, 2, 3)
            }
        }.finnedOn(5)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun reportsWhatItRemovedAsAFinnedFish() {
        val recorder = RecordingDeductionListener()

        FinnedFishEliminator().process(finnedXWingOn5(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.FINNED_FISH }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            FinnedFishEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
