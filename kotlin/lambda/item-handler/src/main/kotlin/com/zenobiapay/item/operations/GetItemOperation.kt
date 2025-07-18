package com.zenobiapay.item.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.GetItemRequest
import com.zenobiapay.api.generated.model.Item
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.item.util.S3UrlGenerator
import com.zenobiapay.rds.util.RdsWrapper
import jakarta.inject.Inject
import java.util.UUID

class GetItemOperation @Inject constructor(
    private val rdsWrapper: RdsWrapper,
    private val s3UrlGenerator: S3UrlGenerator
): Operation<GetItemRequest, Item>() {
    override val inputType = GetItemRequest::class.java
    override fun run(
        request: GetItemRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Item {
        val item = rdsWrapper.getItem(UUID.fromString(request.itemId)) ?: throw ResourceNotFoundException("ITEM")

        return Item()
            .itemId(item.itemId.toString())
            .name(item.itemMetadata.name)
            .brandName(item.itemMetadata.brandName)
            .size(item.itemMetadata.size)
            .color(item.itemMetadata.color)
            .material(item.itemMetadata.material)
            .year(item.itemMetadata.year)
            .imageUrls(s3UrlGenerator.generatePresignedUrls(item.imageS3ObjectKeys))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN)
    }
}