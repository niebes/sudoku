package net.niebes.sudoku

import net.niebes.sudoku.replay.SolveResponseJson
import net.niebes.sudoku.replay.solvePuzzle

/**
 * Publishes the solver to the page as `sudokuSolver.solve(puzzle, allowGuessing)`. The result is
 * the parsed form of the JSON the api used to serve, so the site reads it unchanged. Going through
 * JSON rather than @JsExport keeps Kotlin's collections and value classes off the boundary - the
 * site sees plain arrays and objects.
 */
fun main() {
    val facade: dynamic = js("({})")
    facade.solve = { puzzle: String, allowGuessing: Boolean ->
        JSON.parse<dynamic>(SolveResponseJson.render(solvePuzzle(puzzle, allowGuessing)))
    }
    js("globalThis").sudokuSolver = facade
}
