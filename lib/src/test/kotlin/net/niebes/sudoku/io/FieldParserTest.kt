package net.niebes.sudoku.io

import net.niebes.sudoku.SolveResult
import net.niebes.sudoku.SudokuSolver
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

internal class FieldParserTest {

    @Test
    fun parsesAndSolvesTheCompactFormat() {
        val input = CompactFieldParser()
            .parse("53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79")
        val expected = CompactFieldParser()
            .parse("534678912672195348198342567859761423426853791713924856961537284287419635345286179")

        val result = SudokuSolver().solve(input)

        assertThat(result).isInstanceOf(SolveResult.Solved::class.java)
        assertThat(result.field).isEqualTo(expected)
    }

    @Test
    fun readsTheSameGridThroughEveryFormat() {
        val compact = CompactFieldParser()
            .parse("53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79")
        val csv = CsvFieldParser().parse("""
            5,3,,,7,,,,
            6,,,1,9,5,,,
            ,9,8,,,,,6,
            8,,,,6,,,,3
            4,,,8,,3,,,1
            7,,,,2,,,,6
            ,6,,,,,2,8,
            ,,,4,1,9,,,5
            ,,,,8,,,7,9
        """.trimIndent())

        assertThat(csv).isEqualTo(compact)
    }

    @Test
    fun toleratesPaddingAroundValues() {
        val padded = CsvFieldParser().parse("""
             5 , 3 ,,, 7 ,,,,
             6 ,,, 1 , 9 , 5 ,,,
            ,9,8,,,,,6,
            8,,,,6,,,,3
            4,,,8,,3,,,1
            7,,,,2,,,,6
            ,6,,,,,2,8,
            ,,,4,1,9,,,5
            ,,,,8,,,7,9
        """.trimIndent())

        assertThat(padded).isEqualTo(
            CompactFieldParser()
                .parse("53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79")
        )
    }

    @Test
    fun rejectsGivensThatAlreadyConflict() {
        assertThatIllegalArgumentException()
            .isThrownBy {
                //          two 5s in the first row
                //          v   v
                CompactFieldParser()
                    .parse("53..5....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79")
            }
            .withMessageContaining("repeats a value already given")
    }

    @Test
    fun rejectsAShortRow() {
        assertThatIllegalArgumentException()
            .isThrownBy { CsvFieldParser().parse(",,,,,,,,\n".repeat(8) + ",,,,,,,") }
            .withMessageContaining("row 8 has 8 cells")
    }

    @Test
    fun rejectsTheWrongNumberOfRows() {
        assertThatIllegalArgumentException()
            .isThrownBy { CsvFieldParser().parse(",,,,,,,,\n".repeat(8).trim()) }
            .withMessageContaining("expected 9 rows, got 8")
    }
}
