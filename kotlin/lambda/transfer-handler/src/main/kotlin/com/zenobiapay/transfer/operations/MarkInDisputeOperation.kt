package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.MarkInDisputeRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.table.transfer.dao.TransferDao
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class MarkInDisputeOperation @Inject constructor(private val transferDao: TransferDao): Operation<MarkInDisputeRequest, EmptyApiResponse>() {
    override val inputType = MarkInDisputeRequest::class.java
    
    override fun run(
        request: MarkInDisputeRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): EmptyApiResponse {
        logger.info { "Marking transfer ${request.transferRequestId} as in dispute" }
        
        val transferId = request.transferRequestId ?: throw InvalidRequestException("Transfer ID is required")
        
        val updatedTransfer = transferDao.updateTransferDisputeStatus(
            transferRequestId = transferId,
            inDispute = true,
        ) ?: throw ResourceNotFoundException("TRANSFER")
        
        logger.info { "Successfully marked transfer $transferId as in dispute" }
        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.ADMIN)
    }
}