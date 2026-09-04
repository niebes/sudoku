package net.niebes.sudoku.technique

/**
 * Every way of choosing [size] elements, order irrelevant.
 *
 * Lazy on purpose: the subset techniques reject almost every combination on its first check, so
 * materialising them all would be wasted work.
 */
internal fun <T> List<T>.combinations(size: Int): Sequence<List<T>> = sequence {
    if (size == 0) {
        yield(emptyList())
        return@sequence
    }
    for (index in indices) {
        if (this@combinations.size - index < size) break
        for (rest in subList(index + 1, this@combinations.size).combinations(size - 1)) {
            yield(listOf(this@combinations[index]) + rest)
        }
    }
}
