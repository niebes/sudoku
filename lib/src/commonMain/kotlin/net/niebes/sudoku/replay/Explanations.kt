package net.niebes.sudoku.replay

import net.niebes.sudoku.deduction.Deduction
import net.niebes.sudoku.deduction.Elimination
import net.niebes.sudoku.deduction.Placement
import net.niebes.sudoku.deduction.Technique
import net.niebes.sudoku.model.CellPosition

/**
 * One sentence per deduction, in terms of this grid: the cells cited by the deduction's evidence
 * and the actual values involved, not a recital of what the technique is in general. The general
 * teaching lives in the site's technique panel; this is the line that ties it to the board.
 *
 * Cells are named r1c1..r9c9, one-based, matching how the site labels the grid.
 */
internal object Explanations {

    fun explain(deduction: Deduction): String = when (deduction) {
        is Placement -> placement(deduction)
        is Elimination -> elimination(deduction)
    }

    private fun placement(d: Placement): String = when (d.technique) {
        Technique.FULL_HOUSE ->
            "${houseOfPositions(d.because + d.at) ?: "This house"} has a single empty cell left. " +
                "The other eight are filled, and the one value missing from them is ${d.value}, " +
                "so ${d.at.label()} takes it."

        Technique.NAKED_SINGLE ->
            "${d.at.label()} has one candidate left: earlier steps crossed off everything " +
                "but ${d.value}, so ${d.value} goes in."

        Technique.HIDDEN_SINGLE ->
            "In ${houseOfPositions(d.because + d.at)?.lowercase() ?: "one house"}, ${d.value} fits nowhere else: " +
                "every other cell there is either solved already or has lost ${d.value} from its " +
                "pencil marks. That leaves ${d.at.label()}."

        Technique.GUESS ->
            "No implemented technique makes progress on this grid, so the solver assumes " +
                "${d.at.label()} is ${d.value} and follows the consequences. It picked this cell " +
                "because it has the fewest candidates left. This is a guess, not a deduction. " +
                "When an assumption ran into a contradiction the solver withdrew it, and none of " +
                "those steps appear here."

        else -> "${d.technique}: ${d.at.label()} must be ${d.value}."
    }

    private fun elimination(d: Elimination): String {
        val values = d.values.values.toList()
        val cells = d.because.map { CellRef.of(it) }
        return when (d.technique) {
            Technique.PEER_ELIMINATION ->
                "${d.at.label()} shares a row, column or box with ${cells.size.cells()} already " +
                    "holding ${values.prose()}. A value cannot appear twice in the same house, " +
                    "so ${if (values.size == 1) "that mark goes" else "those marks go"}."

            Technique.POINTING -> {
                val v = values.single()
                "Inside ${boxOf(cells)}, $v fits only in the highlighted cells, which all lie on " +
                    "${lineOf(cells)}. The box has to put its $v in one of them, so the rest of " +
                    "the line, ${d.at.label()} included, cannot hold one."
            }

            Technique.CLAIMING -> {
                val v = values.single()
                "Along ${lineOf(cells)}, $v fits only in the highlighted cells, all inside " +
                    "${boxOf(cells)}. The line has to put its $v in one of them, which uses up " +
                    "the box's $v, so the rest of the box, ${d.at.label()} included, cannot hold one."
            }

            Technique.NAKED_SUBSET ->
                "The ${cells.size} highlighted cells of ${houseWith(cells, CellRef.of(d.at))?.lowercase() ?: "this house"} " +
                    "hold only ${cells.size} different candidates between them. Those cells use " +
                    "those values up, so ${values.prose()} cannot appear at ${d.at.label()}."

            Technique.HIDDEN_SUBSET ->
                "In ${houseOf(cells)?.lowercase() ?: "this house"}, ${cells.size} values fit only in the " +
                    "${cells.size} highlighted cells. Those cells must hold exactly those values " +
                    "between them, so ${values.prose()} ${values.goVerb()} from ${d.at.label()}."

            Technique.BASIC_FISH -> {
                val v = values.single()
                val (base, cover) = fishOrientation(cells, CellRef.of(d.at))
                "${fishName(cells)}: in ${cells.map { it.lineIndex(base) }.distinct().size.lines(base)}, " +
                    "$v fits only in the highlighted cells, and they line up on the same " +
                    "${cells.map { it.lineIndex(cover) }.distinct().size.lines(cover)}. Each of those " +
                    "${cover}s takes its $v from one of them, so $v leaves every other cell in " +
                    "those ${cover}s, including ${d.at.label()}."
            }

            Technique.TURBOT_FISH -> {
                val v = values.single()
                "The chain ${cells.chain()} alternates two strong links on $v, joined in the middle. " +
                    "If ${cells.first().label()} is not $v then ${cells[1].label()} is, which forces " +
                    "${cells[2].label()} off $v and ${cells.last().label()} onto it. One end of the " +
                    "chain holds $v either way, and ${d.at.label()} sees both ends."
            }

            Technique.XY_WING -> {
                val v = values.single()
                "${cells.first().label()} is the pivot, with two candidates. Whichever value it takes " +
                    "forces one of the pincers ${cells[1].label()} and ${cells[2].label()} onto $v. " +
                    "${d.at.label()} sees both pincers, so it cannot be $v."
            }

            Technique.XYZ_WING -> {
                val v = values.single()
                "${cells.first().label()}, ${cells[1].label()} and ${cells[2].label()} must share out " +
                    "three values between them, and any of the three could be the $v, so one of " +
                    "them is. ${d.at.label()} sees all three and cannot be $v."
            }

            Technique.W_WING -> {
                val v = values.single()
                "${cells.first().label()} and ${cells.last().label()} hold the same pair. The strong " +
                    "link ${cells[1].label()}-${cells[2].label()} puts its value at one end or the " +
                    "other, and that end forces the pair cell it sees onto $v. One of the pair is " +
                    "$v either way, and ${d.at.label()} sees both."
            }

            Technique.EMPTY_RECTANGLE -> {
                val v = values.single()
                val (link, places) = cells.takeLast(2) to cells.dropLast(2)
                "In ${boxOf(places)}, every place for $v (highlighted) sits on one row plus one " +
                    "column. If ${d.at.label()} were $v, the strong link ${link.first().label()}-" +
                    "${link.last().label()} would push the box's $v off both arms at once, which " +
                    "cannot happen. So ${d.at.label()} is not $v."
            }

            Technique.UNIQUE_RECTANGLE ->
                "Three corners of the highlighted rectangle hold the identical pair ${values.prose()}. " +
                    "If ${d.at.label()} were reduced to that pair too, the two values could swap " +
                    "around the rectangle and the puzzle would have two solutions. A proper puzzle " +
                    "has exactly one, so ${d.at.label()} holds neither of them."

            Technique.BUG_PLUS_ONE ->
                "Every other unsolved cell is down to a pair. Take ${values.prose()} away here and " +
                    "each remaining candidate would appear exactly twice in every house, a pattern " +
                    "that never has a unique solution. This cell must keep its extra candidate, " +
                    "so ${values.prose()} ${values.goVerb()}."

            Technique.AIC ->
                if (cells.firstOrNull() == CellRef.of(d.at))
                    "A chain of alternating strong and weak links leaves ${d.at.label()} as one value " +
                        "and arrives back at it as another: ${cells.chain()}. One of those two ends " +
                        "is true, so the cell holds one of the two chain values, and " +
                        "${values.prose()} ${values.goVerb()}."
                else
                    "A chain of alternating strong and weak links runs ${cells.chain()}. Whichever " +
                        "way its first link falls, one of the two ends holds ${values.prose()}, " +
                        "and ${d.at.label()} sees both ends."

            Technique.SIMPLE_COLOURING ->
                "Two-colouring the strong links on ${values.prose()} traps ${d.at.label()}: whichever " +
                    "colour is true, one of the highlighted cells it sees holds the value."

            Technique.FINNED_FISH ->
                "A fish on ${values.prose()} spoiled by extra candidates in one base line, the fins. " +
                    "Either the fish is real or a fin holds the value, and ${d.at.label()} loses " +
                    "it both ways."

            Technique.ALS_XZ ->
                "The two highlighted almost-locked sets share ${values.prose()}, and their common " +
                    "restricted value fits only one of them. Whichever set locks, it contains " +
                    "${values.prose()}, and ${d.at.label()} sees every cell that could hold it."

            else -> "${d.technique}: ${values.prose()} cannot go at ${d.at.label()}."
        }
    }

