package net.niebes.sudoku.replay

import net.niebes.sudoku.deduction.Deduction
import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Placement
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.SolvedCell
import net.niebes.sudoku.model.UnsolvedCell

/**
 * Rebuilds the grid a deduction-at-a-time and keeps the steps that visibly change it.
 *
 * Two normalisations happen here, both deliberate:
 *
 * - A placement on a cell the replay has already filled folds away. The solver reports a hidden
 *   single when it narrows a cell and a naked single when it later solidifies it - one pedagogical
 *   move told twice - so the replay treats every placement as filling the cell at once.
 * - An elimination that removes nothing the replayed grid still holds folds away too, rather than
 *   showing a step in which the grid does not change.
 *
 * Expects a trace from [net.niebes.sudoku.deduction.SuccessfulLineRecordingListener]: deductions
 * from abandoned search branches would replay wrong moves as confidently as right ones.
 */
class TraceReplayer {

    fun replay(givens: Field, deductions: List<Deduction>): Replay {
        val cells = givens.cells.toTypedArray()
        val steps = mutableListOf<Step>()

        deductions.forEach { deduction ->
            val index = deduction.at.index
            val cell = cells[index]

            when (deduction) {
                is Placement -> {
                    if (cell !is UnsolvedCell) return@forEach
                    cells[index] = SolvedCell(deduction.at, deduction.value)
                    steps += step(deduction, if (deduction.technique == Technique.GUESS) StepKind.GUESS else StepKind.PLACEMENT)
                }
                is Elimination -> {
                    if (cell !is UnsolvedCell) return@forEach
                    val removable = cell.candidates and deduction.values
                    if (removable.isEmpty()) return@forEach
                    cells[index] = UnsolvedCell(deduction.at, cell.candidates - removable)
                    steps += step(deduction.copy(values = removable), StepKind.ELIMINATION)
                }
            }
        }

        val grid = cells.joinToString("") { if (it is SolvedCell) it.value.toString() else "." }
        return Replay(steps, grid)
    }

    private fun step(deduction: Placement, kind: StepKind) = Step(
        technique = deduction.technique.name,
        kind = kind,
        at = CellRef.of(deduction.at),
        value = deduction.value,
        because = deduction.because.map { CellRef.of(it) },
        explanation = Explanations.explain(deduction)
    )

    private fun step(deduction: Elimination, kind: StepKind) = Step(
        technique = deduction.technique.name,
        kind = kind,
        at = CellRef.of(deduction.at),
        values = deduction.values.values.toList(),
        because = deduction.because.map { CellRef.of(it) },
        explanation = Explanations.explain(deduction)
    )
}
