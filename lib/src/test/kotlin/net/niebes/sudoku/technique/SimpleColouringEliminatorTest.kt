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
import net.niebes.sudoku.model.Field
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SimpleColouringEliminatorTest {

    private fun Field.colouringOn(value: Int): List<CellPosition> =
        SimpleColouringEliminator().eliminations(this)
            .filter { it.values == Candidates.of(value) }
            .map { it.at }

    /**
     * Three strong links on 7 chain (0,0) - (0,4) - (5,4) - (3,3), so the colours alternate
     * A B A B. Whichever colour is true, (3,0) sees one of them: it shares column 0 with (0,0) and
     * row 3 with (3,3).
     */
    private fun colourTrapOn7() = field {
        (0..8).forEach { column ->
            if (column == 0 || column == 4) candidates(0, column, 7, 8) else candidates(0, column, 1, 2, 3)
        }
        (0..8).forEach { row ->
            if (row == 0 || row == 5) candidates(row, 4, 7, 8) else candidates(row, 4, 1, 2, 3)
        }
        (3..5).forEach { row ->
            (3..5).forEach { column ->
                val onTheChain = (row == 5 && column == 4) || (row == 3 && column == 3)
                if (onTheChain) candidates(row, column, 7, 8) else candidates(row, column, 1, 2, 3)
            }
        }
    }

    /**
     * (0,0) - (0,2) - (2,2) chain, so (0,0) and (2,2) take the same colour - and they share
     * segment 0. Two cells of one colour in one house means that colour is false throughout.
     */
    private fun colourWrapOn7() = field {
        (0..8).forEach { column ->
            if (column == 0 || column == 2) candidates(0, column, 7, 8) else candidates(0, column, 1, 2, 3)
        }
        (0..8).forEach { row ->
            if (row == 0 || row == 2) candidates(row, 2, 7, 8) else candidates(row, 2, 1, 2, 3)
        }
    }

    @Test
    fun colourTrapClearsACellThatSeesBothColours() {
        assertThat(colourTrapOn7().colouringOn(7)).containsExactly(CellPosition(3, 0))
    }

    @Test
    fun colourWrapClearsTheColourThatRepeatsInAHouse() {
        // The two same-coloured cells are themselves eliminated, which only the wrap rule does.
        assertThat(colourWrapOn7().colouringOn(7))
            .contains(CellPosition(0, 0), CellPosition(2, 2))
            .doesNotContain(CellPosition(0, 2))
    }

    @Test
    fun leavesTheChainAloneWhenNeitherRuleFires() {
        val after = SimpleColouringEliminator().process(colourTrapOn7())

        listOf(CellPosition(0, 0), CellPosition(0, 4), CellPosition(5, 4), CellPosition(3, 3))
            .forEach { assertThat(after.cellAt(it).couldBe(7)).describedAs("$it").isTrue() }
    }

    @Test
    fun staysSilentWithoutAChainOfStrongLinks() {
        val eliminated = field {
            (0..8).forEach { column ->
                if (column == 0 || column == 4) candidates(0, column, 7, 8) else candidates(0, column, 1, 2, 3)
            }
        }.colouringOn(7)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun reportsWhatItRemovedAsSimpleColouring() {
        val recorder = RecordingDeductionListener()

        SimpleColouringEliminator().process(colourTrapOn7(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.SIMPLE_COLOURING }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            SimpleColouringEliminator().eliminations(settled).forEach { elimination: Elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
