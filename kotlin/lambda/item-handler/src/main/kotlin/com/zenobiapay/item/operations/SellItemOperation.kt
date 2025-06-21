package com.zenobiapay.item.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.GetItemRequest
import com.zenobiapay.api.generated.model.SellItem200Response
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.item.util.ResaleUtil
import com.zenobiapay.rds.util.RdsWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import java.util.UUID

private val logger = KotlinLogging.logger {}

class SellItemOperation @Inject constructor(
    private val rdsWrapper: RdsWrapper,
    private val resaleUtil: ResaleUtil
): Operation<GetItemRequest, SellItem200Response>() {
    override val inputType = GetItemRequest::class.java

    override fun run(
        request: GetItemRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): SellItem200Response {
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
        
        val listingCreated = resaleUtil.createDepopListing(item)
        assert(listingCreated)
        
        // Return response with listing status
        return SellItem200Response()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}