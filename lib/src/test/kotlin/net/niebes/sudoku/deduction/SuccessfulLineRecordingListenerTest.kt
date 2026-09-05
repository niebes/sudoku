package net.niebes.sudoku.deduction

import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.HardPuzzles
import net.niebes.sudoku.Puzzles
import net.niebes.sudoku.Solved
import net.niebes.sudoku.SudokuSolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SuccessfulLineRecordingListenerTest {

    /**
     * The property a teaching consumer needs: every placement in the pruned trace is a move the
     * real solution makes, and every elimination removes only what the solution never held. Run
     * over all three corpora because abandoned branches only appear where search does.
     */
    @Test
    fun keepsOnlyDeductionsFromTheLineThatReachesTheSolution() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val trace = SuccessfulLineRecordingListener()

            assertThat(SudokuSolver(trace).solve(puzzle.field())).isInstanceOf(Solved::class.java)

            trace.placements.forEach { placement ->
                assertThat(placement.value)
                    .describedAs("${placement.technique} at ${placement.at} in ${puzzle.givens}")
                    .isEqualTo(puzzle.valueAt(placement.at.index))
            }
            trace.eliminations.forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.technique} at ${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }

    @Test
    fun theUnprunedTraceReallyDoesTeachWrongMoves() {
        // Why this listener exists, measured rather than asserted: the plain recording listener
        // keeps placements from abandoned branches, and some of them contradict the solution.
        val contradicting = (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).sumOf { puzzle ->
            val trace = RecordingDeductionListener()
            SudokuSolver(trace).solve(puzzle.field())
            trace.placements.count { it.value != puzzle.valueAt(it.at.index) }
        }

        assertThat(contradicting).isGreaterThan(0)
    }
}
