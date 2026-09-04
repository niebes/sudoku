package net.niebes.sudoku

import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.technique.ClaimingEliminator
import net.niebes.sudoku.technique.EliminationTechnique
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
        assertThat(withoutGuessing).isGreaterThanOrEqualTo(24)
    }

    @Test
    fun searchStillFinishesWhatPropagationCannot() {
        (GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val result = SudokuSolver().solve(puzzle.field())

            assertThat(result).describedAs(puzzle.givens).isInstanceOf(Solved::class.java)
            assertThat(result.field).isEqualTo(puzzle.solved())
        }
    }

    @Test
    fun aPuzzleIsCreditedOnlyToTheCheapestTechniqueThatCouldHaveSolvedIt() {
        // The solver stops at the first technique that makes progress, so a puzzle that singles can
        // finish is never credited to anything above them. Running the whole chain on every pass
        // used to hand a jellyfish the credit for work a naked single was about to do anyway.
        val recorder = RecordingDeductionListener()

        SudokuSolver(recorder).propagate(Puzzles.singlesOnly.field())

        assertThat(recorder.techniquesUsed()).containsExactlyInAnyOrder(
            Technique.PEER_ELIMINATION,
            Technique.FULL_HOUSE,
            Technique.NAKED_SINGLE,
            Technique.HIDDEN_SINGLE
        )
    }

    @Test
    fun everyImplementedTechniqueEarnsItsPlaceOnSomePuzzle() {
        val used = (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).flatMapTo(mutableSetOf()) { puzzle ->
            RecordingDeductionListener()
                .also { SudokuSolver(it).solve(puzzle.field()) }
                .techniquesUsed()
        }

        // Every technique the default chain runs has to pay its way; one that stops firing has
        // become dead code and this says so. The enum may name more than the chain runs - a
        // technique can be implemented, sound and tested without being worth its place.
        val inTheChain = SudokuSolver().processors.mapNotNull { (it as? EliminationTechnique)?.technique }
        assertThat(used).containsAll(inTheChain)
    }

    @Test
    fun eachStepOfTheChainIsOneTechniqueAndTheyRunCheapestFirst() {
        // The order the solver applies them in, and the level each belongs to.
        val chain = SudokuSolver().processors.map { it::class.simpleName }

        assertThat(chain).containsExactly(
            "FullHouseSolver",              // level 1
            "HouseCandidateEliminator",     // level 1 - keeps candidates honest for everything below
            "SolveSingleCandidateTransformer", // level 1
            "SingleCandidateMarker",        // level 1
            "PointingEliminator",           // level 2
            "ClaimingEliminator",           // level 2
            "NakedSubsetEliminator",        // level 3
            "HiddenSubsetEliminator",       // level 3
            "BasicFishEliminator",          // level 4
            "TurbotFishEliminator",         // level 5
            "XyWingEliminator",             // level 5
            "XyzWingEliminator",            // level 5
            "WWingEliminator",              // level 5
            "EmptyRectangleEliminator",     // level 5
            "AicEliminator",                // level 7
            "FinnedFishEliminator"          // level 8
        )
    }
}
