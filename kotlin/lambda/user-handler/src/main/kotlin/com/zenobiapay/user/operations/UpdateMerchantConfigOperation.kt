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
import com.zenobiapay.table.user.dao.UserDao
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

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
        val request = objectMapper.readValue(input.body, UpdateMerchantConfigRequest::class.java)
        logger.info { "Got request $request" }
        if (request.bankAccountId != null) {
            // Validate bank id exists
            logger.info { "Fetching bank account ${request.bankAccountId}" }
            bankDao.getBankAccount(userId!!, null, request.bankAccountId!!) ?: throw ResourceNotFoundException("BANK_ACCOUNT")
        }
        userDao.updateMerchant(
            userId!!,
            request.bankAccountId,
            request.merchantDisplayName,
            request.merchantDescription,
            request.merchantLocation,
            request.webhookUrl?.toString(),
        )
        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
