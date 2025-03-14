package com.zenobiapay.model.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zenobiapay.model.orum.OrumCreateTransferResponse
import org.junit.jupiter.api.Test

class OrumCreateTransferResponseTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `test serialize`() {
        val mappedResponse = mapper.readValue(
            """
            {   
                "transfer": {
                    "amount":10,
                    "created_at":"2025-01-22T08:00:43.648085Z",
                    "currency":"USD",
                    "id":"332c7d52-d42b-4ece-864b-1f5dc8b5b150",
                    "source":{
                        "account_reference_id":"6a3V31dE9QhymXrMk76dc49l1vaxozi8Kz13o",
                        "customer_reference_id":"74684478-c0b1-70f1-76aa-0517b6927f54",
                        "statement_display_name":"Pendertif charge"
                    },
                "speed":"standard",
                "status":"created",
                "transfer_reference_id":"c43adc76-5561-4109-9b61-bdbed9fd5f4b",
                "updated_at":"2025-01-22T08:00:43.648085Z"
            }
        }
            """.trimIndent(),
            OrumCreateTransferResponse::class.java
        )
    }
}
