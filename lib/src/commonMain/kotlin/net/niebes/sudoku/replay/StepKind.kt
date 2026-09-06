package net.niebes.sudoku.replay


/**
 * What a step does to the grid. A guess is deliberately its own kind rather than a placement:
 * it writes a value like one, but it is an assumption, and the site must never dress it up as
 * a deduction.
 */
enum class StepKind(val wire: String) {
    PLACEMENT("placement"),
    ELIMINATION("elimination"),
    GUESS("guess")
}
