package net.niebes.sudoku

import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.UnsolvedCell
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EliminationTechniqueTest {

    private fun technique(vararg found: Elimination) = object : EliminationTechnique {
        override val technique = Technique.PEER_ELIMINATION
        override fun eliminations(field: Field) = found.toList()
    }

    @Test
    fun appliesEveryEliminationInTheBatch() {
        val before = field { candidates(0, 0, 1, 2, 3) }

        val after = technique(
            Elimination(Technique.PEER_ELIMINATION, CellPosition(0, 0), Candidates.of(1)),
            Elimination(Technique.PEER_ELIMINATION, CellPosition(0, 0), Candidates.of(3))
        ).process(before)

        assertThat(after.candidatesAt(0, 0)).isEqualTo(Candidates.of(2))
    }

    @Test
    fun reportsOnlyWhatItActuallyRemoved() {
        val recorder = RecordingDeductionListener()
        val before = field { candidates(0, 0, 1, 2) }

        // 1 is present and goes; 9 was never a candidate here, so it is not reported.
        technique(Elimination(Technique.PEER_ELIMINATION, CellPosition(0, 0), Candidates.of(1, 9)))
            .process(before, recorder)

        assertThat(recorder.eliminations).singleElement()
            .isEqualTo(Elimination(Technique.PEER_ELIMINATION, CellPosition(0, 0), Candidates.of(1)))
    }

    @Test
    fun staysSilentAndReturnsTheSameFieldWhenNothingApplies() {
        val recorder = RecordingDeductionListener()
        val before = field { candidates(0, 0, 1, 2) }

        val after = technique(Elimination(Technique.PEER_ELIMINATION, CellPosition(0, 0), Candidates.of(9)))
            .process(before, recorder)

        assertThat(after).isEqualTo(before)
        assertThat(recorder.deductions).isEmpty()
    }

    @Test
    fun leavesSolvedCellsAlone() {
        val before = field { solved(0, 0, 4) }

        val after = technique(Elimination(Technique.PEER_ELIMINATION, CellPosition(0, 0), Candidates.of(4)))
            .process(before)

        assertThat(after.valueAt(0, 0)).isEqualTo(4)
    }

    @Test
    fun computesEveryEliminationBeforeApplyingAny() {
        // The technique is handed the field once and must not see its own partial results: both
        // cells are inspected against the original candidates, so both eliminations survive.
        val seen = mutableListOf<Candidates>()
        val recording = object : EliminationTechnique {
            override val technique = Technique.PEER_ELIMINATION
            override fun eliminations(field: Field): List<Elimination> =
                field.unsolved().filter { it.candidates.size == 2 }.map { cell ->
                    seen += cell.candidates
                    Elimination(technique, cell.position, Candidates.of(cell.candidates.values.first()))
                }
        }

        val after = recording.process(field {
            candidates(0, 0, 1, 2)
            candidates(0, 1, 1, 3)
        })

        assertThat(seen).containsExactly(Candidates.of(1, 2), Candidates.of(1, 3))
        assertThat(after.candidatesAt(0, 0)).isEqualTo(Candidates.of(2))
        assertThat(after.candidatesAt(0, 1)).isEqualTo(Candidates.of(3))
    }
}

internal class HouseCandidateEliminatorTest {

    @Test
    fun removesValuesAlreadyPlacedInTheRowColumnOrSegment() {
        val after = HouseCandidateEliminator().process(field {
            solved(0, 8, 3)   // same row
            solved(8, 0, 5)   // same column
            solved(1, 1, 7)   // same segment
        })

        assertThat(after.candidatesAt(0, 0)).isEqualTo(Candidates.of(1, 2, 4, 6, 8, 9))
    }

    @Test
    fun reportsEachRemovalAgainstTheCellItAffected() {
        val recorder = RecordingDeductionListener()

        HouseCandidateEliminator().process(field {
            solved(0, 0, 4)
            candidates(0, 1, 4, 8)
            rowCandidates(0, 2..8, 8, 9)
        }, recorder)

        assertThat(recorder.eliminations).contains(
            Elimination(Technique.PEER_ELIMINATION, CellPosition(0, 1), Candidates.of(4))
        )
        assertThat(recorder.deductions).allMatch { it.technique == Technique.PEER_ELIMINATION }
    }
}
