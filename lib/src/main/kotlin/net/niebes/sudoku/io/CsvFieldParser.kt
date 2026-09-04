package net.niebes.sudoku.io

import net.niebes.sudoku.model.Candidates

/** One value per cell, blank for unknown: `4,,,8,,3,,,1`. */
class CsvFieldParser : DelimitedFieldParser(',', { token ->
    token.trim().toIntOrNull()?.let { Candidates.of(it) } ?: Candidates.NONE
})
