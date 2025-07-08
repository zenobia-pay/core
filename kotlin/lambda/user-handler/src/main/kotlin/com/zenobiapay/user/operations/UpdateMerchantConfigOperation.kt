package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.UpdateMerchantConfigRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.table.bank.dao.BankDao
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.webhook.util.isValidWebhook
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class UpdateMerchantConfigOperation @Inject constructor(
    private val bankDao: BankDao,
    private val userDao: UserDao,
    private val objectMapper: ObjectMapper
) : Operation<UpdateMerchantConfigRequest, EmptyApiResponse>() {

    override val inputType = UpdateMerchantConfigRequest::class.java

    override fun run(
        request: UpdateMerchantConfigRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): EmptyApiResponse {
        logger.info { "Got request $request" }
        if (request.webhookUrl != null) {
            logger.info { "validating webhook url ${request.webhookUrl}" }
            if (!isValidWebhook(request.webhookUrl)) {
                logger.info { "Got invalid webhook ${request.webhookUrl}. Rejecting call" }
                throw InvalidRequestException("Invalid webhook")
            }
        }
        userDao.updateMerchant(
            userId!!,
            request.merchantDescription,
            request.merchantLocation,
            request.webhookUrl?.toString(),
            request.notificationEmail,
        )
        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
