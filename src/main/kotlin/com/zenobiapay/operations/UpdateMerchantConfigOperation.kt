package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.BankDao
import com.zenobiapay.dao.UserDao
import com.zenobiapay.model.api.EmptyApiResponse
import com.zenobiapay.model.api.user.UpdateMerchantConfigRequest
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.model.exception.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class UpdateMerchantConfigOperation @Inject constructor(
    private val bankDao: BankDao,
    private val userDao: UserDao,
    private val objectMapper: ObjectMapper
) : Operation() {

    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): Any {
        val request = objectMapper.readValue(input.body, UpdateMerchantConfigRequest::class.java)
        logger.info { "Got request $request" }
        if (request.bankAccountId != null) {
            // Validate bank id exists
            logger.info { "Fetching bank account ${request.bankAccountId}" }
            bankDao.getBankAccount(userId, request.bankAccountId) ?: throw ResourceNotFoundException("BANK_ACCOUNT")
        }
        // TODO: do in one ddb call
        val merchantItem = userDao.getMerchant(userId)
        logger.info { "Got current item $merchantItem" }
        userDao.updateMerchant(
            userId,
            merchantItem,
            request.bankAccountId,
            request.merchantDisplayName,
            request.merchantDescription,
            request.merchantLocation,
            request.webhookUrl
        )
        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
