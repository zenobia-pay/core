package com.zenobiapay.itemmetadata.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.events.model.ItemMetadataQueueRecord
import com.zenobiapay.events.model.ItemMetadataQueueRecordType
import com.zenobiapay.events.model.PutItemMetadataQueueRecord
import com.zenobiapay.events.model.UpdateItemMetadataQueueRecord
import com.zenobiapay.rds.util.RdsWrapper
import com.zenobiapay.itemmetadata.di.DaggerAppComponent
import com.zenobiapay.itemmetadata.transform.ShopifySchemaTransformer
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import java.sql.Timestamp

private val logger = KotlinLogging.logger {}

class ItemMetadataHandler : RequestHandler<SQSEvent, Unit> {
    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var rdsWrapper: RdsWrapper
    
    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: SQSEvent, context: Context) {
        logger.info { "Received ${event.records.size} SQS messages" }
        
        event.records.forEach { record ->
            // First deserialize as base class to check the type
            val baseRecord = objectMapper.readValue(record.body, ItemMetadataQueueRecord::class.java)
            
            when (baseRecord.type) {
                ItemMetadataQueueRecordType.PUT -> handlePutRecord(record.body)
                ItemMetadataQueueRecordType.UPDATE -> handleUpdateRecord(record.body)
                else -> logger.error { "Unknown record type: ${baseRecord.type}" }
            }
        }
    }
    
    private fun handlePutRecord(recordBody: String) {
        val putRecord = objectMapper.readValue(recordBody, PutItemMetadataQueueRecord::class.java)
        
        logger.info { "Processing PUT item metadata for transfer request ID: ${putRecord.transferRequestId}" }
        logger.info { "Item metadata: ${putRecord.transferMetadata}" }

        val transformedItems = putRecord.itemMetadata?.map { (itemId, metadata) ->
            ShopifySchemaTransformer().transform(itemId, putRecord.merchantId, metadata)
        }?.flatten()

        rdsWrapper.storeTransferAndItemsMetadata(
            putRecord.transferRequestId,
            putRecord.merchantId,
            putRecord.transferMetadata,
            transformedItems,
        )
    }
    
    private fun handleUpdateRecord(recordBody: String) {
        val updateRecord = objectMapper.readValue(recordBody, UpdateItemMetadataQueueRecord::class.java)
        
        logger.info { "Processing UPDATE ownership for transfer request ID: ${updateRecord.transferRequestId}" }
        logger.info { "Owner ID: ${updateRecord.ownerId}, Ownership Time: ${updateRecord.ownershipTime}" }
        
        // Convert Java Instant to SQL Timestamp
        val ownershipTimestamp = Timestamp.from(updateRecord.ownershipTime)
        
        // Call the new method to update ownership information
        val updatedCount = rdsWrapper.updateTransferOwnership(
            updateRecord.transferRequestId,
            updateRecord.ownerId,
            ownershipTimestamp
        )
        
        logger.info { "Updated ownership for $updatedCount items in transfer ID: ${updateRecord.transferRequestId}" }
    }
}