package net.niebes.sudoku.api

import net.niebes.sudoku.Contradiction
import net.niebes.sudoku.Solved
import net.niebes.sudoku.Stalled
import net.niebes.sudoku.SudokuSolver
import net.niebes.sudoku.deduction.SuccessfulLineRecordingListener
import net.niebes.sudoku.io.CompactFieldParser
import net.niebes.sudoku.io.CompactFieldWriter
import net.niebes.sudoku.model.CellPosition
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * One endpoint: POST /solve takes a puzzle and returns the solution together with the reasoning
 * that reaches it, step by step. The static site that plays those steps back is served from the
 * classpath by Spring's default static resource handling.
 */
@RestController
class SolveController {

    @PostMapping("/solve")
    fun solve(@RequestBody request: SolveRequest): ResponseEntity<SolveResponse> {
        // The parsers reject the wrong number of cells and givens that already conflict, with a
        // message worth relaying. Their objection is the caller's mistake, never a server error.
        val givens = try {
            CompactFieldParser().parse(request.puzzle)
        } catch (rejected: IllegalArgumentException) {
            return ResponseEntity.badRequest()
                .body(SolveResponse(Outcome.INVALID, message = rejected.message))
        }

        val trace = SuccessfulLineRecordingListener()
        val solver = SudokuSolver(trace)
        val result = if (request.allowGuessing) solver.solve(givens) else solver.propagate(givens)
        val replay = TraceReplayer().replay(givens, trace.deductions)
        val compactGivens = CompactFieldWriter().render(givens)

        return ResponseEntity.ok(
            when (result) {
                is Solved -> SolveResponse(
                    Outcome.SOLVED,
                    givens = compactGivens,
                    solution = replay.grid,
                    grid = replay.grid,
                    steps = replay.steps,
                    guesses = trace.guesses
                )

                is Stalled -> SolveResponse(
                    Outcome.STALLED,
                    givens = compactGivens,
                    grid = replay.grid,
                    steps = replay.steps,
                    message = "No implemented technique makes further progress on this grid. " +
                        "The steps show everything the techniques could honestly deduce; " +
                        "solving with guessing allowed would continue by assumption, labelled as such."
                )

                is Contradiction -> SolveResponse(
                    Outcome.INVALID,
                    givens = compactGivens,
                    // The proving cell is either left without a candidate or forced into a
                    // duplicate - Contradiction does not say which, so nor does the message.
                    message = "This puzzle cannot be completed: following the givens breaks " +
                        "down at ${result.at.label()}."
                )
            }
        )
    }

    private fun CellPosition.label() = "r${row + 1}c${column + 1}"
}
