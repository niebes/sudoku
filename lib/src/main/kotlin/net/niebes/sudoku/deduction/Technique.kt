package net.niebes.sudoku.deduction

/**
 * Which technique produced a deduction.
 *
 * It lives with the deductions rather than with the implementations so that the dependency runs one
 * way: techniques know about deductions, and deductions know nothing about techniques.
 */
enum class Technique {
    /** A house had one unsolved cell left, so the missing value goes there. */
    FULL_HOUSE,

    /** The cell had exactly one candidate left. */
    NAKED_SINGLE,

    /** The value fitted in only one cell of a house. */
    HIDDEN_SINGLE,

    /** A value already placed in a house cannot be a candidate elsewhere in it. */
    PEER_ELIMINATION,

    /** Within a segment the value fitted only on one line, so it leaves the rest of that line. */
    POINTING,

    /** Along a line the value fitted only in one segment, so it leaves the rest of that segment. */
    CLAIMING,

    /** k cells of a house held k values between them, so those values leave the rest of the house. */
    NAKED_SUBSET,

    /** k values of a house fitted in k cells, so everything else leaves those cells. */
    HIDDEN_SUBSET,

    /** The value was confined to n lines that between them use only n crossing lines. */
    BASIC_FISH,

    /** Propagation stalled, so the value is an assumption search may withdraw. */
    GUESS
}
