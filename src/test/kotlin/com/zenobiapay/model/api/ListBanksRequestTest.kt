package com.zenobiapay.model.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zenobiapay.model.api.bank.ListBanksRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class ListBanksRequestTest {
    private val objectMapper = jacksonObjectMapper()
    @Test
    fun `test list banks request deserializes token if it exists`() {
        val token = "token"
        val request = ListBanksRequest.from(mapOf("continuationToken" to token), objectMapper)
        assertEquals(token, request.continuationToken)
    }

    @Test
    fun `test list banks request deserializes with empty map`() {
        val token = "token"
        val request = ListBanksRequest.from(mapOf(), objectMapper)
        assertNull(request.continuationToken)
    }
}