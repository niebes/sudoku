package net.niebes.sudoku.model

data class Candidates(val values: Set<Int> = (1..9).toSet())
