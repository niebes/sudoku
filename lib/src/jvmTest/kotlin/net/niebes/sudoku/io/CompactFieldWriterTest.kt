package net.niebes.sudoku.io

import net.niebes.sudoku.Puzzles
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CompactFieldWriterTest {

    @Test
    fun roundTripsTheCompactFormat() {
        assertThat(CompactFieldWriter().render(Puzzles.classic.field())).isEqualTo(Puzzles.classic.givens)
        assertThat(CompactFieldWriter().render(Puzzles.classic.solved())).isEqualTo(Puzzles.classic.solution)
    }
}
