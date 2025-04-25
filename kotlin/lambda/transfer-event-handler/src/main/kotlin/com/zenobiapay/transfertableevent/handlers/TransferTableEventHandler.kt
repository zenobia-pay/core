package com.zenobiapay.transfertableevent.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.events.model.EventBridgeEvent
import com.zenobiapay.events.util.EventBridgeEventSerializer
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.transfertableevent.di.DaggerAppComponent
import com.zenobiapay.transfertableevent.logic.TransferTableEventLogic
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.logging.log4j.ThreadContext
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class TransferTableEventHandler : RequestHandler<Map<String, Any>, Unit> {
    init {
        DaggerAppComponent.create().inject(this)
    }

    @Inject
    lateinit var logic: TransferTableEventLogic

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var eventBridgeEventSerializer: EventBridgeEventSerializer

    override fun handleRequest(event: Map<String, Any>, context: Context?) {
        logger.info { "Got event $event" }
        val request = objectMapper.convertValue(event, EventBridgeEvent::class.java)

        val oldImage = request.detail?.dynamodb?.oldImage?.let {
            TransferItem.fromAttributeValueMap(
                eventBridgeEventSerializer.parseAttributeValueMap(it)
            )
        }
        val newImage = request.detail?.dynamodb?.newImage?.let {
            TransferItem.fromAttributeValueMap(
                eventBridgeEventSerializer.parseAttributeValueMap(it)
            )
        }

        try {
            newImage?.requestId.let { ThreadContext.put("transferId", it) }
            logic.handleRecord(oldImage, newImage)
            throw Exception("Uh oh unexpected error!")
        } finally {
            ThreadContext.clearAll()
        }
    }
}