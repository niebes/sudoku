package net.niebes.sudoku.replay


/** How a solve attempt ended, told plainly - a stalled puzzle is stalled, not quietly guessed at. */
enum class Outcome(val wire: String) {
    SOLVED("solved"),
    STALLED("stalled"),
    INVALID("invalid")
}
