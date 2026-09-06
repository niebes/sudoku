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

internal class BugPlusOneEliminatorTest {

    private val open = setOf(
        CellPosition(0, 0), CellPosition(0, 3), CellPosition(1, 0), CellPosition(1, 3)
    )

    /**
     * Four cells left open. Take the 5 away from (1,3) and every value sits in exactly two cells of
     * every house it appears in - a grave, which has an even number of solutions. A puzzle with one
     * cannot be there, so the 5 is the answer.
     */
    private fun bugPlusOneOn5() = field {
        solvedFrom(Puzzles.classic.solution, open)
        candidates(0, 0, 1, 2)
        candidates(0, 3, 1, 2)
        candidates(1, 0, 1, 2)
        candidates(1, 3, 1, 2, 5)
    }

    @Test
    fun clearsEverythingButTheCandidateThatEscapesTheGrave() {
        val eliminated = BugPlusOneEliminator().eliminations(bugPlusOneOn5())

        assertThat(eliminated).containsExactly(
            Elimination(Technique.BUG_PLUS_ONE, CellPosition(1, 3), Candidates.of(1, 2))
        )
    }

    @Test
    fun staysSilentWhenTwoCellsHoldMoreThanAPair() {
        val eliminated = BugPlusOneEliminator().eliminations(field {
            solvedFrom(Puzzles.classic.solution, open)
            candidates(0, 0, 1, 2)
            candidates(0, 3, 1, 2, 7)
            candidates(1, 0, 1, 2)
            candidates(1, 3, 1, 2, 5)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenNoRemovalLeavesAGrave() {
        // 1 appears once in row 0 and three times nowhere: taking any candidate away leaves a count
        // that is neither zero nor two, so this is not a grave waiting to happen.
        val eliminated = BugPlusOneEliminator().eliminations(field {
            solvedFrom(Puzzles.classic.solution, open)
            candidates(0, 0, 1, 2)
            candidates(0, 3, 3, 4)
            candidates(1, 0, 1, 2)
            candidates(1, 3, 3, 4, 5)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentOnAnOrdinaryGridWithManyOpenCells() {
        val eliminated = BugPlusOneEliminator().eliminations(Puzzles.classic.field())

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun reportsWhatItRemovedAsBugPlusOne() {
        val recorder = RecordingDeductionListener()

        BugPlusOneEliminator().process(bugPlusOneOn5(), recorder)

        assertThat(recorder.eliminations).singleElement()
            .matches { it.technique == Technique.BUG_PLUS_ONE }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            BugPlusOneEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
