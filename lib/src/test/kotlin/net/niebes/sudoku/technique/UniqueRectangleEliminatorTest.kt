package net.niebes.sudoku.technique

import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.HardPuzzles
import net.niebes.sudoku.Puzzles
import net.niebes.sudoku.SudokuSolver
import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.field
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class UniqueRectangleEliminatorTest {

    /**
     * Rows 0 and 1, columns 0 and 3 - two rows, two columns, two segments. Three corners hold
     * exactly {3,7}; if the fourth did too, the pair could be swapped diagonally and the puzzle
     * would have two solutions.
     */
    private fun uniqueRectangleOn37() = field {
        candidates(0, 0, 3, 7)
        candidates(0, 3, 3, 7)
        candidates(1, 0, 3, 7)
        candidates(1, 3, 3, 7, 5)
    }

    @Test
    fun clearsThePairFromTheCornerThatCarriesExtras() {
        val eliminated = UniqueRectangleEliminator().eliminations(uniqueRectangleOn37())

        assertThat(eliminated).containsExactly(
            Elimination(Technique.UNIQUE_RECTANGLE, CellPosition(1, 3), Candidates.of(3, 7))
        )
    }

    @Test
    fun staysSilentWhenTheRectangleSpansFourSegments() {
        // Two rows in different bands and two columns in different stacks touch four segments, and
        // the swap argument needs exactly two.
        val eliminated = UniqueRectangleEliminator().eliminations(field {
            candidates(0, 0, 3, 7)
            candidates(0, 3, 3, 7)
            candidates(4, 0, 3, 7)
            candidates(4, 3, 3, 7, 5)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenFewerThanThreeCornersHoldExactlyThePair() {
        val eliminated = UniqueRectangleEliminator().eliminations(field {
            candidates(0, 0, 3, 7)
            candidates(0, 3, 3, 7)
            candidates(1, 0, 3, 7, 9)
            candidates(1, 3, 3, 7, 5)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenAllFourCornersAreAlreadyThePair() {
        // This is the deadly pattern itself: the puzzle has two solutions and there is nothing to
        // remove. Reporting an elimination here would be inventing one.
        val eliminated = UniqueRectangleEliminator().eliminations(field {
            candidates(0, 0, 3, 7)
            candidates(0, 3, 3, 7)
            candidates(1, 0, 3, 7)
            candidates(1, 3, 3, 7)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun reportsWhatItRemovedAsAUniqueRectangle() {
        val recorder = RecordingDeductionListener()

        UniqueRectangleEliminator().process(uniqueRectangleOn37(), recorder)

        assertThat(recorder.eliminations).singleElement()
            .matches { it.technique == Technique.UNIQUE_RECTANGLE }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            UniqueRectangleEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
