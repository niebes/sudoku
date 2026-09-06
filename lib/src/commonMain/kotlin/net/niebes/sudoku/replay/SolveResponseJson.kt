package net.niebes.sudoku.replay

/**
 * Renders a [SolveResponse] to the JSON the site reads - the shape the api used to serve, kept by
 * hand now that no Jackson sits in between. Null fields are omitted; empty lists and a zero guess
 * count are not, matching what the wire always carried.
 */
object SolveResponseJson {

    fun render(response: SolveResponse): String = buildString {
        append("{\"outcome\":").append(quoted(response.outcome.wire))
        response.givens?.let { append(",\"givens\":").append(quoted(it)) }
        response.solution?.let { append(",\"solution\":").append(quoted(it)) }
        response.grid?.let { append(",\"grid\":").append(quoted(it)) }
        append(",\"steps\":[")
        response.steps.forEachIndexed { i, step ->
            if (i > 0) append(',')
            append(render(step))
        }
        append("],\"guesses\":").append(response.guesses)
        response.message?.let { append(",\"message\":").append(quoted(it)) }
        append('}')
    }

    private fun render(step: Step): String = buildString {
        append("{\"technique\":").append(quoted(step.technique))
        append(",\"kind\":").append(quoted(step.kind.wire))
        append(",\"at\":").append(render(step.at))
        step.value?.let { append(",\"value\":").append(it) }
        append(",\"values\":[").append(step.values.joinToString(",")).append(']')
        append(",\"because\":[").append(step.because.joinToString(",") { render(it) }).append(']')
        append(",\"explanation\":").append(quoted(step.explanation))
        append('}')
    }

    private fun render(cell: CellRef) = "{\"row\":${cell.row},\"column\":${cell.column}}"

    private fun quoted(text: String): String = buildString(text.length + 2) {
        append('"')
        text.forEach { ch ->
            when {
                ch == '"' -> append("\\\"")
                ch == '\\' -> append("\\\\")
                ch == '\n' -> append("\\n")
                ch == '\r' -> append("\\r")
                ch == '\t' -> append("\\t")
                ch < ' ' -> append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                else -> append(ch)
            }
        }
        append('"')
    }
}
