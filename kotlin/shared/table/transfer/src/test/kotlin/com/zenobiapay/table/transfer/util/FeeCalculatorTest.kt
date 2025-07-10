package com.zenobiapay.table.transfer.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeeCalculatorTest {
    @Test
    fun `calculate fee with perfect rounding`() {
        assertEquals(130, getFee(10000))
    }

    @Test
    fun `calculate fee floors additional amount`() {
        assertEquals(130, getFee(10030))
    }

    @Test
    fun `calculate fee with low amount`() {
        assertEquals(30, getFee(30))
    }
}
