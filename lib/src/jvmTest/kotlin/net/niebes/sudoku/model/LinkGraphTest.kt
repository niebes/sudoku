package net.niebes.sudoku.model

import net.niebes.sudoku.field
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LinkGraphTest {

    @Test
    fun theTwoCandidatesOfABivalueCellAreStronglyLinked() {
        val graph = LinkGraph(field { candidates(0, 0, 3, 7) })

        assertThat(graph.strong(Node(CellPosition(0, 0), 3)))
            .contains(Node(CellPosition(0, 0), 7))
    }

    @Test
    fun theTwoHomesOfAValueInAHouseAreStronglyLinked() {
        val graph = LinkGraph(field {
            rowCandidates(0, 0..1, 5, 6)
            rowCandidates(0, 2..8, 1, 2, 3)
        })

        assertThat(graph.strong(Node(CellPosition(0, 0), 5)))
            .contains(Node(CellPosition(0, 1), 5))
    }

    @Test
    fun aCellWithThreeCandidatesLinksStronglyToNeitherOfTheOthers() {
        val graph = LinkGraph(field { candidates(4, 4, 1, 2, 3) })

        assertThat(graph.strong(Node(CellPosition(4, 4), 1)))
            .doesNotContain(Node(CellPosition(4, 4), 2), Node(CellPosition(4, 4), 3))
    }

    @Test
    fun candidatesSharingACellAreWeaklyLinked() {
        val graph = LinkGraph(field { candidates(0, 0, 3, 7) })

        assertThat(graph.weak(Node(CellPosition(0, 0), 3)))
            .contains(Node(CellPosition(0, 0), 7))
    }

    @Test
    fun theSameValueInCellsThatSeeEachOtherIsWeaklyLinked() {
        val graph = LinkGraph(field { candidates(0, 0, 3, 7) })

        assertThat(graph.weak(Node(CellPosition(0, 0), 3)))
            .contains(Node(CellPosition(0, 5), 3), Node(CellPosition(5, 0), 3), Node(CellPosition(1, 1), 3))
            .doesNotContain(Node(CellPosition(5, 5), 3))
    }

    @Test
    fun everyStrongLinkIsAlsoAWeakOne() {
        // If one end being false makes the other true, one being true certainly makes the other
        // false - so chains may always step along a strong link as though it were weak.
        val graph = LinkGraph(field {
            rowCandidates(0, 0..1, 5, 6)
            rowCandidates(0, 2..8, 1, 2, 3)
        })
        val node = Node(CellPosition(0, 0), 5)

        assertThat(graph.weak(node)).containsAll(graph.strong(node))
    }

    @Test
    fun offersOneNodePerRemainingCandidate() {
        val graph = LinkGraph(field {
            candidates(0, 0, 3, 7)
            solved(0, 1, 4)
        })

        assertThat(graph.nodes()).contains(Node(CellPosition(0, 0), 3), Node(CellPosition(0, 0), 7))
        assertThat(graph.nodes().map { it.at }).doesNotContain(CellPosition(0, 1))
        assertThat(graph.nodes()).hasSize(79 * 9 + 2)
    }
}
