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

    /** Two strong links on one value, joined so that one of their far ends must hold it. */
    TURBOT_FISH,

    /** A two-candidate pivot forced one of two pincers to take the value they share. */
    XY_WING,

    /** Like XY_WING, but the pivot could take the shared value too, so it eliminates less. */
    XYZ_WING,

    /** Two cells holding the same pair, joined by a strong link that forces one onto its other value. */
    W_WING,

    /** A segment's candidates fitted one row plus one column, which a strong link then pinned down. */
    EMPTY_RECTANGLE,

    /** Two-colouring a value's strong links trapped a cell between the colours, or wrapped one out. */
    SIMPLE_COLOURING,

    /** Propagation stalled, so the value is an assumption search may withdraw. */
    GUESS
}
