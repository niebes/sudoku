package net.niebes.sudoku.api

import com.fasterxml.jackson.annotation.JsonValue

/** How a solve attempt ended, told plainly - a stalled puzzle is stalled, not quietly guessed at. */
enum class Outcome(@get:JsonValue val wire: String) {
    SOLVED("solved"),
    STALLED("stalled"),
    INVALID("invalid")
}
