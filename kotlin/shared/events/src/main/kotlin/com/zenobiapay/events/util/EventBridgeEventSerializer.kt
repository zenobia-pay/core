package com.zenobiapay.events.util

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.events.model.EventBridgeEvent
import com.zenobiapay.events.model.SqsEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.IllegalArgumentException
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class EventBridgeEventSerializer @Inject constructor(
    private val objectMapper: ObjectMapper,
) {
    val caseInsensitiveMapper = objectMapper.copy()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

    fun getEventBridgeEvent(event: Map<String, Any>): EventBridgeEvent? {
        logger.info { "Fetching event bridge event" }
        return try {
            objectMapper.convertValue(event, EventBridgeEvent::class.java)
        } catch (e: IllegalArgumentException) {
            logger.info { "Trying to deserialize as an sqs event" }
            val sqsEvent = objectMapper.convertValue(event, SqsEvent::class.java)
            val records = sqsEvent.records

            if (records.isEmpty()) {
                logger.info { "Records is empty. Nothing to do, returning" }
                return null
            } else if (records.size > 1) {
                logger.error { "Records size is greater than 1, unexpected! Size: ${records.size}" }
                throw Exception("Records size is greater than 1")
            }
            objectMapper.readValue(sqsEvent.records.first().body, EventBridgeEvent::class.java)
        }
    }

    fun parseAttributeValueMap(raw: Map<String, Any>): Map<String, AttributeValue> {
        return raw.mapValues {
            parseAttributeValue(it.value as Map<String, Any>)
        }
    }

    fun parseAttributeValue(raw: Map<String, Any>): AttributeValue {
        return caseInsensitiveMapper.convertValue(raw, AttributeValue::class.java)
    }
}