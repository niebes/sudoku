package net.niebes.sudoku.technique

import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.Candidates
import net.niebes.sudoku.model.CellPosition
import net.niebes.sudoku.model.Field
import net.niebes.sudoku.model.LinkGraph
import net.niebes.sudoku.model.Node
import net.niebes.sudoku.model.UnsolvedCell

/**
 * Alternating inference chain: strong link, weak link, strong link, ... , strong link.
 *
 * Follow it from one end. If the first node is false the second is true, so the third is false, the
 * fourth true, and so on to the last - which the final strong link makes true. So **one of the two
 * ends holds its value**, and two conclusions follow from that:
 *
 * - Both ends carry the same value: no cell seeing both of them can hold it.
 * - Both ends are in the same cell: that cell is one of the two values, and its others go.
 *
 * This is the general form of most of level 7, and of some of level 5 - an XY-chain is this over
 * bivalue cells, and a turbot fish is the three-link case. Those stay implemented separately
 * because they run sooner and cost far less; this is what is left when they are all stuck.
 *
 * [maxLinks] bounds the search, and the bound is worth more than it looks. Going from five links to
 * nine takes the corpus from fifteen puzzles needing search down to five, for about eight
 * milliseconds across all sixty-six. Past nine nothing more is found and the cost climbs, so this
 * is where the curve flattens - measured, not guessed.
 */
class AicEliminator(private val maxLinks: Int = 9) : EliminationTechnique {
    override val technique = Technique.AIC

    override fun eliminations(field: Field): List<Elimination> {
        val graph = LinkGraph(field)
        // Keyed by conclusion, not by chain: several chains commonly prove the same elimination,
        // and only the first proof found is worth reporting as its evidence.
        val found = LinkedHashMap<Pair<CellPosition, Candidates>, Elimination>()

        graph.nodes().forEach { start ->
            extend(field, graph, start, start, linkedSetOf(start), wantStrong = true, links = 0, found = found)
        }
        return found.values.toList()
    }

    private fun extend(
        field: Field,
        graph: LinkGraph,
        start: Node,
        here: Node,
        chain: LinkedHashSet<Node>,
        wantStrong: Boolean,
        links: Int,
        found: LinkedHashMap<Pair<CellPosition, Candidates>, Elimination>
    ) {
        if (links == maxLinks) return

        (if (wantStrong) graph.strong(here) else graph.weak(here)).forEach { next ->
            if (next in chain) return@forEach
            // A weak step is only worth taking if the chain can carry on strongly from there. This
            // prunes most of the tree: weak links are plentiful, strong ones are not.
            if (!wantStrong && graph.strong(next).isEmpty()) return@forEach

            // Only a chain that ends on a strong link proves anything. One strong link on its own
            // is just a conjugate pair, which cheaper techniques have already read.
            if (wantStrong && links >= 2) {
                conclude(field, start, next, walk(chain, next)).forEach { found.getOrPut(it.at to it.values) { it } }
            }

            chain += next
            extend(field, graph, start, next, chain, !wantStrong, links + 1, found)
            chain -= next
        }
    }

    private fun conclude(field: Field, start: Node, end: Node, walk: List<CellPosition>): List<Elimination> = when {
        start.value == end.value && start.at != end.at ->
            field.seenByBoth(start.at, end.at)
                .filter { it.couldBe(start.value) }
                .map { Elimination(technique, it.position, Candidates.of(start.value), walk) }

        start.at == end.at && start.value != end.value ->
            (field.cellAt(start.at) as UnsolvedCell).candidates
                .minus(Candidates.of(start.value, end.value))
                .takeIf { !it.isEmpty() }
                ?.let { listOf(Elimination(technique, start.at, it, walk)) }
                .orEmpty()

        else -> emptyList()
    }

    /**
     * The chain's cells in walking order. Consecutive duplicates collapse - a step between two
     * candidates of one cell is one stop, not two - but a chain that leaves a cell and returns to
     * it keeps both visits, because that return is the whole story of a same-cell conclusion.
     */
    private fun walk(chain: Collection<Node>, end: Node): List<CellPosition> =
        (chain.map { it.at } + end.at).fold(mutableListOf()) { path, at ->
            if (path.lastOrNull() != at) path += at
            path
        }
}
