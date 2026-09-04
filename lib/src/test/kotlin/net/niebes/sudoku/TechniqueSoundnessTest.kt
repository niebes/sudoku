package net.niebes.sudoku

import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.UnsolvedCell
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The one property every technique in the chain has to hold: it may narrow a cell, but it must
 * never remove the value that cell takes in the puzzle's solution.
 *
 * This guards the failure mode these techniques keep producing - reasoning against a grid that the
 * technique's own earlier steps have already changed - which shows up as a plausible-looking but
 * wrong elimination rather than as a crash.
 */
internal class TechniqueSoundnessTest {

    @Test
    fun propagationNeverEliminatesAValueFromTheSolution() {
        Puzzles.all.forEach { puzzle ->
            val propagated = SudokuSolver().propagate(puzzle.field()).field

            propagated.cells.forEachIndexed { index, cell ->
                val expected = puzzle.valueAt(index)
                when (cell) {
                    is SolvedCell -> assertThat(cell.value)
                        .describedAs("placed value at ${cell.position} in ${puzzle.givens}")
                        .isEqualTo(expected)

                    is UnsolvedCell -> assertThat(cell.candidates.contains(expected))
                        .describedAs("candidate $expected survives at ${cell.position} in ${puzzle.givens}")
                        .isTrue()
                }
            }
        }
    }

    @Test
    fun searchSolvesEveryPuzzleInTheCorpus() {
        Puzzles.all.forEach { puzzle ->
            val result = SudokuSolver().solve(puzzle.field())

            assertThat(result)
                .describedAs(puzzle.givens)
                .isInstanceOf(Solved::class.java)
            assertThat(result.field).isEqualTo(puzzle.solved())
        }
    }
}
