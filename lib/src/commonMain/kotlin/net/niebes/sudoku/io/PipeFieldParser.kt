package net.niebes.sudoku.io

import net.niebes.sudoku.model.Candidates

/** Like [CsvFieldParser], but a cell may also carry a candidate list: `4||1,2,7|8`. */
class PipeFieldParser : DelimitedFieldParser('|', { token ->
    Candidates.of(token.filter { it.isDigit() || it == ',' }.split(',').mapNotNull(String::toIntOrNull))
})
