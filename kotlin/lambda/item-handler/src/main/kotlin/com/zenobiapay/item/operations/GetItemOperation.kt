package com.zenobiapay.item.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.GetItem200Response
import com.zenobiapay.api.generated.model.GetItemRequest
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.rds.util.RdsWrapper
import jakarta.inject.Inject
import java.util.UUID

class GetItemOperation @Inject constructor(private val rdsWrapper: RdsWrapper): Operation<GetItemRequest, GetItem200Response>() {
    override val inputType = GetItemRequest::class.java
    override fun run(
        request: GetItemRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): GetItem200Response {
        val item = rdsWrapper.getItem(UUID.fromString(request.itemId)) ?: throw ResourceNotFoundException("ITEM")

        return GetItem200Response()
            .itemId(item.itemId.toString())
            .name(item.name)
            .tags(item.tags)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}