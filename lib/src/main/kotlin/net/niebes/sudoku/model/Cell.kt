package net.niebes.sudoku.model

sealed class Cell(
    val position: CellPosition
) {
    companion object {
        fun new(position: CellPosition) = UnsolvedCell(position)
        fun new(position: CellPosition, candidates: Set<Int>) = when (candidates.size){
            0 ->  UnsolvedCell(position)
            1 ->  SolvedCell(position, candidates.first())
            else -> UnsolvedCell(position, Candidates.of(candidates))
        }

        fun new(position: CellPosition, value: Int?) =
            value?.let { SolvedCell(position, value) } ?: UnsolvedCell(position)
    }
}

class SolvedCell(
        position: CellPosition,
        val value: Int
) : Cell(position) {
    override fun toString(): String = value.toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SolvedCell

        if (value != other.value) return false
        if (position != other.position) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + position.hashCode()
        return result
    }
}

class UnsolvedCell(
        position: CellPosition,
        val candidates: Candidates = Candidates.ALL
) : Cell(position) {
    override fun toString(): String = candidates.toString()
    fun removeCandidate(value: Int): UnsolvedCell = UnsolvedCell(position, candidates - value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnsolvedCell

        if (candidates != other.candidates) return false
        if (position != other.position) return false

        return true
    }

    override fun hashCode(): Int {
        var result = candidates.hashCode()
        result = 31 * result + position.hashCode()
        return result
    }
}
