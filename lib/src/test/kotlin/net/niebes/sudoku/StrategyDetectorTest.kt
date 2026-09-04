package net.niebes.sudoku

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class StrategyDetectorTest {

    private val corpus = Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all

    private fun stalledGrids() = corpus
        .map { it to SudokuSolver().propagate(it.field()) }
        .filter { (_, result) -> result !is Solved }
        .map { (puzzle, result) -> puzzle to result.field }

    @Test
    fun reportsWhatWouldFireWhereTheSolverIsStuck() {
        val stalled = stalledGrids()
        println("grids where the chain stalls: ${stalled.size}")

        val totals = mutableMapOf<String, Int>()
        stalled.forEach { (puzzle, grid) ->
            val findings = StrategyDetector.detect(grid)
            findings.forEach { totals.merge(it.strategy, it.eliminations, Int::plus) }
            println("  ${puzzle.givens.take(20)}...  " +
                findings.joinToString("  ") { "${it.strategy}=${it.eliminations}" })
        }
        println("totals: " + totals.entries.sortedByDescending { it.value }
            .joinToString(", ") { "${it.key}=${it.value}" })

        assertThat(stalled).isNotEmpty()
    }

    @Test
    fun theGroupedChainDetectorFindsAnOrdinaryTurbotFish() {
        // A skyscraper is the ungrouped case of the pattern, so a detector that cannot see one is
        // broken - and a broken detector reporting zero looks exactly like good news.
        val skyscraper = field {
            (0..8).forEach { column ->
                if (column == 1 || column == 7) candidates(1, column, 9, 8) else candidates(1, column, 1, 2, 3)
                if (column == 1 || column == 8) candidates(4, column, 9, 8) else candidates(4, column, 1, 2, 3)
            }
        }

        assertThat(StrategyDetector.detect(skyscraper).single { it.strategy == "grouped chains" }.eliminations)
            .isGreaterThanOrEqualTo(4)
    }

    @Test
    fun theFrankenFishDetectorFindsAFishBuiltOnASegment() {
        // Row 0 and segment 4 both need a 7, and both can only take it in columns 1 and 4.
        val franken = field {
            (0..8).forEach { column ->
                if (column == 1 || column == 4) candidates(0, column, 7, 8) else candidates(0, column, 1, 2, 3)
            }
            (3..5).forEach { row ->
                (3..5).forEach { column ->
                    if (column == 4 && row != 4) candidates(row, column, 7, 8)
                    else candidates(row, column, 1, 2, 3)
                }
            }
        }

        assertThat(StrategyDetector.detect(franken).single { it.strategy == "franken fish" }.eliminations)
            .isPositive()
    }

    @Test
    fun findsNothingLeftToDoOnAGridTheChainFinishes() {
        val solved = SudokuSolver().propagate(Puzzles.singlesOnly.field()).field

        assertThat(StrategyDetector.detect(solved).sumOf { it.eliminations }).isZero()
    }
}
