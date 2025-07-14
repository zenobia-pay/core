package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.ListMerchants200Response
import com.zenobiapay.api.generated.model.ListMerchants200ResponseMerchantsInner
import com.zenobiapay.api.model.NoApiBody
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.user.util.Auth0Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val logger = KotlinLogging.logger {}

class ListMerchantsOperation @Inject constructor(
    private val auth0Wrapper: Auth0Wrapper
) : Operation<NoApiBody, ListMerchants200Response>() {

    override val inputType = NoApiBody::class.java

    override fun run(request: NoApiBody, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): ListMerchants200Response {
        logger.info { "Processing list merchants request" }
        
        val merchantInfoList = auth0Wrapper.listMerchants()
        
        val merchants = merchantInfoList.map { merchantInfo ->
            ListMerchants200ResponseMerchantsInner()
                .id(merchantInfo.id)
                .name(merchantInfo.name)
                .approved(merchantInfo.approved)
                .creationTime(merchantInfo.creationTime?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) }.toString())
        }
        
        return ListMerchants200Response().merchants(merchants)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.ADMIN)
    }
}
