package com.zenobiapay.webhook.util

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse

class WebhookValidatorTest {
    @Test
    fun `test valid host`() {
        assertTrue(isValidWebhook("https://google.com/webhook"))
    }

    @Test
    fun `test localhost is invalid`() {
        assertFalse(isValidWebhook("https://localhost/webhook"))
    }

    @Test
    fun `test local ipv4 address is invalid`() {
        assertFalse(isValidWebhook("https://127.0.0.1/webhook"))
    }

    @Test
    fun `test private subnet 1 is invalid`() {
        assertFalse(isValidWebhook("https://10.0.3.0.25/webhook"))
    }

    @Test
    fun `test private subnet 2 is invalid`() {
        assertFalse(isValidWebhook("https://10.0.4.0.25/webhook"))
    }
}