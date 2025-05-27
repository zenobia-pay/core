package com.zenobiapay.transfermetadata.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.events.model.ItemMetadataRecord
import com.zenobiapay.rds.util.RdsWrapper
import com.zenobiapay.transfermetadata.di.DaggerAppComponent
import com.zenobiapay.transfermetadata.transform.ShopifySchemaTransformer
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

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
            val itemMetadataRecord = objectMapper.readValue(record.body, ItemMetadataRecord::class.java)

            logger.info { "Processing item metadata for transfer request ID: ${itemMetadataRecord.transferRequestId}" }
            logger.info { "Item metadata: ${itemMetadataRecord.transferMetadata}" }

            val transformedItems = itemMetadataRecord.itemMetadata.map { itemMetadata ->
                ShopifySchemaTransformer().transform(itemMetadataRecord.merchantId, itemMetadata)
            }.flatten()

            rdsWrapper.storeTransferAndItemsMetadata(
                itemMetadataRecord.transferRequestId,
                itemMetadataRecord.merchantId,
                itemMetadataRecord.transferMetadata,
                transformedItems,
            )
        }
    }
}