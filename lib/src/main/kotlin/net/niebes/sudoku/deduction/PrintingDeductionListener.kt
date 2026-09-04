package net.niebes.sudoku.deduction

/** Prints deductions as they happen. */
class PrintingDeductionListener : DeductionListener {
    override fun onDeduction(deduction: Deduction) = println(deduction)
}
