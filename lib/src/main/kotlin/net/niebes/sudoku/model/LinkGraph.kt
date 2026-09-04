package net.niebes.sudoku.model

/**
 * The inference graph over a field's remaining candidates.
 *
 * - A **strong** link means: if one end is false the other is true. Two candidates in a bivalue
 *   cell, or the two homes a value has left in some house.
 * - A **weak** link means: if one end is true the other is false. Two candidates of one cell, or
 *   the same value in two cells that see each other.
 *
 * Every strong link is also a weak one - if either end being false forces the other true, then
 * either being true certainly forces the other false - so a chain may always step along a strong
 * link where it needs a weak one. The reverse does not hold, which is the whole reason chains
 * alternate.
 *
 * Built once per pass and read many times, so links are computed on demand and cached.
 */
class LinkGraph(private val field: Field) {
    private val strongLinks = HashMap<Node, List<Node>>()
    private val weakLinks = HashMap<Node, List<Node>>()

    fun nodes(): List<Node> = field.unsolved().flatMap { cell ->
        cell.candidates.values.map { Node(cell.position, it) }
    }

    fun strong(node: Node): List<Node> = strongLinks.getOrPut(node) {
        val cell = field.cellAt(node.at)
        if (!cell.couldBe(node.value)) return@getOrPut emptyList()

        buildList {
            // The other candidate of a cell that has exactly two.
            (cell as UnsolvedCell).candidates
                .takeIf { it.size == 2 }
                ?.let { add(Node(node.at, (it - node.value).single())) }

            // The other place a house has left for this value.
            field.housesOf(node.at).forEach { house ->
                house.candidatesFor(node.value)
                    .takeIf { it.size == 2 }
                    ?.firstOrNull { it.position != node.at }
                    ?.let { add(Node(it.position, node.value)) }
            }
        }.distinct()
    }

    fun weak(node: Node): List<Node> = weakLinks.getOrPut(node) {
        val cell = field.cellAt(node.at)
        if (!cell.couldBe(node.value)) return@getOrPut emptyList()

        buildList {
            (cell as UnsolvedCell).candidates.values
                .filter { it != node.value }
                .forEach { add(Node(node.at, it)) }

            field.housesOf(node.at).forEach { house ->
                house.candidatesFor(node.value)
                    .filter { it.position != node.at }
                    .forEach { add(Node(it.position, node.value)) }
            }
        }.distinct()
    }
}
