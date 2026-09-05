package net.niebes.sudoku.api

import com.fasterxml.jackson.annotation.JsonValue

/**
 * What a step does to the grid. A guess is deliberately its own kind rather than a placement:
 * it writes a value like one, but it is an assumption, and the site must never dress it up as
 * a deduction.
 */
enum class StepKind(@get:JsonValue val wire: String) {
    PLACEMENT("placement"),
    ELIMINATION("elimination"),
    GUESS("guess")
}
