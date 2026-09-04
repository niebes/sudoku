package net.niebes.sudoku.api

import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.HardPuzzles
import net.niebes.sudoku.Puzzles
import net.niebes.sudoku.SudokuSolver
import net.niebes.sudoku.deduction.SuccessfulLineRecordingListener
import net.niebes.sudoku.io.CompactFieldParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TraceReplayerTest {

    private fun replayOf(puzzle: net.niebes.sudoku.Puzzle): Replay {
        val trace = SuccessfulLineRecordingListener()
        SudokuSolver(trace).solve(CompactFieldParser().parse(puzzle.givens))
        return TraceReplayer().replay(CompactFieldParser().parse(puzzle.givens), trace.deductions)
    }

    /** The claim the whole site rests on, measured over every corpus puzzle rather than asserted. */
    @Test
    fun replayingTheStepsReproducesTheSolutionOnEveryCorpusPuzzle() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            assertThat(replayOf(puzzle).grid)
                .describedAs(puzzle.givens)
                .isEqualTo(puzzle.solution)
        }
    }

    /**
     * The solver reports a hidden single when it narrows the cell and a naked single when it later
     * solidifies it - one pedagogical move told twice. The replay must show it once: a placement on
     * a cell the replay has already filled folds away silently.
     */
    @Test
    fun showsOnePlacementPerCell() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val placements = replayOf(puzzle).steps.filter { it.kind != StepKind.ELIMINATION }
            assertThat(placements.map { it.at })
                .describedAs(puzzle.givens)
                .doesNotHaveDuplicates()
        }
    }

    @Test
    fun presentsAssumptionsAsGuessStepsRatherThanDeductions() {
        val needingSearch = (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).mapNotNull { puzzle ->
            replayOf(puzzle).takeIf { replay -> replay.steps.any { it.kind == StepKind.GUESS } }
        }

        // The corpus is known to hold puzzles the technique chain cannot finish; if this ever
        // becomes empty the chain has swallowed the corpus and the corpus needs harder puzzles.
        assertThat(needingSearch).isNotEmpty()
        needingSearch.flatMap { it.steps }.filter { it.kind == StepKind.GUESS }.forEach { guess ->
            assertThat(guess.technique).isEqualTo("GUESS")
            assertThat(guess.because).isEmpty()
        }
    }

    @Test
    fun everyStepCarriesAnExplanationInGridTerms() {
        val steps = replayOf(Puzzles.needsSubsets).steps

        assertThat(steps).isNotEmpty()
        assertThat(steps).allMatch { it.explanation.isNotBlank() }
        // Spot-check the shape on the cheapest and most frequent technique: a peer elimination
        // names the values it removes.
        val peer = steps.first { it.technique == "PEER_ELIMINATION" }
        assertThat(peer.explanation).contains(peer.values.first().toString())
    }
}
