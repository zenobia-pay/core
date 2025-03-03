package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.BankDao
import com.zenobiapay.dao.UserDao
import com.zenobiapay.model.api.ApiResponse
import com.zenobiapay.model.api.EmptyApiResponse
import com.zenobiapay.model.api.account.UpdateMerchantRequest
import com.zenobiapay.model.cognito.UserPoolGroup
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class UpdateMerchantOperation @Inject constructor(
    private val bankDao: BankDao,
    private val userDao: UserDao,
    private val objectMapper: ObjectMapper
): Operation() {

    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): ApiResponse {
        val request = objectMapper.readValue(input.body, UpdateMerchantRequest::class.java)
        logger.info { "Got request $request" }
        if (request.bankAccountId != null) {
            // Validate bank id exists
            logger.info { "Fetching bank account ${request.bankAccountId}" }
            bankDao.getBankItem(userId, request.bankAccountId)
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
            request.merchantLocation
        )
        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}