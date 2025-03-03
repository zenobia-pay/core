package com.zenobiapay.model.logic

import com.zenobiapay.logic.getFee
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeeCalculatorTest {
    @Test
    fun `calculate fee with perfect rounding`() {
        assertEquals(110, getFee(10000))
    }

    @Test
    fun `calculate fee floors additional amount`() {
        assertEquals(110, getFee(10010))
    }

    @Test
    fun `calculate fee with low amount`() {
        assertEquals(10, getFee(10))
    }
}