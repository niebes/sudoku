package net.niebes.sudoku.model
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CellPositionTest {

    @Test
    fun mapsCellsToTheirThreeByThreeSegment() {

        val cluster1 = SegmentPosition(0, 0)
        val cluster2 = SegmentPosition(0, 1)
        val cluster3 = SegmentPosition(0, 2)
        val cluster4 = SegmentPosition(1, 0)
        val cluster7 = SegmentPosition(2, 0)
        assertThat(CellPosition(0, 0).segment).isEqualTo(cluster1)
        assertThat(CellPosition(0, 1).segment).isEqualTo(cluster1)
        assertThat(CellPosition(0, 2).segment).isEqualTo(cluster1)
        assertThat(CellPosition(0, 3).segment).isEqualTo(cluster2)
        assertThat(CellPosition(0, 4).segment).isEqualTo(cluster2)
        assertThat(CellPosition(0, 5).segment).isEqualTo(cluster2)
        assertThat(CellPosition(0, 6).segment).isEqualTo(cluster3)
        assertThat(CellPosition(0, 7).segment).isEqualTo(cluster3)
        assertThat(CellPosition(0, 8).segment).isEqualTo(cluster3)
        assertThat(CellPosition(1, 0).segment).isEqualTo(cluster1)
        assertThat(CellPosition(1, 1).segment).isEqualTo(cluster1)
        assertThat(CellPosition(1, 2).segment).isEqualTo(cluster1)
        assertThat(CellPosition(2, 0).segment).isEqualTo(cluster1)
        assertThat(CellPosition(2, 1).segment).isEqualTo(cluster1)
        assertThat(CellPosition(2, 2).segment).isEqualTo(cluster1)

        assertThat(CellPosition(3, 0).segment).isEqualTo(cluster4)
        assertThat(CellPosition(3, 1).segment).isEqualTo(cluster4)
        assertThat(CellPosition(3, 2).segment).isEqualTo(cluster4)
        assertThat(CellPosition(6, 0).segment).isEqualTo(cluster7)
        assertThat(CellPosition(6, 1).segment).isEqualTo(cluster7)
        assertThat(CellPosition(6, 2).segment).isEqualTo(cluster7)
        //FieldWriter().writeField(Field(setOf(Cell())))
    }
}
