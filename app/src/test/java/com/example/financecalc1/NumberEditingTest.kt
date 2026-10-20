package com.example.financecalc1

import org.junit.Assert.*
import org.junit.Test

class NumberEditingTest {
    @Test fun formulaRatesKeepTheirPrecision() {
        assertEquals("0,001",NumberEditing.rateFraction("0,1"))
        assertEquals("0,015",NumberEditing.rateFraction("1,5"))
        assertEquals("0,0000000000001",NumberEditing.rateFraction("0,00000000001"))
    }
    @Test fun groupsThousandsWithoutRounding() {
        assertEquals("100 000",NumberEditing.group("100000"))
        assertEquals("1 234 567,8900",NumberEditing.group("1234567,8900"))
        assertEquals("-1 000.50",NumberEditing.group("-1000.50"))
    }
    @Test fun preservesIncompleteInput() {
        for(value in listOf("","-","0,","-0.","123,00"))assertEquals(value,NumberEditing.group(value))
        assertEquals("1,2,3",NumberEditing.group("1,2,3"))
    }
    @Test fun normalizedGroupingIsIdempotent() {
        assertEquals("1 234 567",NumberEditing.group("1\u202f234\u00a0567"))
        assertEquals("1 234,50",NumberEditing.group(NumberEditing.group("1234,50")))
    }
    @Test fun keepsCursorBesideEditedDigit() {
        assertEquals(NumberEditing.Edit("19 234",2),NumberEditing.update("19234",2,"1 234",1,0,1,1))
        assertEquals(NumberEditing.Edit("100 000",7),NumberEditing.update("100000",6))
    }
    @Test fun backspaceDoesNotGetStuckAtGroupingSpace() {
        assertEquals(NumberEditing.Edit("10 000",2),NumberEditing.update("100000",3,"100 000",3,1,0,4))
        assertEquals(2..3,NumberEditing.backspaceRange("100 000",4))
        assertNull(NumberEditing.backspaceRange("1",0))
    }
    @Test fun groupedInputStillCalculates() {
        assertEquals(20000.0,FinanceEngine.calculate(Tool.BUDGET,0,listOf(NumberEditing.group("100000"),NumberEditing.group("80000"))).value,.001)
    }
}
