package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HiddenSubsetEliminatorTest {

    @Test
    fun stripsTheExtrasFromTheOnlyTwoCellsTwoValuesCanReach() {
        // 1 and 3 appear nowhere else in row 0, so those two cells hold them and nothing else.
        val after = HiddenSubsetEliminator().process(field {
            candidates(0, 0, 1, 3, 5, 8)
            candidates(0, 1, 1, 3, 5, 8)
            rowCandidates(0, 2..8, 2, 4, 5, 6, 7, 8, 9)
        })

        assertThat(after.candidatesAt(0, 0)).isEqualTo(Candidates.of(1, 3))
        assertThat(after.candidatesAt(0, 1)).isEqualTo(Candidates.of(1, 3))
    }

    @Test
    fun findsATripleEvenWhenNoCellHoldsAllThreeValues() {
        val after = HiddenSubsetEliminator().process(field {
            candidates(0, 0, 1, 3, 5, 8)
            candidates(0, 1, 3, 7, 5, 8)
            candidates(0, 2, 1, 7, 9)
            rowCandidates(0, 3..8, 2, 4, 5, 6, 8, 9)
        })

        assertThat(after.candidatesAt(0, 0)).isEqualTo(Candidates.of(1, 3))
        assertThat(after.candidatesAt(0, 1)).isEqualTo(Candidates.of(3, 7))
        assertThat(after.candidatesAt(0, 2)).isEqualTo(Candidates.of(1, 7))
    }

    @Test
    fun leavesEveryCellOutsideTheSubsetAlone() {
        val before = field {
            candidates(0, 0, 1, 3, 5, 8)
            candidates(0, 1, 1, 3, 5, 8)
            rowCandidates(0, 2..8, 2, 4, 5, 6, 7, 8, 9)
        }

        val after = HiddenSubsetEliminator().process(before)

        (2..8).forEach { column ->
            assertThat(after.candidatesAt(0, column))
                .describedAs("column $column")
                .isEqualTo(before.candidatesAt(0, column))
        }
    }

    @Test
    fun staysSilentWhenOneOfTheValuesFitsInAThirdCell() {
        val eliminated = HiddenSubsetEliminator(2..2).eliminations(field {
            candidates(0, 0, 1, 3, 5, 8)
            candidates(0, 1, 1, 3, 5, 8)
            candidates(0, 2, 1, 5, 8)      // 1 escapes the pair
            rowCandidates(0, 3..8, 2, 4, 6, 7, 9)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenOneOfTheValuesIsAlreadyPlacedInTheHouse() {
        // 1 sits at (0,4), but (0,0) and (0,1) still list it because peer elimination has not caught
        // up. Counting those stale candidates pairs 1 with 6 and strips 3 and 5 from both cells -
        // unsound, since with 1 settled elsewhere one of them has to be a 3 or a 5.
        val eliminated = HiddenSubsetEliminator().eliminations(field {
            solved(0, 4, 1)
            candidates(0, 0, 1, 6, 3, 5)
            candidates(0, 1, 1, 6, 3, 5)
            candidates(0, 2, 3, 5, 8, 9)
            candidates(0, 3, 5, 8, 9, 2)
            candidates(0, 5, 3, 8, 9, 2)
            candidates(0, 6, 3, 5, 9, 2)
            candidates(0, 7, 3, 5, 8, 2)
            candidates(0, 8, 3, 5, 8, 9)
        })

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun reportsWhatItRemovedAsAHiddenSubset() {
        val recorder = RecordingDeductionListener()

        HiddenSubsetEliminator().process(field {
            candidates(0, 0, 1, 3, 5, 8)
            candidates(0, 1, 1, 3, 5, 8)
            rowCandidates(0, 2..8, 2, 4, 5, 6, 7, 8, 9)
        }, recorder)

        assertThat(recorder.eliminations).containsExactlyInAnyOrder(
            Elimination(Technique.HIDDEN_SUBSET, CellPosition(0, 0), Candidates.of(5, 8)),
            Elimination(Technique.HIDDEN_SUBSET, CellPosition(0, 1), Candidates.of(5, 8))
        )
    }

    @Test
    fun isTheDualOfANakedSubsetAndReachesTheSameConclusionFromTheOtherSide() {
        // Three cells of this row hold {2,7,9}; the other six between them hold the remaining six
        // values. A naked triple sees the first fact, a hidden six-subset would see the second.
        val grid = field {
            candidates(0, 0, 2, 7)
            candidates(0, 3, 2, 9)
            candidates(0, 6, 7, 9)
            rowCandidates(0, 1..2, 1, 2, 3, 4, 7, 9)
        }

        val nakedResult = NakedSubsetEliminator().process(grid)

        assertThat(nakedResult.candidatesAt(0, 1)).isEqualTo(Candidates.of(1, 3, 4))
        assertThat(nakedResult.candidatesAt(0, 2)).isEqualTo(Candidates.of(1, 3, 4))
    }

    @Test
    fun contributesToRealPuzzles() {
        val recorder = RecordingDeductionListener()

        SudokuSolver(recorder).solve(Puzzles.needsSubsets.field())

        assertThat(recorder.techniquesUsed()).contains(Technique.HIDDEN_SUBSET)
    }
}
