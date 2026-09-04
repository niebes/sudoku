package net.niebes.sudoku.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a step does to the grid. A guess is deliberately its own kind rather than a placement:
 * it writes a value like one, but it is an assumption, and the site must never dress it up as
 * a deduction.
 */
@Serializable
enum class StepKind {
    @SerialName("placement") PLACEMENT,
    @SerialName("elimination") ELIMINATION,
    @SerialName("guess") GUESS
}
