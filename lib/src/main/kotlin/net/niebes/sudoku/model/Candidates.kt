package net.niebes.sudoku.model

/**
 * The values a cell may still take, held as a nine-bit mask where bit `v - 1` means `v` is possible.
 *
 * Sudoku techniques are set algebra over 1..9, so a mask keeps them to single instructions: a naked
 * pair is two cells in a house with equal masks and [size] 2, a pointing pair is an intersection.
 * The set-of-boxed-Int representation this replaces allocated a HashSet per cell per pass, which
 * search makes far too expensive.
 */
@JvmInline
value class Candidates(val mask: Int) {
    val size: Int get() = Integer.bitCount(mask)

    /** Ascending, for printing and for anything that still wants a collection. */
    val values: Set<Int> get() = (1..9).filterTo(LinkedHashSet(size)) { contains(it) }

    fun isEmpty(): Boolean = mask == 0
    fun contains(value: Int): Boolean = mask and bitOf(value) != 0
    fun single(): Int = values.single()

    operator fun minus(value: Int): Candidates = Candidates(mask and bitOf(value).inv())
    operator fun minus(other: Candidates): Candidates = Candidates(mask and other.mask.inv())
    operator fun plus(value: Int): Candidates = Candidates(mask or bitOf(value))
    infix fun and(other: Candidates): Candidates = Candidates(mask and other.mask)
    infix fun or(other: Candidates): Candidates = Candidates(mask or other.mask)

    override fun toString(): String = values.joinToString(",", "{", "}")

    companion object {
        val ALL = Candidates(0b1_1111_1111)
        val NONE = Candidates(0)

        fun of(values: Iterable<Int>): Candidates =
            Candidates(values.fold(0) { mask, value -> mask or bitOf(value) })

        fun of(vararg values: Int): Candidates = of(values.asIterable())

        private fun bitOf(value: Int): Int {
            require(value in 1..9) { "$value is not a sudoku value" }
            return 1 shl (value - 1)
        }
    }
}
