package net.niebes.sudoku.io

import net.niebes.sudoku.model.Field

interface FieldWriter {
    /** Renders [field] as text. Printing it is the caller's decision. */
    fun render(field: Field): String

    fun writeField(field: Field) = println(render(field))
}
