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

internal class AicEliminatorTest {

    @Test
    fun endsOnTheSameValueClearThatValueFromWhatSeesBoth() {
        // An XY-chain: {4,9} at (0,0), {2,9} at (0,6), {2,4} at (4,6). Following it strong-weak-
        // strong-weak-strong proves 4 sits at (0,0) or (4,6), so (4,0) - which sees both - loses it.
        val eliminated = AicEliminator().eliminations(field {
            candidates(0, 0, 4, 9)
            candidates(0, 6, 2, 9)
            candidates(4, 6, 2, 4)
        })

        assertThat(eliminated).containsExactly(
            Elimination(Technique.AIC, CellPosition(4, 0), Candidates.of(4))
        )
    }

    @Test
    fun endsInTheSameCellLeaveThatCellHoldingOneOfThem() {
        // The chain starts at 1 in (4,4) and ends at 2 in (4,4), so that cell is a 1 or a 2 and
        // its third candidate goes.
        val eliminated = AicEliminator().eliminations(field {
            candidates(4, 4, 1, 2, 3)
            candidates(0, 0, 1, 2)
            (0..8).forEach { column -> if (column != 0 && column != 4) candidates(4, column, 5, 6, 7) }
            candidates(4, 0, 1, 8)
            (0..8).forEach { row -> if (row != 0 && row != 4) candidates(row, 4, 5, 6, 7) }
            candidates(0, 4, 2, 9)
        })

        assertThat(eliminated).contains(
            Elimination(Technique.AIC, CellPosition(4, 4), Candidates.of(3))
        )
    }

    @Test
    fun subsumesTurbotFish() {
        // The skyscraper from the turbot fish tests is a three-link chain, so this finds it too.
        val skyscraper = field {
            (0..8).forEach { column ->
                if (column == 1 || column == 7) candidates(1, column, 9, 8) else candidates(1, column, 1, 2, 3)
                if (column == 1 || column == 8) candidates(4, column, 9, 8) else candidates(4, column, 1, 2, 3)
            }
        }

        val byAic = AicEliminator().eliminations(skyscraper).filter { it.values == Candidates.of(9) }

        assertThat(byAic.map { it.at }).contains(
            CellPosition(0, 8), CellPosition(2, 8), CellPosition(3, 7), CellPosition(5, 7)
        )
    }

    @Test
    fun staysSilentWhenNoChainAlternates() {
        // One bivalue cell on its own: a chain needs a strong link out of somewhere to continue.
        val eliminated = AicEliminator().eliminations(field { candidates(0, 0, 4, 9) })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun reportsWhatItRemovedAsAnAic() {
        val recorder = RecordingDeductionListener()

        AicEliminator().process(field {
            candidates(0, 0, 4, 9)
            candidates(0, 6, 2, 9)
            candidates(4, 6, 2, 4)
        }, recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.AIC }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            AicEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
