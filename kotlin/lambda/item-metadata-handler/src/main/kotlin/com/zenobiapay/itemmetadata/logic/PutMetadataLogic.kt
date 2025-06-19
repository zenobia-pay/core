package com.zenobiapay.itemmetadata.logic

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.events.model.PutItemMetadataQueueRecord
import com.zenobiapay.itemmetadata.transform.ItemSchemaTransformer
import com.zenobiapay.itemmetadata.util.S3Uploader
import com.zenobiapay.rds.model.RdsItemMetadataSchema
import com.zenobiapay.rds.util.RdsWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import java.util.UUID

private val logger = KotlinLogging.logger {}

class PutMetadataLogic @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val rdsWrapper: RdsWrapper,
    private val itemSchemaTransformer: ItemSchemaTransformer,
    private val s3Uploader: S3Uploader,
) {

    fun process(recordBody: String) {
        val putRecord = objectMapper.readValue(recordBody, PutItemMetadataQueueRecord::class.java)
        
        logger.info { "Processing PUT item metadata for transfer request ID: ${putRecord.transferRequestId}" }
        logger.info { "Item metadata: ${putRecord.transferMetadata}" }

        val transformedItems = putRecord.itemMetadata?.map { (itemId, metadata) ->
            itemSchemaTransformer.transform(metadata).map { itemMetadata ->
                RdsItemMetadataSchema(
                    itemId = UUID.fromString(itemId),
                    merchantId = putRecord.merchantId,
                    itemMetadata = itemMetadata,
                    rawMetadata = metadata,
                    imageS3ObjectKeys = null,
                )
            }
        }?.flatten()
        logger.info { "Transformation: $transformedItems" }

        val transformedItemsWithInternalImages = convertToS3ObjectKeys(transformedItems)

        rdsWrapper.storeTransferAndItemsMetadata(
            putRecord.transferRequestId,
            putRecord.merchantId,
            putRecord.transferMetadata,
            transformedItemsWithInternalImages,
        )
        logger.info { "Successfully stored ${transformedItems?.size} in rds" }
    }

    private fun convertToS3ObjectKeys(itemMetadataList: List<RdsItemMetadataSchema>?): List<RdsItemMetadataSchema>? {
        return itemMetadataList?.map { metadata ->
            metadata.copy(
                imageS3ObjectKeys = metadata.itemMetadata.imageUrls?.mapNotNull { imageUrl ->
                    s3Uploader.uploadImageFromUrl(imageUrl = imageUrl, prefix = "item/${metadata.itemId}")
                }
            )
        }
    }
}
