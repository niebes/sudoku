package net.niebes.sudoku.model

/**
 * One candidate: this value, in this cell. The thing chains reason about.
 *
 * Techniques up to level four ask questions about cells; from colouring onwards the unit is finer
 * than a cell, because a chain steps between "the 5 here" and "the 5 there" while saying nothing
 * about the other candidates of either.
 */
data class Node(val at: CellPosition, val value: Int) {
    override fun toString(): String = "$value@$at"
}
