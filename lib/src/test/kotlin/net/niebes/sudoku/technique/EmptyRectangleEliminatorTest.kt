package net.niebes.sudoku.technique

import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.HardPuzzles
import net.niebes.sudoku.Puzzles
import net.niebes.sudoku.SudokuSolver
import net.niebes.sudoku.candidatesAt
import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.field
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EmptyRectangleEliminatorTest {

    /**
     * In segment 4 (rows 3..5, columns 3..5) the 4s fit inside row 4 plus column 4, leaving the
     * 2x2 corner empty. Column 7 holds 4 only at rows 1 and 4, and its row-4 end lies on the
     * empty rectangle's row - which pins the elimination to (1,4).
     */
    private fun emptyRectangleOn4() = field {
        (3..5).forEach { row ->
            (3..5).forEach { column ->
                val onTheCross = (row == 4 && column != 4) || (row == 3 && column == 4)
                if (onTheCross) candidates(row, column, 4, 6) else candidates(row, column, 1, 2, 7)
            }
        }
        (0..8).forEach { row ->
            if (row == 1 || row == 4) candidates(row, 7, 4, 8) else candidates(row, 7, 1, 2, 9)
        }
    }

    @Test
    fun clearsWhereTheStrongLinkAndTheRectangleColumnMeet() {
        val eliminated = EmptyRectangleEliminator().eliminations(emptyRectangleOn4())

        // The segment's places for the value, then the strong link that pins them down.
        assertThat(eliminated).containsExactly(
            Elimination(Technique.EMPTY_RECTANGLE, CellPosition(1, 4), Candidates.of(4),
                because = listOf(
                    CellPosition(3, 4), CellPosition(4, 3), CellPosition(4, 5),
                    CellPosition(4, 7), CellPosition(1, 7)
                ))
        )
    }

    @Test
    fun staysSilentWhenTheSegmentsCandidatesLieInASingleLine() {
        // All in one row is locked candidates, which pointing already handles - there is no
        // rectangle here and no second arm to reason with.
        val eliminated = EmptyRectangleEliminator().eliminations(field {
            (3..5).forEach { row ->
                (3..5).forEach { column ->
                    if (row == 4 && column != 4) candidates(row, column, 4, 6)
                    else candidates(row, column, 1, 2, 7)
                }
            }
            (0..8).forEach { row ->
                if (row == 1 || row == 4) candidates(row, 7, 4, 8) else candidates(row, 7, 1, 2, 9)
            }
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheSegmentsCandidatesDoNotFitOneRowAndOneColumn() {
        val eliminated = EmptyRectangleEliminator().eliminations(field {
            (3..5).forEach { row ->
                (3..5).forEach { column ->
                    // A diagonal reaches into the corner the rectangle has to leave empty.
                    val onTheCross = (row == 4 && column != 4) || (row == 3 && column == 4) ||
                        (row == 5 && column == 5)
                    if (onTheCross) candidates(row, column, 4, 6) else candidates(row, column, 1, 2, 7)
                }
            }
            (0..8).forEach { row ->
                if (row == 1 || row == 4) candidates(row, 7, 4, 8) else candidates(row, 7, 1, 2, 9)
            }
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWithoutAStrongLinkToWorkWith() {
        val eliminated = EmptyRectangleEliminator().eliminations(field {
            (3..5).forEach { row ->
                (3..5).forEach { column ->
                    val onTheCross = (row == 4 && column != 4) || (row == 3 && column == 4)
                    if (onTheCross) candidates(row, column, 4, 6) else candidates(row, column, 1, 2, 7)
                }
            }
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun leavesTheRectangleAndTheLinkAlone() {
        val after = EmptyRectangleEliminator().process(emptyRectangleOn4())

        assertThat(after.candidatesAt(4, 3)).isEqualTo(Candidates.of(4, 6))
        assertThat(after.candidatesAt(3, 4)).isEqualTo(Candidates.of(4, 6))
        assertThat(after.candidatesAt(1, 7)).isEqualTo(Candidates.of(4, 8))
        assertThat(after.candidatesAt(4, 7)).isEqualTo(Candidates.of(4, 8))
    }

    @Test
    fun reportsWhatItRemovedAsAnEmptyRectangle() {
        val recorder = RecordingDeductionListener()

        EmptyRectangleEliminator().process(emptyRectangleOn4(), recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.EMPTY_RECTANGLE }
    }

    @Test
    fun neverRemovesAValueTheSolutionNeeds() {
        (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).forEach { puzzle ->
            val settled = SudokuSolver(listOf(HouseCandidateEliminator())).propagate(puzzle.field()).field

            EmptyRectangleEliminator().eliminations(settled).forEach { elimination ->
                assertThat(elimination.values.contains(puzzle.valueAt(elimination.at.index)))
                    .describedAs("${elimination.at} in ${puzzle.givens}")
                    .isFalse()
            }
        }
    }
}
