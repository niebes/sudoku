package net.niebes.sudoku.model

import net.niebes.sudoku.field
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class FieldTest {

    private val grid = field { }

    @Test
    fun cellsSharingAHouseSeeEachOther() {
        assertThat(grid.sees(CellPosition(0, 0), CellPosition(0, 5))).isTrue()   // same row
        assertThat(grid.sees(CellPosition(0, 0), CellPosition(5, 0))).isTrue()   // same column
        assertThat(grid.sees(CellPosition(0, 0), CellPosition(1, 1))).isTrue()   // same segment
    }

    @Test
    fun cellsSharingNoHouseDoNotSeeEachOther() {
        assertThat(grid.sees(CellPosition(0, 0), CellPosition(5, 5))).isFalse()
    }

    @Test
    fun aCellDoesNotSeeItself() {
        assertThat(grid.sees(CellPosition(4, 4), CellPosition(4, 4))).isFalse()
    }

    @Test
    fun findsHousesWhereAValueHasExactlyTwoHomes() {
        val pairs = field {
            rowCandidates(0, 0..1, 3, 4)
            rowCandidates(0, 2..8, 1, 2)
        }.conjugatePairs(3)

        assertThat(pairs).singleElement().satisfies({
            assertThat(it.value).isEqualTo(3)
            assertThat(it.house.kind).isEqualTo(HouseKind.ROW)
            assertThat(it.house.index).isEqualTo(0)
            assertThat(it.ends.map { end -> end.position })
                .containsExactly(CellPosition(0, 0), CellPosition(0, 1))
        })
    }

    @Test
    fun ignoresHousesThatAlreadyHoldTheValue() {
        // (0,0) and (0,1) still list 3 although row 0 has one; counting them would make a strong
        // link out of two cells that cannot hold the value at all.
        val pairs = field {
            solved(0, 5, 3)
            rowCandidates(0, 0..1, 3, 4)
            rowCandidates(0, 2..4, 1, 2)
            rowCandidates(0, 6..8, 1, 2)
        }.conjugatePairs(3)

        assertThat(pairs).isEmpty()
    }

    @Test
    fun aConjugatePairKnowsTheOppositeEndOfEachOfItsCells() {
        val pair = field {
            rowCandidates(0, 0..1, 3, 4)
            rowCandidates(0, 2..8, 1, 2)
        }.conjugatePairs(3).single()

        assertThat(pair.other(pair.first)).isEqualTo(pair.second)
        assertThat(pair.other(pair.second)).isEqualTo(pair.first)
    }
}
