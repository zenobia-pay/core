package com.zenobiapay.model.util

import com.zenobiapay.util.getCorsHeaders
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CorsUtilTest {
    @Test
    fun `test getCorsHeaders returns correct headers`() {
        val output = getCorsHeaders()
        assertEquals("https://zenobiapay.com", output["Access-Control-Allow-Origin"])
        assertEquals("Content-Type,Authorization", output["Access-Control-Allow-Headers"])
        assertEquals("GET, POST, OPTIONS", output["Access-Control-Allow-Methods"])
    }
}
