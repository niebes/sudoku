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

internal class NakedSubsetEliminatorTest {

    private fun Field.subsetEliminations(): Map<CellPosition, Candidates> =
        NakedSubsetEliminator().eliminations(this)
            .groupBy { it.at }
            .mapValues { (_, found) -> found.fold(Candidates.NONE) { all, it -> all or it.values } }

    @Test
    fun clearsTheRestOfTheHouseWhenTwoCellsShareTwoCandidates() {
        // Columns 0 and 4 sit in different segments, so row 0 is the only house they share.
        val eliminated = field {
            candidates(0, 0, 1, 2)
            candidates(0, 4, 1, 2)
        }.subsetEliminations()

        assertThat(eliminated.keys).containsExactlyInAnyOrderElementsOf(
            listOf(1, 2, 3, 5, 6, 7, 8).map { CellPosition(0, it) }
        )
        assertThat(eliminated.values).allMatch { it == Candidates.of(1, 2) }
    }

    @Test
    fun findsATripleEvenWhenNoCellHoldsAllThreeValues() {
        val eliminated = field {
            candidates(0, 0, 2, 7)
            candidates(0, 3, 2, 9)
            candidates(0, 6, 7, 9)
        }.subsetEliminations()

        assertThat(eliminated.keys).containsExactlyInAnyOrderElementsOf(
            listOf(1, 2, 4, 5, 7, 8).map { CellPosition(0, it) }
        )
        assertThat(eliminated.values).allMatch { it == Candidates.of(2, 7, 9) }
    }

    @Test
    fun findsAQuad() {
        val eliminated = field {
            candidates(0, 0, 1, 2)
            candidates(0, 1, 2, 3)
            candidates(0, 3, 3, 4)
            candidates(0, 6, 4, 1)
        }.subsetEliminations()

        assertThat(eliminated.keys).containsExactlyInAnyOrderElementsOf(
            listOf(2, 4, 5, 7, 8).map { CellPosition(0, it) }
        )
        assertThat(eliminated.values).allMatch { it == Candidates.of(1, 2, 3, 4) }
    }

    @Test
    fun clearsBothHousesWhenTheSubsetSitsInARowAndASegmentAtOnce() {
        // A locked pair: columns 0 and 1 of row 0 are also both inside segment 0.
        val eliminated = field {
            candidates(0, 0, 1, 2)
            candidates(0, 1, 1, 2)
        }.subsetEliminations()

        assertThat(eliminated.keys).containsExactlyInAnyOrderElementsOf(
            (2..8).map { CellPosition(0, it) } +
                listOf(1, 2).flatMap { row -> (0..2).map { CellPosition(row, it) } }
        )
    }

    @Test
    fun staysSilentWhenTheCellsSpreadOverMoreValuesThanCells() {
        val eliminated = field {
            candidates(0, 0, 1, 2)
            candidates(0, 4, 1, 3)
        }.subsetEliminations()

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun staysSilentWhenTheSubsetCoversEveryOpenCellOfTheHouse() {
        // Every unsolved cell of row 0 is part of the pair, so there is nothing left to clear.
        // The two sit in different segments, so row 0 is the only house they share.
        val eliminated = field {
            listOf(0, 1, 2, 4, 5, 6, 8).forEachIndexed { value, column -> solved(0, column, value + 1) }
            candidates(0, 3, 8, 9)
            candidates(0, 7, 8, 9)
        }.subsetEliminations()

        assertThat(eliminated).isEmpty()
    }

    @Test
    fun neverEliminatesFromTheSubsetItself() {
        val after = NakedSubsetEliminator().process(field {
            candidates(0, 0, 1, 2)
            candidates(0, 4, 1, 2)
        })

        assertThat(after.candidatesAt(0, 0)).isEqualTo(Candidates.of(1, 2))
        assertThat(after.candidatesAt(0, 4)).isEqualTo(Candidates.of(1, 2))
    }

    @Test
    fun reportsWhatItRemovedAsANakedSubset() {
        val recorder = RecordingDeductionListener()

        NakedSubsetEliminator().process(field {
            candidates(0, 0, 1, 2)
            candidates(0, 4, 1, 2)
        }, recorder)

        assertThat(recorder.eliminations).isNotEmpty()
        assertThat(recorder.eliminations).allMatch { it.technique == Technique.NAKED_SUBSET }
        // The pair itself is the evidence.
        assertThat(recorder.eliminations).allMatch {
            it.because == listOf(CellPosition(0, 0), CellPosition(0, 4))
        }
    }

    @Test
    fun isTheCheapestAvailableTechniqueOnSomeRealPuzzle() {
        // Naming one puzzle here would be brittle: the solver stops at the first technique that
        // makes progress, so whether this one is ever reached depends on what the cheaper ones
        // leave behind. Asking the whole corpus keeps the point - it still pays its way - without
        // pinning it to a puzzle that a cheaper technique may start finishing tomorrow.
        val used = (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).flatMapTo(mutableSetOf()) {
            RecordingDeductionListener().also { recorder -> SudokuSolver(recorder).solve(it.field()) }
                .techniquesUsed()
        }

        assertThat(used).contains(Technique.NAKED_SUBSET)
    }
}
