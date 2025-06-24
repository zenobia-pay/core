package com.zenobiapay.item.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.CompleteSellJobRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.item.util.ResaleUtil
import com.zenobiapay.item.util.S3UrlGenerator
import com.zenobiapay.rds.util.RdsWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import java.util.UUID

private val logger = KotlinLogging.logger {}

class CompleteSellJobOperation @Inject constructor(
    private val rdsWrapper: RdsWrapper,
    private val resaleUtil: ResaleUtil,
    val s3UrlGenerator: S3UrlGenerator,
): Operation<CompleteSellJobRequest, EmptyApiResponse>() {
    override val inputType = CompleteSellJobRequest::class.java

    override fun run(
        request: CompleteSellJobRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): EmptyApiResponse {
        logger.info { "Processing sell item request" }
        val itemUuid = try {
            UUID.fromString(request.itemId)
        } catch (e: IllegalArgumentException) {
            logger.error { "Invalid item ID format: ${request.itemId}" }
            throw IllegalArgumentException("Invalid item ID format")
        }
        
        val item = rdsWrapper.getItem(itemUuid)
        if (item == null) {
            logger.error { "Item not found with ID: $request.itemId" }
            throw IllegalArgumentException("Item not found")
        }

        val itemImageUrls = s3UrlGenerator.generatePresignedUrlForS3Prefix(S3UrlGenerator.generateCustomerImagePrefix(request.itemId, request.sellJobId))
        
        resaleUtil.createDepopListing(
            item = item,
            itemImageUrls = itemImageUrls,
            price = request.price,
            category = request.category,
            shippingAddress = request.shippingAddress,
            condition = request.condition
        )
        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}