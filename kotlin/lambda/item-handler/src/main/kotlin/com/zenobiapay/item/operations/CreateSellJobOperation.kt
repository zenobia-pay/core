package com.zenobiapay.item.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.CreateSellJob200Response
import com.zenobiapay.api.generated.model.GetItemRequest
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.item.util.S3UrlGenerator
import jakarta.inject.Inject

class CreateSellJobOperation @Inject constructor(
    private val s3UrlGenerator: S3UrlGenerator,
): Operation<GetItemRequest, CreateSellJob200Response>() {

    companion object {
        const val NUMBER_IMAGE_UPLOAD_URLS = 5
    }
    override val inputType = GetItemRequest::class.java

    override fun run(request: GetItemRequest, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): CreateSellJob200Response {
        val sellJobId = input.requestContext.requestId
        return CreateSellJob200Response()
            .sellJobId(sellJobId)
            .imageUploadUrls(generatePresignedPutUrls(request.itemId, sellJobId))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }

    private fun generatePresignedPutUrls(itemId: String, sellJobId: String): List<String> {
        return (1..NUMBER_IMAGE_UPLOAD_URLS)
            .map { index ->
                s3UrlGenerator.generatePresignedPutUrl("${S3UrlGenerator.generateCustomerImagePrefix(itemId, sellJobId)}/$index")
            }
    }

}