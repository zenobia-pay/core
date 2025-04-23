package com.zenobiapay.payout.util

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class EventBridgeEventSerializer @Inject constructor(
    private val objectMapper: ObjectMapper,
) {
    val caseInsensitiveMapper = objectMapper.copy()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

    fun parseAttributeValueMap(raw: Map<String, Any>): Map<String, AttributeValue> {
        return raw.mapValues {
            parseAttributeValue(it.value as Map<String, Any>)
        }
    }

    fun parseAttributeValue(raw: Map<String, Any>): AttributeValue {
        return caseInsensitiveMapper.convertValue(raw, AttributeValue::class.java)
    }
}