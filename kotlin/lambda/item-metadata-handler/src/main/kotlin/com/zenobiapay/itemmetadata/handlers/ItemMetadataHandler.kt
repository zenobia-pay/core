package com.zenobiapay.itemmetadata.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.events.model.ItemMetadataQueueRecord
import com.zenobiapay.events.model.ItemMetadataQueueRecordType
import com.zenobiapay.itemmetadata.di.DaggerAppComponent
import com.zenobiapay.itemmetadata.logic.PutMetadataLogic
import com.zenobiapay.itemmetadata.logic.UpdateMetadataLogic
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class ItemMetadataHandler : RequestHandler<SQSEvent, Unit> {
    @Inject
    lateinit var objectMapper: ObjectMapper
    
    @Inject
    lateinit var putMetadataLogic: PutMetadataLogic
    
    @Inject
    lateinit var updateMetadataLogic: UpdateMetadataLogic
    
    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: SQSEvent, context: Context) {
        logger.info { "Received ${event.records.size} SQS messages" }
        
        event.records.forEach { record ->
            // First deserialize as base class to check the type
            val baseRecord = objectMapper.readValue(record.body, ItemMetadataQueueRecord::class.java)
            
            when (baseRecord.type) {
                ItemMetadataQueueRecordType.PUT -> putMetadataLogic.process(record.body)
                ItemMetadataQueueRecordType.UPDATE -> updateMetadataLogic.process(record.body)
                else -> logger.error { "Unknown record type: ${baseRecord.type}" }
            }
        }
    }
}
