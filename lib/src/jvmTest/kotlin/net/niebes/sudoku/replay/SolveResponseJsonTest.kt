package net.niebes.sudoku.replay

import net.niebes.sudoku.Puzzles
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The JSON shape is the contract with app.js, formerly guaranteed by Jackson. These tests pin
 * exactly the properties the site reads and the two conventions it relies on: wire-cased enum
 * names, and absent-not-null optional fields.
 */
internal class SolveResponseJsonTest {

    @Test
    fun rendersASolveInTheShapeTheSiteReads() {
        val json = SolveResponseJson.render(solvePuzzle(Puzzles.classic.givens))

        assertThat(json).contains("\"outcome\":\"solved\"")
        assertThat(json).contains("\"givens\":\"${Puzzles.classic.givens}\"")
        assertThat(json).contains("\"solution\":\"${Puzzles.classic.solution}\"")
        assertThat(json).contains("\"steps\":[{")
        assertThat(json).contains("\"kind\":\"placement\"", "\"kind\":\"elimination\"")
        assertThat(json).contains("\"at\":{\"row\":")
        assertThat(json).contains("\"guesses\":0")
        // A solved response has no message; non-null omission, not a null literal.
        assertThat(json).doesNotContain("\"message\"", "null")
    }

    @Test
    fun omitsTheGridFieldsWhenParsingAlreadyFailed() {
        val json = SolveResponseJson.render(solvePuzzle("12345"))

        assertThat(json).contains("\"outcome\":\"invalid\"", "\"message\":\"")
        assertThat(json).doesNotContain("\"givens\"", "\"solution\"", "\"grid\":")
    }

    @Test
    fun escapesTheCharactersJsonCannotHoldBare() {
        val json = SolveResponseJson.render(
            SolveResponse(Outcome.INVALID, message = "a \"quoted\" back\\slash\nand tab\t.")
        )

        assertThat(json).contains("""a \"quoted\" back\\slash\nand tab\t.""")
    }
}