    private fun CellRef.label() = "r${row + 1}c${column + 1}"
    private fun CellPosition.label() = "r${row + 1}c${column + 1}"

    private fun List<CellRef>.chain() = joinToString(" - ") { it.label() }

    private fun List<Int>.prose(): String = when (size) {
        1 -> single().toString()
        2 -> "${this[0]} and ${this[1]}"
        else -> dropLast(1).joinToString(", ") + " and ${last()}"
    }

    private fun List<Int>.goVerb() = if (size == 1) "goes" else "go"

    private fun Int.cells() = if (this == 1) "one solved cell" else "$this solved cells"
    private fun Int.lines(kind: String) = if (this == 1) "one $kind" else "$this ${kind}s"

    /** "Row 3" / "Column 7" / "Box 2" when every cell shares one, else null. */
    private fun houseOf(cells: List<CellRef>): String? = when {
        cells.isEmpty() -> null
        cells.all { it.row == cells[0].row } -> "Row ${cells[0].row + 1}"
        cells.all { it.column == cells[0].column } -> "Column ${cells[0].column + 1}"
        cells.all { it.box() == cells[0].box() } -> "Box ${cells[0].box() + 1}"
        else -> null
    }

    private fun houseOfPositions(cells: List<CellPosition>): String? = houseOf(cells.map { CellRef.of(it) })

    private fun houseWith(cells: List<CellRef>, at: CellRef): String? = houseOf(cells + at)

    private fun boxOf(cells: List<CellRef>) = "box ${cells[0].box() + 1}"

    private fun lineOf(cells: List<CellRef>): String =
        if (cells.all { it.row == cells[0].row }) "row ${cells[0].row + 1}" else "column ${cells[0].column + 1}"

    private fun CellRef.box() = row / 3 * 3 + column / 3

    /** Base and cover orientation of a fish, read off where the evidence and the target sit. */
    private fun fishOrientation(cells: List<CellRef>, at: CellRef): Pair<String, String> =
        if (cells.any { it.column == at.column }) "row" to "column" else "column" to "row"

    private fun CellRef.lineIndex(kind: String) = if (kind == "row") row else column

    private fun fishName(cells: List<CellRef>): String {
        val size = minOf(cells.map { it.row }.distinct().size, cells.map { it.column }.distinct().size)
        return when (size) {
            2 -> "An X-Wing"
            3 -> "A Swordfish"
            else -> "A Jellyfish"
        }
    }
}
