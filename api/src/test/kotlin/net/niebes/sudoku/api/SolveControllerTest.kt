package net.niebes.sudoku.api

import com.fasterxml.jackson.databind.ObjectMapper
import net.niebes.sudoku.GeneratedPuzzles
import net.niebes.sudoku.HardPuzzles
import net.niebes.sudoku.Puzzles
import net.niebes.sudoku.Stalled
import net.niebes.sudoku.SudokuSolver
import net.niebes.sudoku.io.CompactFieldParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@AutoConfigureMockMvc
internal class SolveControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var json: ObjectMapper

    private fun solve(request: SolveRequest): Pair<Int, SolveResponse> {
        val result = mockMvc.perform(
            post("/solve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))
        ).andReturn()
        return result.response.status to json.readValue(result.response.contentAsString, SolveResponse::class.java)
    }

    @Test
    fun solvesAPuzzleAndReturnsTheSteps() {
        val (status, body) = solve(SolveRequest(Puzzles.classic.givens))

        assertThat(status).isEqualTo(200)
        assertThat(body.outcome).isEqualTo(Outcome.SOLVED)
        assertThat(body.givens).isEqualTo(Puzzles.classic.givens)
        assertThat(body.solution).isEqualTo(Puzzles.classic.solution)
        assertThat(body.steps).isNotEmpty()
        assertThat(body.steps).allMatch { it.explanation.isNotBlank() }
    }

    @Test
    fun rejectsAPuzzleOfTheWrongLengthAtTheEdge() {
        val (status, body) = solve(SolveRequest("12345"))

        assertThat(status).isEqualTo(400)
        assertThat(body.outcome).isEqualTo(Outcome.INVALID)
        assertThat(body.message).contains("81")
    }

    @Test
    fun rejectsGivensThatAlreadyConflict() {
        val (status, body) = solve(SolveRequest("55" + ".".repeat(79)))

        assertThat(status).isEqualTo(400)
        assertThat(body.outcome).isEqualTo(Outcome.INVALID)
        assertThat(body.message).isNotBlank()
    }

    @Test
    fun namesTheCellThatProvesAPuzzleImpossible() {
        // Consistent givens, no solution: r1c1 can only take 9, but r9c1 already holds the
        // column's 9. Full house fires first and places that 9, so the contradiction surfaces
        // as the column's duplicate at r9c1 - which is the cell the message must name.
        val (status, body) = solve(SolveRequest(".12345678" + ".".repeat(63) + "9........"))

        assertThat(status).isEqualTo(200)
        assertThat(body.outcome).isEqualTo(Outcome.INVALID)
        assertThat(body.message).contains("r9c1")
    }

    @Test
    fun admitsWhenTheTechniquesAloneCannotFinish() {
        // Found rather than named: which puzzles outrun the chain shifts as techniques improve.
        val beyondTheChain = (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).first {
            SudokuSolver().propagate(CompactFieldParser().parse(it.givens)) is Stalled
        }

        val (status, body) = solve(SolveRequest(beyondTheChain.givens, allowGuessing = false))

        assertThat(status).isEqualTo(200)
        assertThat(body.outcome).isEqualTo(Outcome.STALLED)
        assertThat(body.solution).isNull()
        assertThat(body.steps).isNotEmpty()
        assertThat(body.message).isNotBlank()
        // The partial grid is honest: it is where the steps end, not a solution.
        assertThat(body.grid).contains(".")
    }

    @Test
    fun labelsSearchAssumptionsAsGuessesInTheStepList() {
        val needsSearch = (Puzzles.all + GeneratedPuzzles.all + HardPuzzles.all).first {
            SudokuSolver().propagate(CompactFieldParser().parse(it.givens)) is Stalled
        }

        val (status, body) = solve(SolveRequest(needsSearch.givens))

        assertThat(status).isEqualTo(200)
        assertThat(body.outcome).isEqualTo(Outcome.SOLVED)
        assertThat(body.guesses).isGreaterThan(0)
        assertThat(body.steps.count { it.kind == StepKind.GUESS }).isEqualTo(body.guesses)
    }
}
