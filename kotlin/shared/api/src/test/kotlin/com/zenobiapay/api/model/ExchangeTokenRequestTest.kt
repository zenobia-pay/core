package com.zenobiapay.api.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zenobiapay.api.model.bank.ExchangeTokenRequest
import org.junit.jupiter.api.Test

class ExchangeTokenRequestTest {
    val objectMapper = jacksonObjectMapper()

    @Test
    fun `test deserializing body succeeds`() {
        ExchangeTokenRequest.from(
            """
            {
                "linkToken": "public-sandbox-4d2c1ae3-4020-4645-aa84-c2abb525649f"
            }
            """.trimIndent(),
            objectMapper
        )
    }
}
