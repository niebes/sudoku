package net.niebes.sudoku

import net.niebes.sudoku.model.Field

/** No technique in the chain can make further progress; [field] is partially solved. */
data class Stalled(override val field: Field) : SolveResult
