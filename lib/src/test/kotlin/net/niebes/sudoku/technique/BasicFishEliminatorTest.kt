package net.niebes.sudoku.technique

import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.HardPuzzles
import net.niebes.sudoku.Puzzles
import net.niebes.sudoku.SudokuSolver
import net.niebes.sudoku.candidatesAt
import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.field
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BasicFishEliminatorTest {

    private fun Field.fishOn(value: Int): List<CellPosition> =
        BasicFishEliminator().eliminations(this)
            .filter { it.values == Candidates.of(value) }
            .map { it.at }

    /** Rows 1 and 4 can take a 4 only in columns 2 and 6. */
    private fun xWingOn4() = field {
        listOf(1, 4).forEach { row ->
            (0..8).forEach { column ->
                if (column == 2 || column == 6) candidates(row, column, 4, 5)
                else candidates(row, column, 1, 2, 3)
            }
        }
    }

    @Test
    fun xWingClearsBothColumnsOutsideTheTwoBaseRows() {
        val eliminated = xWingOn4().fishOn(4)

        assertThat(eliminated).containsExactlyInAnyOrderElementsOf(
            listOf(0, 2, 3, 5, 6, 7, 8).flatMap { row ->
                listOf(CellPosition(row, 2), CellPosition(row, 6))
            }
        )
    }

    @Test
    fun xWingWorksWithColumnsAsTheBaseToo() {
        val eliminated = field {
            listOf(1, 4).forEach { column ->
                (0..8).forEach { row ->
                    if (row == 2 || row == 6) candidates(row, column, 4, 5)
                    else candidates(row, column, 1, 2, 3)
                }
            }
        }.fishOn(4)

        assertThat(eliminated).containsExactlyInAnyOrderElementsOf(
            listOf(0, 2, 3, 5, 6, 7, 8).flatMap { column ->
                listOf(CellPosition(2, column), CellPosition(6, column))
            }
        )
    }

    @Test
    fun swordfishNeedsOnlyTwoPlacesInEachBaseRow() {
        // Rows 0, 2 and 4 between them confine 7 to columns 1, 4 and 7 - no row uses all three.
        val eliminated = field {
            mapOf(0 to setOf(1, 4), 2 to setOf(1, 7), 4 to setOf(4, 7)).forEach { (row, columns) ->
                (0..8).forEach { column ->
                    if (column in columns) candidates(row, column, 7, 8)
                    else candidates(row, column, 1, 2, 3)
                }
            }
        }.fishOn(7)

        assertThat(eliminated).containsExactlyInAnyOrderElementsOf(
            listOf(1, 3, 5, 6, 7, 8).flatMap { row ->
                listOf(CellPosition(row, 1), CellPosition(row, 4), CellPosition(row, 7))
            }
        )
    }

    @Test
    fun staysSilentWhenABaseRowCanTakeTheValueOutsideTheCoverColumns() {
        val eliminated = field {
            listOf(1, 4).forEach { row ->
                (0..8).forEach { column ->
                    if (column == 2 || column == 6) candidates(row, column, 4, 5)
                    else candidates(row, column, 1, 2, 3)
                }
            }
            candidates(1, 0, 1, 2, 4)   // 4 escapes the two columns
        }.fishOn(4)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheValueIsAlreadyPlacedInABaseRow() {
        // The pattern cells still list 4 because peer elimination has not caught up. Counting them
        // builds a fish out of a row that has already had its 4.
        val eliminated = xWingOn4().let { grid ->
            field {
                grid.cells.forEach { cell ->
                    candidates(cell.position.row, cell.position.column, *candidatesOf(grid, cell.position))
                }
                solved(1, 0, 4)
            }
        }.fishOn(4)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheValueIsAlreadyPlacedInACoverColumn() {
        val eliminated = xWingOn4().let { grid ->
            field {
                grid.cells.forEach { cell ->
                    candidates(cell.position.row, cell.position.column, *candidatesOf(grid, cell.position))
                }
                solved(7, 2, 4)
            }
        }.fishOn(4)

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun neverEliminatesFromTheBaseRowsThemselves() {
        val after = BasicFishEliminator().process(xWingOn4())

        assertThat(after.candidatesAt(1, 2)).isEqualTo(Candidates.of(4, 5))
        assertThat(after.candidatesAt(4, 6)).isEqualTo(Candidates.of(4, 5))
    }

    @Test
    fun reportsWhatItRemovedAsABasicFish() {
        val recorder = RecordingDeductionListener()

        BasicFishEliminator().process(xWingOn4(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.BASIC_FISH }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            BasicFishEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }

    private fun candidatesOf(grid: Field, position: CellPosition): IntArray =
        grid.candidatesAt(position.row, position.column).values.toIntArray()
}
