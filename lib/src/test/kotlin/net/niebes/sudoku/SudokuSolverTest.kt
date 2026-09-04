package net.niebes.sudoku

import net.niebes.sudoku.deduction.RecordingDeductionListener
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.io.CsvFieldParser
import net.niebes.sudoku.io.PipeFieldParser
import net.niebes.sudoku.model.Field
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SudokuSolverTest {

    @Test
    fun testIsSolvable() {
        val input = CsvFieldParser().parse(
            """
            ,6,,,,4,,,
            ,,,3,,,1,4,
            4,,,,5,1,,8,9
            ,,,5,,3,9,6,1
            ,,,,,,,,
            1,9,2,6,,8,,,
            2,8,,4,9,,,,3
            ,3,9,,,5,,,
            ,,,2,,,,9,
        """.trimIndent()
        )
        val expectedSolution = CsvFieldParser().parse(
            """
            5,6,1,9,8,4,2,3,7
            9,7,8,3,6,2,1,4,5
            4,2,3,7,5,1,6,8,9
            8,4,7,5,2,3,9,6,1
            3,5,6,1,4,9,8,7,2
            1,9,2,6,7,8,3,5,4
            2,8,5,4,9,6,7,1,3
            7,3,9,8,1,5,4,2,6
            6,1,4,2,3,7,5,9,8
        """.trimIndent()
        )
        solutionEquals(input, expectedSolution)
    }

    @Test
    fun unsolved() {
        val input = PipeFieldParser().parse(
            """
            ||||||||
            5|||2|7|6|||
            ||8||||||4
            7||||5||||9
            |||||9|||
            |||8|2||1||5
            ||4||||9|3|
            2||1||||||
            3|||||||4|2
         """.trimIndent()
        )
        val expectedSolution = PipeFieldParser().parse(
            """
            1 | 3 | 7 | 4 | 8 | 5 | 2 | 9 | 6
            5 | 4 | 9 | 2 | 7 | 6 | 3 | 1 | 8
            6 | 2 | 8 | 9 | 3 | 1 | 7 | 5 | 4
            7 | 1 | 2 | 6 | 5 | 3 | 4 | 8 | 9
            4 | 8 | 5 | 7 | 1 | 9 | 6 | 2 | 3
            9 | 6 | 3 | 8 | 2 | 4 | 1 | 7 | 5
            8 | 7 | 4 | 5 | 6 | 2 | 9 | 3 | 1
            2 | 9 | 1 | 3 | 4 | 8 | 5 | 6 | 7
            3 | 5 | 6 | 1 | 9 | 7 | 8 | 4 | 2
         """.trimIndent()
        )

        solutionEquals(input, expectedSolution)
    }

    @Test
    fun unsolved2() {
        val input = PipeFieldParser().parse(
            """
            3|2|||||||
            |||5|||8||
            4||||||||
            |6|||1|4|||
            ||5||||3||
            |||||||2|
            |||7|9||||8
            |||||||4|6
            |8||3|||||
         """.trimIndent()
        )
        val expectedSolution = PipeFieldParser().parse(
            """
            3 | 2 | 1 | 4 | 8 | 9 | 6 | 5 | 7
            9 | 7 | 6 | 5 | 2 | 3 | 8 | 1 | 4
            4 | 5 | 8 | 1 | 6 | 7 | 2 | 9 | 3
            2 | 6 | 3 | 9 | 1 | 4 | 7 | 8 | 5
            1 | 4 | 5 | 2 | 7 | 8 | 3 | 6 | 9
            8 | 9 | 7 | 6 | 3 | 5 | 4 | 2 | 1
            6 | 1 | 4 | 7 | 9 | 2 | 5 | 3 | 8
            7 | 3 | 2 | 8 | 5 | 1 | 9 | 4 | 6
            5 | 8 | 9 | 3 | 4 | 6 | 1 | 7 | 2
         """.trimIndent()
        )

        solutionEquals(input, expectedSolution)
    }

    @Test
    fun unsolved3() {
        val input = PipeFieldParser().parse(
            """
            |8|||||4||
            |||5|||||7
            |1|||||||
            7||||3||||6
            ||||2|8|||
            5|||||||1|
            |||||3|2|8|
            1|||7|||||
            ||||||||
         """.trimIndent()
        )
        val expectedSolution = PipeFieldParser().parse(
            """
            6 | 8 | 5 | 3 | 7 | 2 | 4 | 9 | 1
            2 | 9 | 3 | 5 | 4 | 1 | 8 | 6 | 7
            4 | 1 | 7 | 8 | 9 | 6 | 5 | 3 | 2
            7 | 4 | 8 | 1 | 3 | 5 | 9 | 2 | 6
            3 | 6 | 1 | 9 | 2 | 8 | 7 | 5 | 4
            5 | 2 | 9 | 4 | 6 | 7 | 3 | 1 | 8
            9 | 7 | 4 | 6 | 1 | 3 | 2 | 8 | 5
            1 | 5 | 2 | 7 | 8 | 9 | 6 | 4 | 3
            8 | 3 | 6 | 2 | 5 | 4 | 1 | 7 | 9
         """.trimIndent()
        )

        solutionEquals(input, expectedSolution)
    }

    @Test
    fun round15() {
        val input = CsvFieldParser().parse(
            """
            ,,9,,1,,7,4,
            2,,,,,,,,
            ,,,,,4,,3,
            ,,7,,,,9,,
            8,,3,,,6,,,
            ,,4,,2,,,1,
            ,,6,,,1,4,,9
            4,,,,,,3,,8
            ,,,,7,,,,1
        """.trimIndent()
        )
        val expectedSolution = CsvFieldParser().parse(
            """
             3,6,9,8,1,2,7,4,5
             2,4,8,3,5,7,1,9,6
             1,7,5,6,9,4,8,3,2
             6,2,7,1,3,5,9,8,4
             8,1,3,9,4,6,2,5,7
             5,9,4,7,2,8,6,1,3
             7,3,6,5,8,1,4,2,9
             4,5,1,2,6,9,3,7,8
             9,8,2,4,7,3,5,6,1
        """.trimIndent()
        )
        solutionEquals(input, expectedSolution)
        /**
        3,6,9,8,1,2,7,4,5
        2,4,8,3,5,7,1,9,6
        1,7,5,6,9,4,8,3,2
        6,2,7,1,3,5,9,8,4
        8,1,3,9,4,6,2,5,7
        5,9,4,7,2,8,6,1,3
        7,3,6,5,8,1,4,2,9
        4,5,1,2,6,9,3,7,8
        9,8,2,4,7,3,5,6,1
         */
    }

    @Test
    fun recordsDeductionsInsteadOfPrinting() {
        val singlesOnly = CsvFieldParser().parse(
            """
            ,6,,,,4,,,
            ,,,3,,,1,4,
            4,,,,5,1,,8,9
            ,,,5,,3,9,6,1
            ,,,,,,,,
            1,9,2,6,,8,,,
            2,8,,4,9,,,,3
            ,3,9,,,5,,,
            ,,,2,,,,9,
        """.trimIndent()
        )
        val needsSearch = PipeFieldParser().parse(
            """
            3|2|||||||
            |||5|||8||
            4||||||||
            |6|||1|4|||
            ||5||||3||
            |||||||2|
            |||7|9||||8
            |||||||4|6
            |8||3|||||
         """.trimIndent()
        )

        val easy = RecordingDeductionListener()
        assertThat(SudokuSolver(easy).solve(singlesOnly)).isInstanceOf(Solved::class.java)
        val hard = RecordingDeductionListener()
        assertThat(SudokuSolver(hard).solve(needsSearch)).isInstanceOf(Solved::class.java)

        assertThat(easy.deductions).isNotEmpty()
        assertThat(easy.deductions.map { it.technique }).doesNotContain(Technique.GUESS)
        assertThat(easy.guesses).isZero()
        // The techniques in the chain do not reach this one, so search has to assume its way in.
        assertThat(hard.guesses).isPositive()
    }

    private fun solutionEquals(input: Field, expectedSolution: Field) {
        val result = SudokuSolver().solve(input)

        assertThat(result).isInstanceOf(Solved::class.java)
        assertThat(result.field).isEqualTo(expectedSolution)
    }
}
