package net.niebes.sudoku.model

/**
 * The only two cells a value can go in within one house - a strong link. Exactly one of them holds
 * the value, so knowing either end settles the other, which is what every chain-flavoured technique
 * is built out of.
 */
data class ConjugatePair(
    val value: Int,
    val house: House,
    val first: UnsolvedCell,
    val second: UnsolvedCell
) {
    val ends: List<UnsolvedCell> get() = listOf(first, second)

    fun other(end: UnsolvedCell): UnsolvedCell = if (end == first) second else first

    override fun toString(): String = "$value in ${first.position} or ${second.position} ($house)"
}
