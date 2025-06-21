package com.zenobiapay.item.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.Item
import com.zenobiapay.api.generated.model.ListItems200Response
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.item.util.S3UrlGenerator
import com.zenobiapay.rds.util.RdsWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class ListItemsOperation @Inject constructor(
    private val rdsWrapper: RdsWrapper,
    private val s3UrlGenerator: S3UrlGenerator
): Operation<EmptyApiResponse, ListItems200Response>() {
    override val inputType = EmptyApiResponse::class.java
    
    override fun run(
        request: EmptyApiResponse,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): ListItems200Response {
        if (userId == null) {
            logger.warn { "User ID is null when listing items" }
            return ListItems200Response().items(emptyList())
        }
        
        logger.info { "Listing items for user: $userId" }
        val items = rdsWrapper.listItemsByOwnerId(userId)
        
        return ListItems200Response()
            .items(
                items.map { item ->
                    Item()
                        .itemId(item.itemId.toString())
                        .name(item.itemMetadata.name)
                        .brandName(item.itemMetadata.brandName)
                        .size(item.itemMetadata.size)
                        .color(item.itemMetadata.color)
                        .material(item.itemMetadata.material)
                        .year(item.itemMetadata.year)
                        .imageUrls(s3UrlGenerator.generatePresignedUrls(item.imageS3ObjectKeys))
                }
            )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}