package net.niebes.sudoku.io

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class FieldWriterTest {

    @Test
    fun rendersSolvedCellsAsValuesAndUnsolvedOnesAsCandidates() {
        val field = CsvFieldParser().parse("""
            5,3,4,6,7,8,9,1,2
            6,7,2,1,9,5,3,4,8
            1,9,8,3,4,2,5,6,7
            8,5,9,7,6,1,4,2,3
            4,2,6,8,5,3,7,9,1
            7,1,3,9,2,4,8,5,6
            9,6,1,5,3,7,2,8,4
            2,8,7,4,1,9,6,3,5
            3,4,5,2,8,6,1,7,
        """.trimIndent())

        val rows = SolutionWriter().render(field).lines()

        assertThat(rows).hasSize(9)
        assertThat(rows.first()).isEqualTo("5 | 3 | 4 | 6 | 7 | 8 | 9 | 1 | 2")
        // The last cell is blank in the input, so it still carries every candidate.
        assertThat(rows.last()).isEqualTo("3 | 4 | 5 | 2 | 8 | 6 | 1 | 7 | {1,2,3,4,5,6,7,8,9}")
    }

    @Test
    fun rendersInRowMajorOrderRegardlessOfHowTheFieldWasBuilt() {
        val input = "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
        val field = CompactFieldParser().parse(input)

        val flattened = SolutionWriter().render(field)
            .replace(" | ", "")
            .lines()
            .joinToString("")

        assertThat(flattened).isEqualTo(input.replace(".", "{1,2,3,4,5,6,7,8,9}"))
    }
}
