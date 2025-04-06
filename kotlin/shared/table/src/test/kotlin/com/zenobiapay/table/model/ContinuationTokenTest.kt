package com.zenobiapay.table.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import kotlin.test.assertEquals


class ContinuationTokenTest {
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerKotlinModule()
    }

    @Test
    fun `test encode and decode returns same results`() {
        val map = mapOf(
            "pk" to AttributeValue.fromS("primaryKey"),
            "sk" to AttributeValue.fromS("sortKey"),
        )
        val token = ContinuationToken(map)
        val encodedToken = token.encodeToken(mapper)
        assertEquals(token, ContinuationToken.decodeToken(encodedToken, mapper))
    }

    @Test
    fun `test encode returns correct base64 string`() {
        val map = mapOf(
            "pk" to AttributeValue.fromS("primaryKey"),
            "sk" to AttributeValue.fromS("sortKey"),
        )
        val token = ContinuationToken(map)
        val encodedToken = token.encodeToken(mapper)
        assertEquals("eyJwayI6InByaW1hcnlLZXkiLCJzayI6InNvcnRLZXkifQ==", encodedToken)
    }
}