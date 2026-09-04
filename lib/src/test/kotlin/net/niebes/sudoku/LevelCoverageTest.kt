package net.niebes.sudoku

import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.technique.ClaimingEliminator
import net.niebes.sudoku.technique.FullHouseSolver
import net.niebes.sudoku.technique.HiddenSubsetEliminator
import net.niebes.sudoku.technique.HouseCandidateEliminator
import net.niebes.sudoku.technique.NakedSubsetEliminator
import net.niebes.sudoku.technique.PointingEliminator
import net.niebes.sudoku.technique.SingleCandidateMarker
import net.niebes.sudoku.technique.SolveSingleCandidateTransformer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * What the implemented levels buy. Propagation alone - no search - is expected to finish every
 * puzzle up to level three, and to stall on the two that need chains.
 */
internal class LevelCoverageTest {

    private val withinLevelThree = listOf(
        Puzzles.classic, Puzzles.singlesOnly, Puzzles.needsPointing, Puzzles.needsSubsets
    )
    private val beyondLevelThree = listOf(Puzzles.needsChains, Puzzles.minimalClues)

    @Test
    fun levelsOneToThreeFinishTheirPuzzlesWithoutGuessing() {
        withinLevelThree.forEach { puzzle ->
            val recorder = RecordingDeductionListener()

            val result = SudokuSolver(recorder).propagate(puzzle.field())

            assertThat(result)
                .describedAs(puzzle.givens)
                .isInstanceOf(Solved::class.java)
            assertThat(result.field).isEqualTo(puzzle.solved())
            assertThat(recorder.guesses).isZero()
        }
    }

    @Test
    fun puzzlesNeedingChainsStillStallAndFallToSearch() {
        beyondLevelThree.forEach { puzzle ->
            assertThat(SudokuSolver().propagate(puzzle.field()))
                .describedAs(puzzle.givens)
                .isInstanceOf(Stalled::class.java)

            val result = SudokuSolver().solve(puzzle.field())

            assertThat(result).isInstanceOf(Solved::class.java)
            assertThat(result.field).isEqualTo(puzzle.solved())
        }
    }

    @Test
    fun everyImplementedTechniqueEarnsItsPlaceOnSomePuzzle() {
        val used = Puzzles.all.flatMapTo(mutableSetOf()) { puzzle ->
            RecordingDeductionListener()
                .also { SudokuSolver(it).solve(puzzle.field()) }
                .techniquesUsed()
        }

        assertThat(used).containsAll(
            listOf(
                Technique.FULL_HOUSE,
                Technique.NAKED_SINGLE,
                Technique.HIDDEN_SINGLE,
                Technique.PEER_ELIMINATION,
                Technique.POINTING,
                Technique.CLAIMING,
                Technique.NAKED_SUBSET,
                Technique.HIDDEN_SUBSET
            )
        )
    }

    @Test
    fun eachStepOfTheChainIsOneTechniqueAndTheyRunCheapestFirst() {
        // The order the solver applies them in, and the level each belongs to.
        val chain = SudokuSolver().processors.map { it::class.simpleName }

        assertThat(chain).containsExactly(
            "FullHouseSolver",              // level 1
            "HouseCandidateEliminator",     // level 1 - keeps candidates honest for everything below
            "SingleCandidateMarker",        // level 1
            "SolveSingleCandidateTransformer", // level 1
            "PointingEliminator",           // level 2
            "ClaimingEliminator",           // level 2
            "NakedSubsetEliminator",        // level 3
            "HiddenSubsetEliminator"        // level 3
        )
    }
}
