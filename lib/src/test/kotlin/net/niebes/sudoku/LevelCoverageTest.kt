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

    @Test
    fun everyNamedPuzzleNowSolvesWithoutGuessing() {
        // All six, including the 18-clue one and the 17-clue one at the proven minimum. Both of
        // those needed search until fish and turbot fish landed.
        Puzzles.all.forEach { puzzle ->
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
    fun propagationAloneCarriesMostOfTheGeneratedCorpus() {
        val withoutGuessing = GeneratedPuzzles.all.count {
            SudokuSolver().propagate(it.field()) is Solved
        }

        // A floor, not a target: a new technique should be free to raise it.
        assertThat(withoutGuessing).isGreaterThanOrEqualTo(20)
    }

    @Test
    fun searchStillFinishesWhatPropagationCannot() {
        GeneratedPuzzles.all.forEach { puzzle ->
            val result = SudokuSolver().solve(puzzle.field())

            assertThat(result).describedAs(puzzle.givens).isInstanceOf(Solved::class.java)
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
                Technique.HIDDEN_SUBSET,
                Technique.BASIC_FISH,
                Technique.TURBOT_FISH
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
            "HiddenSubsetEliminator",       // level 3
            "BasicFishEliminator",          // level 4
            "TurbotFishEliminator"          // level 5
        )
    }
}
