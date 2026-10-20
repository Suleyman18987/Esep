package com.example.financecalc1

import org.junit.Assert.*
import org.junit.Test

class FinanceEngineTest {
    private fun value(t: Tool, vararg values: String, mode: Int = 0) = FinanceEngine.calculate(t, mode, values.toList()).value
    private fun problem(reason: String, t: Tool, vararg values: String, mode: Int = 0) {
        try { value(t, *values, mode = mode); fail("Expected $reason") }
        catch (e: InputProblem) { assertEquals(reason, e.reason) }
    }
    @Test fun budgetAndNegativeBalance() {
        assertEquals(90000.0, value(Tool.BUDGET, "300000", "210000"), .001)
        val deficit = FinanceEngine.calculate(Tool.BUDGET, 0, listOf("100", "150"))
        assertEquals(-50.0, deficit.value, .001)
        assertEquals(deficit.value, deficit.details.sumOf { it.second }, .001)
    }
    @Test fun savingsAnnualCapitalizationAndAnnualContributions() {
        assertEquals(121000.0, value(Tool.SAVINGS, "100000", "10", "2"), .001)
        assertEquals(210000.0, value(Tool.SAVINGS, "100000", "10", "2", mode = 1), .001)
        assertEquals(200000.0, value(Tool.SAVINGS, "100000", "0", "2", mode = 1), .001)
    }
    @Test fun simpleAndCompoundInterest() {
        assertEquals(20000.0, value(Tool.INTEREST, "100000", "10", "2"), .001)
        assertEquals(122039.09613756, value(Tool.INTEREST, "100000", "10", "2", "12", mode = 1), .001)
    }
    @Test fun monthlyLoanRateAndZeroRate() {
        assertEquals(49924.10196, value(Tool.LOAN, "1000000", "1,5", "24"), .001)
        assertEquals(1000.0, value(Tool.LOAN, "12000", "0", "12"), .001)
        assertEquals(1000.0, value(Tool.LOAN, "12000", "0,000000000000001", "12"), .001)
    }
    @Test fun debtCanExceedOneHundredPercent() {
        assertEquals(20.0, value(Tool.DEBT, "60000", "300000"), .001)
        assertEquals(150.0, value(Tool.DEBT, "150", "100"), .001)
    }
    @Test fun investmentIncludesLosses() {
        assertEquals(20.0, value(Tool.INVESTMENT, "100000", "120000"), .001)
        assertEquals(-100.0, value(Tool.INVESTMENT, "100000", "0"), .001)
    }
    @Test fun inflationIsExplicitApproximation() {
        assertEquals(4.0, value(Tool.INFLATION, "12", "8"), .001)
        assertEquals(-10.0, value(Tool.INFLATION, "-2", "8"), .001)
    }
    @Test fun goalIsLumpSumPresentValue() {
        assertEquals(826446.2809917, value(Tool.GOAL, "1000000", "10", "2"), .001)
    }
    @Test fun localizedInputAndInvalidNumbers() {
        assertEquals(12345.67, FinanceEngine.parse("12\u202f345,67")!!, .001)
        assertNull(FinanceEngine.parse("NaN")); assertNull(FinanceEngine.parse("Infinity"))
        problem("number", Tool.BUDGET, "1,2,3", "10")
        problem("required", Tool.BUDGET, "", "10")
    }
    @Test fun denominatorAndBoundsValidation() {
        problem("range", Tool.DEBT, "100", "0")
        problem("range", Tool.INVESTMENT, "0", "100")
        problem("integer", Tool.LOAN, "100", "1", "2,5")
        problem("range", Tool.INTEREST, "100", "10", "2", "0", mode = 1)
        problem("overflow", Tool.SAVINGS, "100000", "1000", "1000")
    }
}
