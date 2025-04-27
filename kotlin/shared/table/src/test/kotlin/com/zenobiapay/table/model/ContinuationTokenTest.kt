package com.zenobiapay.table.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import kotlin.test.assertEquals


class ContinuationTokenTest {
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerKotlinModule()
    }

    val secret = "sanotehusnatoheus"

    @Test
    fun `test encode and decode returns same results`() {
        val map = mapOf(
            "pk" to AttributeValue.fromS("primaryKey"),
            "sk" to AttributeValue.fromS("sortKey"),
        )
        val token = ContinuationToken(map)
        val encodedToken = token.encodeToken(mapper, secret)
        assertEquals(token, ContinuationToken.decodeToken(encodedToken, mapper, secret))
    }

    @Test
    fun `test encode returns correct base64 string`() {
        val map = mapOf(
            "pk" to AttributeValue.fromS("primaryKey"),
            "sk" to AttributeValue.fromS("sortKey"),
        )
        val token = ContinuationToken(map)
        val encodedToken = token.encodeToken(mapper, secret)
        assertEquals(
            "eyJwayI6InByaW1hcnlLZXkiLCJzayI6InNvcnRLZXkifQ==.ZN2Z5jihyXWGJ9jOdt1mekwRFbpacwk5N7jNrp1Uvy4=",
            encodedToken
        )
    }
}