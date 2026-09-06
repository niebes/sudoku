package net.niebes.sudoku.model

import net.niebes.sudoku.field
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HouseTest {

    @Test
    fun exposesTwentySevenHousesNineOfEachKind() {
        val houses = field { }.houses()

        assertThat(houses).hasSize(27)
        assertThat(houses.count { it.kind == HouseKind.ROW }).isEqualTo(9)
        assertThat(houses.count { it.kind == HouseKind.COLUMN }).isEqualTo(9)
        assertThat(houses.count { it.kind == HouseKind.SEGMENT }).isEqualTo(9)
        assertThat(houses).allSatisfy { assertThat(it.cells).hasSize(9) }
    }

    @Test
    fun groupsTheRightCellsIntoEachKindOfHouse() {
        val houses = field { }.houses().associateBy { it.kind to it.index }

        assertThat(houses.getValue(HouseKind.ROW to 3).cells.map { it.position.row }).containsOnly(3)
        assertThat(houses.getValue(HouseKind.COLUMN to 7).cells.map { it.position.column }).containsOnly(7)
        // Segment 4 is the middle one: rows 3..5, columns 3..5.
        assertThat(houses.getValue(HouseKind.SEGMENT to 4).cells.map { it.position })
            .containsExactlyInAnyOrderElementsOf(
                (3..5).flatMap { row -> (3..5).map { CellPosition(row, it) } }
            )
    }

    @Test
    fun everyCellBelongsToOneHouseOfEachKind() {
        val houses = field { }.housesOf(CellPosition(4, 7))

        assertThat(houses.map { it.kind to it.index })
            .containsExactlyInAnyOrder(
                HouseKind.ROW to 4,
                HouseKind.COLUMN to 7,
                HouseKind.SEGMENT to 5
            )
    }

    @Test
    fun reportsWhetherAValueIsAlreadyPlacedAndWhichCellsCouldStillTakeIt() {
        val house = field {
            solved(0, 0, 4)
            candidates(0, 1, 2, 7)
            candidates(0, 2, 7, 9)
        }.houses().single { it.kind == HouseKind.ROW && it.index == 0 }

        assertThat(house.holds(4)).isTrue()
        assertThat(house.holds(7)).isFalse()
        assertThat(house.unsolved()).hasSize(8)
        assertThat(house.candidatesFor(2).map { it.position })
            .contains(CellPosition(0, 1))
            .doesNotContain(CellPosition(0, 2))
    }

    @Test
    fun exposesFiftyFourSegmentLineOverlapsOfThreeCellsEach() {
        val intersections = field { }.intersections()

        assertThat(intersections).hasSize(54)
        assertThat(intersections).allSatisfy { intersection ->
            assertThat(intersection.segment.kind).isEqualTo(HouseKind.SEGMENT)
            assertThat(intersection.line.kind).isIn(HouseKind.ROW, HouseKind.COLUMN)
            assertThat(intersection.cells).hasSize(3)
            assertThat(intersection.segmentOnly()).hasSize(6)
            assertThat(intersection.lineOnly()).hasSize(6)
        }
    }

    @Test
    fun splitsAnOverlapIntoSharedSegmentOnlyAndLineOnlyCells() {
        val intersection = field { }.intersections()
            .single { it.segment.index == 0 && it.line.kind == HouseKind.ROW && it.line.index == 1 }

        assertThat(intersection.cells.map { it.position })
            .containsExactly(CellPosition(1, 0), CellPosition(1, 1), CellPosition(1, 2))
        assertThat(intersection.segmentOnly().map { it.position })
            .containsExactlyInAnyOrder(
                CellPosition(0, 0), CellPosition(0, 1), CellPosition(0, 2),
                CellPosition(2, 0), CellPosition(2, 1), CellPosition(2, 2)
            )
        assertThat(intersection.lineOnly().map { it.position })
            .containsExactlyInAnyOrderElementsOf((3..8).map { CellPosition(1, it) })
    }
}
