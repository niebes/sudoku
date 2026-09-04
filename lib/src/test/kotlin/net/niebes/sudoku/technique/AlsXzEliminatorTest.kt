package net.niebes.sudoku.technique

import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.HardPuzzles
import net.niebes.sudoku.Puzzles
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

internal class AlsXzEliminatorTest {

    private fun Field.alsXzOn(value: Int): List<CellPosition> =
        AlsXzEliminator().eliminations(this)
            .filter { it.values == Candidates.of(value) }
            .map { it.at }

    /**
     * Two almost locked sets: {2,3} alone at (0,4), and {2,9} with {3,9} across (4,4) and (4,5) -
     * two cells holding three values. They share 2 and 3, and the 2s see each other down column 4,
     * so 2 can live in only one of them. Whichever set misses out on the 2 locks, and a locked set
     * uses every value it has - so the 3 is in one of them either way.
     */
    private fun alsXzOn3() = field {
        candidates(0, 4, 2, 3)
        candidates(4, 4, 2, 9)
        candidates(4, 5, 3, 9)
    }

    @Test
    fun clearsTheSharedValueFromWhatSeesEveryPlaceItCouldGo() {
        val eliminated = alsXzOn3().alsXzOn(3)

        assertThat(eliminated).containsExactlyInAnyOrder(
            CellPosition(0, 5), CellPosition(1, 5), CellPosition(2, 5),
            CellPosition(3, 4), CellPosition(5, 4)
        )
    }

    @Test
    fun staysSilentWhenTheSharedValueIsNotRestricted() {
        // Same two sets, but the 2s no longer see each other, so both could hold one.
        val eliminated = field {
            candidates(0, 1, 2, 3)
            candidates(4, 4, 2, 9)
            candidates(4, 5, 3, 9)
        }.alsXzOn(3)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheSetsShareOnlyOneValue() {
        // Nothing left to eliminate once the restricted candidate is spent.
        val eliminated = field {
            candidates(0, 4, 2, 7)
            candidates(4, 4, 2, 9)
            candidates(4, 5, 3, 9)
        }.alsXzOn(3)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheSetsOverlap() {
        // A set paired with one containing it proves nothing: they are not independent.
        val eliminated = field {
            candidates(4, 4, 2, 9)
            candidates(4, 5, 3, 9)
        }.alsXzOn(3)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun leavesBothSetsAlone() {
        val after = AlsXzEliminator().process(alsXzOn3())

        assertThat(after.candidatesAt(0, 4)).isEqualTo(Candidates.of(2, 3))
        assertThat(after.candidatesAt(4, 4)).isEqualTo(Candidates.of(2, 9))
        assertThat(after.candidatesAt(4, 5)).isEqualTo(Candidates.of(3, 9))
    }

    @Test
    fun reportsWhatItRemovedAsAlsXz() {
        val recorder = RecordingDeductionListener()

        AlsXzEliminator().process(alsXzOn3(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.ALS_XZ }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            AlsXzEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
