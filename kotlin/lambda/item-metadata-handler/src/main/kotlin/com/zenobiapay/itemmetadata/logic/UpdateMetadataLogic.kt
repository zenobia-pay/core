package com.zenobiapay.itemmetadata.logic

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.events.model.UpdateItemMetadataQueueRecord
import com.zenobiapay.rds.util.RdsWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import java.sql.Timestamp

private val logger = KotlinLogging.logger {}

class UpdateMetadataLogic @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val rdsWrapper: RdsWrapper,
) {
    fun process(recordBody: String) {
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
