package com.zenobiapay.model.logic

import com.zenobiapay.logic.TransferTableEventLogic
import com.zenobiapay.util.WebhookHandler
import io.mockk.mockk
import org.junit.jupiter.api.Test

class TransferTableEventLogicTest {

    private val webhookHandler = mockk<WebhookHandler>()

    @Test
    fun `test sends transfer payment update to webhook handler`() {
        val transferTableEventLogic = TransferTableEventLogic(webhookHandler)
    }
}
