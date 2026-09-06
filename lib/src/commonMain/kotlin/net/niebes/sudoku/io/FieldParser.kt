package net.niebes.sudoku.io

import net.niebes.sudoku.model.Field

/** Reads a puzzle from text. Implementations differ only in the layout they accept. */
interface FieldParser {
    fun parse(input: String): Field
}
